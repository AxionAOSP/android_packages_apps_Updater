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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.compose.preferences.ClickablePreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.axion.deviceinfo.DeviceInfoProvider
import org.lineageos.updater.R
import org.lineageos.updater.model.UpdateStatus
import org.lineageos.updater.shared.model.UiState
import org.lineageos.updater.shared.model.UpdaterCallbacks

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdaterApp(
    uiState: UiState,
    callbacks: UpdaterCallbacks,
    changelog: String
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showImportWarning by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val deviceInfo = remember { DeviceInfoProvider.getDeviceInfo(context) }

    val latestUpdate = uiState.latestUpdate
    val status = uiState.updateStatus ?: UpdateStatus.UNKNOWN
    val hasUpdate = latestUpdate != null

    val isOfficial = deviceInfo.axionBuildType.equals("Official", ignoreCase = true)
    val isBeta = deviceInfo.axionBuildType.contains("beta", ignoreCase = true)

    if (showImportWarning) {
        ImportWarningDialog(
            onDismiss = {
                showImportWarning = false
                callbacks.onImportLocal()
            },
            onCancel = { showImportWarning = false }
        )
    }

    if (showChangelog) {
        ChangelogBottomSheet(
            changelog = changelog,
            onDismiss = { showChangelog = false }
        )
    }

    AxionScaffold(
        title = stringResource(R.string.system_updates),
        onBackClick = callbacks.onFinish,
        collapsedByDefault = false,
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_preferences)) },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        callbacks.onShowPreferences()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.local_update_import)) },
                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        showImportWarning = true
                    }
                )
            }
        }
    ) { contentPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = callbacks.onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                StatusBanner(
                    version = deviceInfo.axionVersion,
                    deviceModel = deviceInfo.model,
                    maintainer = deviceInfo.maintainer,
                    isOfficial = isOfficial,
                    isBeta = isBeta,
                    isUpToDate = !hasUpdate
                )

                Spacer(Modifier.height(16.dp))

                val motionScheme = MaterialTheme.motionScheme

                AnimatedVisibility(
                    visible = hasUpdate,
                    enter = expandVertically(motionScheme.defaultSpatialSpec()) + fadeIn(motionScheme.defaultEffectsSpec()),
                    exit = shrinkVertically(motionScheme.defaultSpatialSpec()) + fadeOut(motionScheme.defaultEffectsSpec())
                ) {
                    Column {
                        latestUpdate?.let { update ->
                            UpdateCard(
                                status = status,
                                update = update,
                                downloadProgress = uiState.downloadProgress,
                                installProgress = uiState.installProgress ?: 0,
                                callbacks = callbacks
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                PreferenceGroup {
                    item {
                        ClickablePreference(
                            title = stringResource(R.string.info_label_android),
                            summary = deviceInfo.androidVersion,
                            icon = Icons.Default.Android,
                            onClick = { }
                        )
                    }
                    item {
                        ClickablePreference(
                            title = stringResource(R.string.info_label_date),
                            summary = deviceInfo.securityPatch,
                            icon = Icons.Default.Security,
                            onClick = { }
                        )
                    }
                    item {
                        ClickablePreference(
                            title = stringResource(R.string.info_label_build),
                            summary = deviceInfo.buildDate.takeLast(11).trim(),
                            icon = Icons.Default.CalendarMonth,
                            onClick = { }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                val firstLine = changelog.lines()
                    .firstOrNull { it.isNotBlank() }
                    ?.removePrefix("# ")
                    ?.take(40) ?: ""

                PreferenceGroup {
                    item {
                        ClickablePreference(
                            title = stringResource(R.string.changelog_title),
                            summary = when {
                                uiState.isLoadingChangelog -> null
                                firstLine.isNotEmpty() -> firstLine
                                else -> null
                            },
                            icon = Icons.AutoMirrored.Filled.Article,
                            onClick = { showChangelog = true }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                FilledTonalButton(
                    onClick = callbacks.onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.check_for_updates))
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
