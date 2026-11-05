package io.github.zxl13939888272hub.yexupdf.imageeditor.engine

import android.content.Context
import android.graphics.Bitmap

/**
 * Produces a subject mask for automatic background removal. Kept behind an interface so each
 * distribution can use its own on-device implementation without touching the editor.
 */
interface BackgroundRemover {
    /** False when the device / build can't run automatic removal at all. */
    val isAvailable: Boolean

    /**
     * Returns an alpha mask at any resolution (opaque = keep), or a failure whose message is
     * user-presentable. The renderer scales the mask to the edited image.
     */
    suspend fun segment(image: Bitmap): Result<Bitmap>

    companion object {
        fun create(context: Context): BackgroundRemover = BackgroundRemoverFlavor.create(context.applicationContext)
    }
}
