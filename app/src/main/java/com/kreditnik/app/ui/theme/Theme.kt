package com.kreditnik.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Основная тема приложения "Кредитник".
 *
 * Применяет [MaterialTheme] с определенными цветовыми схемами и типографикой.
 * Поддерживает системную темную тему и динамические цвета (Material You) на Android 12+.
 *
 * @param darkTheme Если `true`, применяется темная цветовая схема. По умолчанию
 * определяется системной настройкой.
 * @param dynamicColor Если `true` (и устройство поддерживает), используются динамические
 * цвета, сгенерированные из обоев пользователя.
 * @param content Содержимое, к которому будет применена тема.
 */
@Composable
fun KreditnikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}