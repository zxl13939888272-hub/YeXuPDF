package io.github.zxl13939888272hub.yexupdf.office

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Offline build: installs the verified LibreOfficeKit archive already packaged in the APK. */
internal object OfficeEngineFlavor {
    const val available: Boolean = true
    fun createInstaller(context: Context): OfficeEngineInstaller = BundledOfficeEngineInstaller(context)
}

private class BundledOfficeEngineInstaller(private val context: Context) : OfficeEngineInstaller {

    companion object {
        private const val ASSET = "office-engine/LibreOffice-arm64.tar.xz"
        private const val BUFFER = 1 shl 16
        private const val XZ_MEMORY_LIMIT_KIB = 300 * 1024
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(OfficeEngine.installedState(context))
    override val state: StateFlow<OfficeEngineState> = _state.asStateFlow()
    override val usesNotification: Boolean = false
    private var installJob: Job? = null

    override fun install() {
        if (!OfficeEngine.isSupported() || installJob?.isActive == true) return
        installJob = scope.launch {
            _state.value = OfficeEngineState.Installing
            try {
                installBundledArchive()
                _state.value = OfficeEngine.installedState(context)
            } catch (cancelled: CancellationException) {
                OfficeEngineStore.stagingDir(context).deleteRecursively()
                _state.value = OfficeEngine.installedState(context)
                throw cancelled
            } catch (_: OutOfMemoryError) {
                OfficeEngineStore.stagingDir(context).deleteRecursively()
                _state.value = OfficeEngineState.Failed("Not enough memory to unpack the Office engine")
            } catch (error: Exception) {
                OfficeEngineStore.stagingDir(context).deleteRecursively()
                _state.value = OfficeEngineState.Failed(error.message ?: "Could not install the bundled Office engine")
            }
        }
    }

    private suspend fun installBundledArchive() {
        if (Runtime.getRuntime().maxMemory() < 290L * 1024 * 1024) {
            throw IOException("Not enough memory to unpack the Office engine")
        }
        val bundle = OfficeEngineManifest.bundleForDevice()
            ?: throw IOException("This device architecture is not supported")
        if (bundle.abi != "arm64-v8a") throw IOException("The offline APK supports arm64 devices only")

        val digest = MessageDigest.getInstance("SHA-256")
        val staging = OfficeEngineStore.resetStaging(context)
        val jobContext = coroutineContext
        val files = context.assets.open(ASSET).use { asset ->
            DigestInputStream(BufferedInputStream(asset, BUFFER), digest).use { verified ->
                XZInputStream(verified, XZ_MEMORY_LIMIT_KIB).use { xz ->
                    val extracted = EngineArchiveLayout.extractTar(xz, staging, bundle.abi) { !jobContext.isActive }
                    val drain = ByteArray(BUFFER)
                    while (xz.read(drain) >= 0) {
                        if (!jobContext.isActive) throw CancellationException()
                    }
                    extracted
                }
            }
        }
        if (files == 0) throw IOException("The bundled Office engine is empty")
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        if (actualHash != bundle.sha256) throw IOException("The bundled Office engine failed verification")

        OfficeEngineClient.stopEngineProcess(context)
        OfficeEngineStore.commit(context, staging, bundle.abi)
        OfficeEngineClient.clearCache(context)
    }

    override fun cancel() {
        installJob?.cancel()
    }

    override fun uninstall() {
        installJob?.cancel()
        scope.launch {
            OfficeEngine.removeInstalledEngine(context)
            _state.value = OfficeEngine.installedState(context)
        }
    }

    override fun refresh() {
        _state.value = OfficeEngine.installedState(context)
    }
}
