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

import androidx.compose.ui.graphics.vector.ImageVector
import org.lineageos.updater.model.Update
import org.lineageos.updater.model.UpdateInfo
import org.lineageos.updater.model.UpdateStatus

data class PreferencesData(
    val autoCheckInterval: Int = 0,
    val autoDelete: Boolean = false,
    val meteredNetworkWarning: Boolean = true,
    val abPerfMode: Boolean = false,
    val updateRecovery: Boolean = false
)

data class UiState(
    var currentScreen: String = "Home",
    var showChangelog: Boolean = false,
    var latestUpdate: UpdateInfo? = null,
    var updateStatus: UpdateStatus? = null,
    var downloadProgress: Float = 0f,
    var downloadedMB: Int = 0,
    var totalMB: Int = 0,
    var isRefreshing: Boolean = false,
    var showImportDialog: Boolean = false,
    var showPreferencesDialog: Boolean = false,
    var showWelcomeDialog: Boolean = false,
    var toastMessage: String? = null,
    var importSuccessUpdate: Update? = null,
    var changelog: String = "",
    var isLoadingChangelog: Boolean = false
)

data class UpdaterCallbacks(
    val onStartDownload: (UpdateInfo) -> Unit,
    val onFinish: () -> Unit,
    val onScreenChange: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onShowPreferences: () -> Unit,
    val onImportLocal: () -> Unit,
    val onExportUpdate: (UpdateInfo) -> Unit,
    val onPause: (UpdateInfo) -> Unit,
    val onResume: (UpdateInfo) -> Unit,
    val onInstalled: (UpdateInfo) -> Unit,
    val onVerified: (UpdateInfo) -> Unit,
    val onDelete: (UpdateInfo) -> Unit
)

data class ToolbarItem(
    val name: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
