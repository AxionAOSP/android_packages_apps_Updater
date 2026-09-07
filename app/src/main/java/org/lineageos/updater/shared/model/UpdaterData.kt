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
package org.lineageos.updater.shared.model

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
    val latestUpdate: UpdateInfo? = null,
    val updateStatus: UpdateStatus? = null,
    val downloadProgress: Float = 0f,
    val downloadedMB: Int = 0,
    val totalMB: Int = 0,
    val isRefreshing: Boolean = false,
    val showImportDialog: Boolean = false,
    val showPreferencesDialog: Boolean = false,
    val showWelcomeDialog: Boolean = false,
    val showBatteryLowDialog: Boolean = false,
    val showScratchMountedDialog: Boolean = false,
    val toastMessage: String? = null,
    val importSuccessUpdate: Update? = null,
    val importErrorReason: String? = null,
    val changelog: String = "",
    val isLoadingChangelog: Boolean = false,
    val installProgress: Int? = null
)

data class UpdaterCallbacks(
    val onStartDownload: (UpdateInfo) -> Unit,
    val onFinish: () -> Unit,
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
