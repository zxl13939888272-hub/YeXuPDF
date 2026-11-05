package io.github.zxl13939888272hub.yexupdf.imageeditor.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/** Offline build: bundled U2NetP model running entirely on-device through ONNX Runtime. */
internal object BackgroundRemoverFlavor {
    fun create(context: Context): BackgroundRemover = OnDeviceU2NetBackgroundRemover(context)
}

/**
 * Lightweight salient-object segmentation. The 4.6 MB U2NetP weights are packaged in the APK,
 * so neither the source image nor a model request ever crosses the network.
 */
private class OnDeviceU2NetBackgroundRemover(private val context: Context) : BackgroundRemover {

    companion object {
        private const val MODEL_ASSET = "u2netp.onnx"
        private const val FALLBACK_SIZE = 320
    }

    private val environment: OrtEnvironment by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OrtEnvironment.getEnvironment()
    }

    private val session: OrtSession by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val model = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        createSession(model)
    }

    override val isAvailable: Boolean = runCatching {
        context.assets.open(MODEL_ASSET).use { it.read() }
        true
    }.getOrDefault(false)

    override suspend fun segment(image: Bitmap): Result<Bitmap> = withContext(Dispatchers.Default) {
        if (!isAvailable) {
            return@withContext Result.failure(IllegalStateException("The offline background-removal model is missing."))
        }
        runCatching { inferMask(image) }
    }

    private fun createSession(model: ByteArray): OrtSession {
        fun options(): OrtSession.SessionOptions = OrtSession.SessionOptions().apply {
            setCPUArenaAllocator(false)
            setMemoryPatternOptimization(false)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val accelerated = options()
            try {
                accelerated.addNnapi()
                return environment.createSession(model, accelerated)
            } catch (_: Exception) {
                // Some NNAPI drivers reject U2Net operators. CPU is slower but deterministic.
            } finally {
                accelerated.close()
            }
        }
        val cpu = options()
        return try {
            environment.createSession(model, cpu)
        } finally {
            cpu.close()
        }
    }

    private fun inferMask(image: Bitmap): Bitmap {
        val inputInfo = session.inputInfo.values.firstOrNull()?.info as? TensorInfo
        val rawShape = inputInfo?.shape
        val height = rawShape?.getOrNull(2)?.takeIf { it > 0 }?.toInt() ?: FALLBACK_SIZE
        val width = rawShape?.getOrNull(3)?.takeIf { it > 0 }?.toInt() ?: FALLBACK_SIZE
        val shape = longArrayOf(1, 3, height.toLong(), width.toLong())

        val scaled = Bitmap.createScaledBitmap(image, width, height, true)
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        if (scaled !== image) scaled.recycle()

        val means = floatArrayOf(0.485f, 0.456f, 0.406f)
        val stds = floatArrayOf(0.229f, 0.224f, 0.225f)
        val input = FloatBuffer.allocate(3 * width * height)
        for (channel in 0..2) {
            for (pixel in pixels) {
                val component = when (channel) {
                    0 -> pixel ushr 16 and 0xFF
                    1 -> pixel ushr 8 and 0xFF
                    else -> pixel and 0xFF
                }
                input.put((component / 255f - means[channel]) / stds[channel])
            }
        }
        input.rewind()

        val values = OnnxTensor.createTensor(environment, input, shape).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor)).use { result ->
                val output = result.first().value as? OnnxTensor
                    ?: error("U2Net returned an unsupported output")
                val outputShape = output.info.shape
                val maskHeight = outputShape.getOrNull(outputShape.size - 2)?.toInt() ?: height
                val maskWidth = outputShape.getOrNull(outputShape.size - 1)?.toInt() ?: width
                val floats = FloatArray(maskWidth * maskHeight)
                output.floatBuffer.get(floats)
                MaskData(floats, maskWidth, maskHeight)
            }
        }

        val alpha = U2NetMaskPostProcessor.alpha(values.values)
        val argb = IntArray(alpha.size) { index ->
            ((alpha[index].toInt() and 0xFF) shl 24) or 0x00FFFFFF
        }
        return Bitmap.createBitmap(argb, values.width, values.height, Bitmap.Config.ARGB_8888)
    }

    private data class MaskData(val values: FloatArray, val width: Int, val height: Int)
}

/** Pure mask post-processing kept separate so edge cases can be unit tested without Android. */
internal object U2NetMaskPostProcessor {
    fun alpha(mask: FloatArray): ByteArray {
        if (mask.isEmpty()) return ByteArray(0)
        var minimum = Float.POSITIVE_INFINITY
        var maximum = Float.NEGATIVE_INFINITY
        mask.forEach { value ->
            if (value.isFinite()) {
                minimum = minOf(minimum, value)
                maximum = maxOf(maximum, value)
            }
        }
        if (!minimum.isFinite() || !maximum.isFinite() || maximum - minimum < 1e-6f) {
            return ByteArray(mask.size)
        }
        val range = maximum - minimum
        return ByteArray(mask.size) { index ->
            val normalized = ((mask[index] - minimum) / range).coerceIn(0f, 1f)
            val smooth = normalized * normalized * (3f - 2f * normalized)
            (smooth * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        }
    }
}
