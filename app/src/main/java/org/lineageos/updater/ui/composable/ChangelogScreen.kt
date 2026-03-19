/*
 * Copyright (C) 2025-2026 AxionOS
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
package org.lineageos.updater.ui.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.axion.compose.theme.AxionColors
import org.lineageos.updater.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChangelogBottomSheet(
    changelog: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AxionColors.cardBackground,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.changelog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                if (changelog.isEmpty()) {
                    Text(
                        text = stringResource(R.string.list_no_updates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    changelog.lines().forEach { line ->
                        when {
                            line.startsWith("# ") -> MarkdownText(
                                text = line.removePrefix("# "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                bottomSpacing = 8.dp
                            )
                            line.startsWith("## ") -> MarkdownText(
                                text = line.removePrefix("## "),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                bottomSpacing = 6.dp
                            )
                            line.startsWith("### ") -> MarkdownText(
                                text = line.removePrefix("### "),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                bottomSpacing = 4.dp
                            )
                            line.startsWith("---") -> HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            line.isBlank() -> Spacer(Modifier.height(4.dp))
                            line.trimStart().startsWith("- ") -> {
                                val indent = (line.length - line.trimStart().length) / 2
                                MarkdownText(
                                    text = line.trimStart().removePrefix("- ").trimStart(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    isBullet = true,
                                    indentLevel = indent
                                )
                            }
                            else -> MarkdownText(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    bottomSpacing: Dp = 0.dp,
    isBullet: Boolean = false,
    indentLevel: Int = 0
) {
    val annotated = buildAnnotatedString {
        val parts = text.trim().split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
            } else {
                append(part)
            }
        }
    }

    if (isBullet) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(start = (indentLevel * 16).dp, bottom = 4.dp)
        ) {
            Text(
                "\u2022  ",
                style = style,
                color = MaterialTheme.colorScheme.primary
            )
            Text(annotated, style = style, color = color, fontWeight = fontWeight)
        }
    } else {
        Text(
            text = annotated,
            style = style,
            modifier = modifier,
            color = color,
            fontWeight = fontWeight
        )
    }
    if (bottomSpacing > 0.dp) {
        Spacer(Modifier.height(bottomSpacing))
    }
}
