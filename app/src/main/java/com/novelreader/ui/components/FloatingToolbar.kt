package com.novelreader.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun FloatingToolbar(
    visible: Boolean,
    onFileClick: () -> Unit,
    onCopyClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 200.dp,
        animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
        label = "toolbar_offset"
    )

    if (!visible && offsetY >= 200.dp) return

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                .offset(y = offsetY)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(12f, 12f, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "文件") },
                    onClick = {
                        triggerHaptic(context)
                        onFileClick()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ToolbarButton(
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = "复制") },
                    onClick = {
                        triggerHaptic(context)
                        onCopyClick()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ToolbarButton(
                    icon = { Icon(Icons.Default.BookmarkBorder, contentDescription = "书签") },
                    onClick = {
                        triggerHaptic(context)
                        onBookmarkClick()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ToolbarButton(
                    icon = { Icon(Icons.Default.Palette, contentDescription = "主题") },
                    onClick = {
                        triggerHaptic(context)
                        onThemeClick()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ToolbarButton(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                    onClick = {
                        triggerHaptic(context)
                        onSettingsClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

private fun triggerHaptic(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, 30))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}


