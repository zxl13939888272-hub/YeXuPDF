package io.github.zxl13939888272hub.yexupdf.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class OfflineAssetIntegrityTest {
    @Test
    fun bundledU2NetModelMatchesReviewedArtifact() {
        val appDir = findAppDir()
        val model = File(appDir, "src/fdroid/assets/u2netp.onnx")
        assertTrue("The offline U2NetP model must be packaged", model.isFile)
        assertEquals(4_574_861L, model.length())
        assertEquals(
            "309c8469258dda742793dce0ebea8e6dd393174f89934733ecc8b14c76f4ddd8",
            sha256(model)
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun findAppDir(): File {
        var current = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(6) {
            if (File(current, "src/main").isDirectory && File(current, "build.gradle.kts").isFile) return current
            val nested = File(current, "app")
            if (File(nested, "src/main").isDirectory && File(nested, "build.gradle.kts").isFile) return nested
            current = current.parentFile ?: return@repeat
        }
        error("Could not locate app module")
    }
}
