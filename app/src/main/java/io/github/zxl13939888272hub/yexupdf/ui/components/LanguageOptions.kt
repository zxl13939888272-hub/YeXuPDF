package io.github.zxl13939888272hub.yexupdf.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.zxl13939888272hub.yexupdf.R
import io.github.zxl13939888272hub.yexupdf.ui.utils.LocaleHelper

/** The app's UI languages, in order, keyed by the tags [LocaleHelper.normalizeForUi] produces. */
val AppLanguageTags = listOf("en", "pt-BR", "es", "it", "ru", "ar", "zh-CN")

/**
 * [AppLanguageTags] as dropdown rows, iOS-style: the name in the current UI language, with the
 * language's own name underneath whenever it differs — so someone who landed in the wrong language
 * can still find theirs.
 */
@Composable
fun rememberLanguageOptions(): List<GlassDropdownOption<String>> {
    val localized = listOf(
        stringResource(R.string.language_english),
        stringResource(R.string.language_portuguese),
        stringResource(R.string.language_spanish),
        stringResource(R.string.language_italian),
        stringResource(R.string.language_russian),
        stringResource(R.string.language_arabic),
        stringResource(R.string.language_chinese_simplified)
    )
    return AppLanguageTags.mapIndexed { i, tag ->
        val native = LocaleHelper.getLanguageDisplayName(tag)
        GlassDropdownOption(
            value = tag,
            label = localized[i],
            supporting = native.takeIf { !it.startsWith(localized[i], ignoreCase = true) }
        )
    }
}
