package io.github.zxl13939888272hub.yexupdf.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.zxl13939888272hub.yexupdf.ui.theme.LiquidGlassColors
import io.github.zxl13939888272hub.yexupdf.ui.theme.LocalIsDarkMode
import io.github.zxl13939888272hub.yexupdf.R
import com.kyant.backdrop.Backdrop

@Composable
fun DocsBottomTabs(
    selectedTab: () -> Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val tint = LiquidGlassColors.secondary(isDarkMode)

    LiquidBottomTabs(
        selectedTabIndex = selectedTab,
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = 3,
        modifier = modifier
    ) {
        LiquidBottomTab(onClick = { onTabSelected(0) }) {
            Icon(Icons.Rounded.Home, contentDescription = stringResource(R.string.nav_home), tint = tint,
                modifier = Modifier.size(22.dp))
        }

        LiquidBottomTab(onClick = { onTabSelected(1) }) {
            Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.nav_tools), tint = tint,
                modifier = Modifier.size(22.dp))
        }

        LiquidBottomTab(onClick = { onTabSelected(2) }) {
            Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.nav_settings), tint = tint,
                modifier = Modifier.size(22.dp))
        }
    }
}
