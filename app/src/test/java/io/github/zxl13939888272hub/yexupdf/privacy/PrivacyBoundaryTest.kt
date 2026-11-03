package io.github.zxl13939888272hub.yexupdf.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the architectural privacy promise with source-level checks.
 *
 * This is deliberately strict: adding a network client or another WebView network entry point
 * must update the allow-list, which makes the privacy impact visible in code review.
 */
class PrivacyBoundaryTest {

    private val appDir: File = findAppDir()

    @Test
    fun directNetworkClientsAreAllowlisted() {
        val networkImport = Regex(
            "(?m)^import\\s+(java\\.net\\.(?:URL|URLConnection|HttpURLConnection)|okhttp3\\..+|retrofit2\\..+)\\s*$"
        )
        val actual = kotlinSources()
            .filter { networkImport.containsMatchIn(it.readText()) }
            .map { it.relativeTo(appDir).invariantSeparatorsPath }
            .toSet()

        assertEquals(
            setOf(
                "src/foss/java/io/github/zxl13939888272hub/yexupdf/office/OfficeEngineDownloadWorker.kt"
            ),
            actual
        )
    }

    @Test
    fun webViewNetworkEntryPointsAreAllowlisted() {
        val actual = kotlinSources()
            .filter { ".loadUrl(" in it.readText() }
            .map { it.relativeTo(appDir).invariantSeparatorsPath }
            .toSet()

        assertEquals(
            setOf(
                "src/main/java/io/github/zxl13939888272hub/yexupdf/util/HtmlToPdfConverter.kt",
                "src/main/java/io/github/zxl13939888272hub/yexupdf/utils/DocxWebRenderer.kt"
            ),
            actual
        )

        val htmlConverter = File(
            appDir,
            "src/main/java/io/github/zxl13939888272hub/yexupdf/util/HtmlToPdfConverter.kt"
        ).readText()
        assertTrue("Local HTML must use the offline WebView hardening", "OfflineWebContent.harden(webView)" in htmlConverter)

        val docxRenderer = File(
            appDir,
            "src/main/java/io/github/zxl13939888272hub/yexupdf/utils/DocxWebRenderer.kt"
        ).readText()
        assertTrue("DOCX rendering must use the offline WebView hardening", "OfflineWebContent.harden(view)" in docxRenderer)
    }

    @Test
    fun telemetryAndUploadSdksAreAbsent() {
        val forbidden = listOf(
            "firebase-analytics",
            "firebase-crashlytics",
            "com.segment.analytics",
            "io.sentry",
            "com.amplitude",
            "com.mixpanel",
            "com.bugsnag",
            "appcenter-analytics"
        )
        val buildText = requireNotNull(appDir.parentFile).walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".gradle.kts") || it.name == "libs.versions.toml") }
            .joinToString("\n") { it.readText().lowercase() }

        forbidden.forEach { dependency ->
            assertFalse("Forbidden telemetry dependency found: $dependency", dependency in buildText)
        }
    }

    @Test
    fun manifestsKeepNetworkPermissionScopedAndRemovePhoneIdentity() {
        val mainPermissions = manifestPermissions(File(appDir, "src/main/AndroidManifest.xml"))
        val fossPermissions = manifestPermissions(File(appDir, "src/foss/AndroidManifest.xml"))
        val fdroidPermissions = manifestPermissions(File(appDir, "src/fdroid/AndroidManifest.xml"))

        assertFalse("The shared manifest must remain offline", "android.permission.INTERNET" in mainPermissions)
        assertTrue("FOSS explicitly enables user-initiated network features", "android.permission.INTERNET" in fossPermissions)
        assertTrue("F-Droid explicitly enables URL-to-PDF", "android.permission.INTERNET" in fdroidPermissions)

        val mainManifest = File(appDir, "src/main/AndroidManifest.xml").readText()
        assertTrue("READ_PHONE_STATE must be removed from merged manifests", "android.permission.READ_PHONE_STATE" in mainManifest)
        assertTrue("The removal must use manifest merger tools:node", "tools:node=\"remove\"" in mainManifest)
    }

    private fun kotlinSources(): Sequence<File> = appDir.resolve("src")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" && "/test/" !in it.invariantSeparatorsPath }

    private fun manifestPermissions(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("uses-permission")
        return buildSet {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as Element
                add(element.getAttributeNS(ANDROID_NAMESPACE, "name"))
            }
        }
    }

    private fun findAppDir(): File {
        var current = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(6) {
            if (File(current, "src/main").isDirectory && File(current, "build.gradle.kts").isFile) return current
            val nested = File(current, "app")
            if (File(nested, "src/main").isDirectory && File(nested, "build.gradle.kts").isFile) return nested
            current = current.parentFile ?: return@repeat
        }
        error("Could not locate the app module from ${System.getProperty("user.dir")}")
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
