/*
 * Copyright (C) 2025 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.updater

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

@Composable
fun UpdaterTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()

    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = colorResource(id = android.R.color.system_accent1_100),
            onPrimary = colorResource(id = android.R.color.system_accent1_900),
            primaryContainer = colorResource(id = android.R.color.system_accent1_200),
            onPrimaryContainer = colorResource(id = android.R.color.system_accent1_900),

            secondary = colorResource(id = android.R.color.system_accent2_200),
            onSecondary = colorResource(id = android.R.color.system_accent2_900),
            secondaryContainer = colorResource(id = android.R.color.system_accent2_300),
            onSecondaryContainer = colorResource(id = android.R.color.system_accent2_900),

            tertiary = colorResource(id = android.R.color.system_accent3_200),
            onTertiary = colorResource(id = android.R.color.system_accent3_900),
            tertiaryContainer = colorResource(id = android.R.color.system_accent3_300),
            onTertiaryContainer = colorResource(id = android.R.color.system_accent3_900),

            background = colorResource(id = android.R.color.system_neutral1_900),
            onBackground = colorResource(id = android.R.color.system_neutral1_50),
            surface = colorResource(id = android.R.color.system_neutral1_800),
            onSurface = colorResource(id = android.R.color.system_neutral2_50),

            error = colorResource(id = android.R.color.system_accent1_300),
            onError = colorResource(id = android.R.color.system_accent1_900),

            inverseOnSurface = colorResource(id = android.R.color.system_neutral1_900),
            inverseSurface = colorResource(id = android.R.color.system_neutral1_50),
            inversePrimary = colorResource(id = android.R.color.system_accent1_400)
        )
    } else {
        lightColorScheme(
            primary = colorResource(id = android.R.color.system_accent1_600),
            onPrimary = colorResource(id = android.R.color.system_accent1_50),
            primaryContainer = colorResource(id = android.R.color.system_accent1_100),
            onPrimaryContainer = colorResource(id = android.R.color.system_accent1_900),

            secondary = colorResource(id = android.R.color.system_accent2_600),
            onSecondary = colorResource(id = android.R.color.system_accent2_50),
            secondaryContainer = colorResource(id = android.R.color.system_accent2_100),
            onSecondaryContainer = colorResource(id = android.R.color.system_accent2_900),

            tertiary = colorResource(id = android.R.color.system_accent3_600),
            onTertiary = colorResource(id = android.R.color.system_accent3_50),
            tertiaryContainer = colorResource(id = android.R.color.system_accent3_100),
            onTertiaryContainer = colorResource(id = android.R.color.system_accent3_900),

            background = colorResource(id = android.R.color.system_neutral1_0),
            onBackground = colorResource(id = android.R.color.system_neutral1_900),
            surface = colorResource(id = android.R.color.system_neutral1_50),
            onSurface = colorResource(id = android.R.color.system_neutral2_900),

            error = colorResource(id = android.R.color.system_accent1_400),
            onError = colorResource(id = android.R.color.system_accent1_50),

            inverseOnSurface = colorResource(id = android.R.color.system_neutral1_50),
            inverseSurface = colorResource(id = android.R.color.system_neutral1_900),
            inversePrimary = colorResource(id = android.R.color.system_accent1_100)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
