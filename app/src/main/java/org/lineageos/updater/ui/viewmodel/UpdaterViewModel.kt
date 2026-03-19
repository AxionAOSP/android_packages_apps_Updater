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
package org.lineageos.updater.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.updater.R
import org.lineageos.updater.UpdatesCheckReceiver
import org.lineageos.updater.controller.UpdaterController
import org.lineageos.updater.controller.UpdaterService
import org.lineageos.updater.download.DownloadClient
import org.lineageos.updater.misc.Constants
import org.lineageos.updater.misc.Utils
import org.lineageos.updater.model.Update
import org.lineageos.updater.model.UpdateInfo
import org.lineageos.updater.model.UpdateStatus
import org.lineageos.updater.shared.model.PreferencesData
import org.lineageos.updater.shared.model.UiState
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class UpdaterViewModel(application: Application) : AndroidViewModel(application) {

    val uiState = mutableStateOf(UiState())

    private var updaterController: UpdaterController? = null

    init {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoadingChangelog = true)
            val changelogResult = getChangelog()
            uiState.value = uiState.value.copy(
                changelog = changelogResult,
                isLoadingChangelog = false
            )
        }
        maybeShowWelcomeMessage()
    }

    fun setController(controller: UpdaterController?) {
        updaterController = controller
        if (controller != null) getUpdatesList()
    }

    fun checkUpdates() {
        val latestUpdate = updaterController
            ?.updates
            ?.maxByOrNull { it.timestamp }
        uiState.value = uiState.value.copy(
            latestUpdate = latestUpdate,
            updateStatus = latestUpdate?.getStatus() ?: UpdateStatus.UNKNOWN
        )
    }

    fun onDlStateChange(downloadId: String?) {
        val ctx = getApplication<Application>()
        val status = updaterController?.getUpdate(downloadId ?: return)?.getStatus()
            ?: UpdateStatus.UNKNOWN
        val message = when (status) {
            UpdateStatus.PAUSED_ERROR -> ctx.getString(R.string.snack_download_failed)
            UpdateStatus.VERIFICATION_FAILED -> ctx.getString(R.string.snack_download_verification_failed)
            UpdateStatus.VERIFIED -> ctx.getString(R.string.snack_download_verified)
            else -> null
        }
        message?.let { showToast(it) }
    }

    fun handleDownloadProgress(progress: Float, downloadedBytes: Long, totalBytes: Long) {
        uiState.value = uiState.value.copy(
            downloadProgress = progress,
            downloadedMB = (downloadedBytes / (1024 * 1024)).toInt(),
            totalMB = (totalBytes / (1024 * 1024)).toInt()
        )
    }

    fun handleInstallProgress(progress: Float, installProgress: Int) {
        uiState.value = uiState.value.copy(
            downloadProgress = progress,
            installProgress = installProgress
        )
    }

    fun handleUpdateRemoved() {
        checkUpdates()
    }

    fun startDownload(update: UpdateInfo) {
        val ctx = getApplication<Application>()
        updaterController?.takeIf { Utils.isNetworkAvailable(ctx) }
            ?.startDownload(update.downloadId)
            ?: showToast(ctx.getString(R.string.no_connection_or_service))
    }

    fun pauseDownload(update: UpdateInfo) {
        val ctx = getApplication<Application>()
        updaterController?.pauseDownload(update.downloadId)
            ?: showToast(ctx.getString(R.string.unable_to_pause))
    }

    fun resumeDownload(update: UpdateInfo) {
        val ctx = getApplication<Application>()
        updaterController?.takeIf { Utils.isNetworkAvailable(ctx) }
            ?.startDownload(update.downloadId)
            ?: showToast(ctx.getString(R.string.no_connection_or_service))
    }

    fun deleteUpdate(update: UpdateInfo) {
        updaterController?.deleteUpdate(update.downloadId)
    }

    fun onImportStarted() {
        uiState.value = uiState.value.copy(showImportDialog = true)
    }

    fun onImportCompleted(update: Update?) {
        val ctx = getApplication<Application>()
        uiState.value = uiState.value.copy(showImportDialog = false)
        if (update == null) showToast(ctx.getString(R.string.local_update_import_failure))
        else uiState.value = uiState.value.copy(importSuccessUpdate = update)
    }

    fun savePreferences(preferences: PreferencesData) {
        val ctx = getApplication<Application>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        prefs.edit()
            .putInt(Constants.PREF_AUTO_UPDATES_CHECK_INTERVAL, preferences.autoCheckInterval)
            .putBoolean(Constants.PREF_AUTO_DELETE_UPDATES, preferences.autoDelete)
            .putBoolean(Constants.PREF_METERED_NETWORK_WARNING, preferences.meteredNetworkWarning)
            .putBoolean(Constants.PREF_AB_PERF_MODE, preferences.abPerfMode)
            .apply()
        if (Utils.isUpdateCheckEnabled(ctx)) {
            UpdatesCheckReceiver.scheduleRepeatingUpdatesCheck(ctx)
        } else {
            UpdatesCheckReceiver.cancelRepeatingUpdatesCheck(ctx)
            UpdatesCheckReceiver.cancelUpdatesCheck(ctx)
        }
        val controller = updaterController ?: return
        if (Utils.isABDevice()) {
            controller.setPerformanceMode(preferences.abPerfMode)
        }
    }

    fun showToast(message: String) {
        uiState.value = uiState.value.copy(toastMessage = message)
    }

    fun clearToast() {
        uiState.value = uiState.value.copy(toastMessage = null)
    }

    fun showPreferences() {
        uiState.value = uiState.value.copy(showPreferencesDialog = true)
    }

    fun dismissPreferences() {
        uiState.value = uiState.value.copy(showPreferencesDialog = false)
    }

    fun dismissImport() {
        uiState.value = uiState.value.copy(showImportDialog = false)
    }

    fun dismissWelcome() {
        uiState.value = uiState.value.copy(showWelcomeDialog = false)
        markWelcomeSeen()
    }

    fun clearImportSuccess() {
        uiState.value = uiState.value.copy(importSuccessUpdate = null)
    }

    fun deleteImportedUpdate(downloadId: String) {
        val ctx = getApplication<Application>()
        UpdaterController.getInstance(ctx).deleteUpdate(downloadId)
        clearImportSuccess()
    }

    fun resetDownloadState() {
        uiState.value = uiState.value.copy(
            downloadProgress = 0f,
            downloadedMB = 0,
            totalMB = 0
        )
    }

    fun fetchList(manualRefresh: Boolean) {
        val ctx = getApplication<Application>()
        val jsonFile = Utils.getCachedUpdateList(ctx)
        val jsonFileTmp = File("${jsonFile.absolutePath}${UUID.randomUUID()}")
        val url = Utils.getServerURL(ctx)
        uiState.value = uiState.value.copy(isRefreshing = true)
        try {
            DownloadClient.Builder()
                .setUrl(url)
                .setDestination(jsonFileTmp)
                .setDownloadCallback(object : DownloadClient.DownloadCallback {
                    override fun onFailure(cancelled: Boolean) {
                        if (!cancelled) showToast(ctx.getString(R.string.snack_updates_check_failed))
                        uiState.value = uiState.value.copy(isRefreshing = false)
                    }
                    override fun onResponse(headers: DownloadClient.Headers) {}
                    override fun onSuccess() {
                        processNewJson(jsonFile, jsonFileTmp, manualRefresh)
                        uiState.value = uiState.value.copy(isRefreshing = false)
                    }
                })
                .build()
                .start()
        } catch (exception: IOException) {
            Log.e(TAG, "Could not build download client")
            showToast(ctx.getString(R.string.snack_updates_check_failed))
            uiState.value = uiState.value.copy(isRefreshing = false)
        }
    }

    fun getUpdatesList() {
        val ctx = getApplication<Application>()
        val jsonFile = Utils.getCachedUpdateList(ctx)
        if (jsonFile.exists()) loadUpdatesList(jsonFile, false) else fetchList(false)
    }

    private fun loadUpdatesList(jsonFile: File, manualRefresh: Boolean) {
        val ctx = getApplication<Application>()
        try {
            val controller = updaterController ?: return
            val updates = Utils.parseJson(jsonFile, true)
            val updatesOnline = updates.map { it.downloadId }
            val newUpdates = updates.any { controller.addUpdate(it) }
            controller.setUpdatesAvailableOnline(updatesOnline, true)
            if (manualRefresh) {
                showToast(ctx.getString(
                    if (newUpdates) R.string.snack_updates_found
                    else R.string.snack_no_updates_found
                ))
            }
            checkUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Error while parsing json list", e)
        }
    }

    private fun processNewJson(json: File, jsonNew: File, manualRefresh: Boolean) {
        val ctx = getApplication<Application>()
        try {
            loadUpdatesList(jsonNew, manualRefresh)
            PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putLong(Constants.PREF_LAST_UPDATE_CHECK, System.currentTimeMillis())
                .apply()
            if (json.exists() && Utils.isUpdateCheckEnabled(ctx) &&
                Utils.checkForNewUpdates(json, jsonNew)
            ) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(ctx)
            }
            UpdatesCheckReceiver.cancelUpdatesCheck(ctx)
            jsonNew.renameTo(json)
        } catch (e: Exception) {
            Log.e(TAG, "Could not read json", e)
            showToast(ctx.getString(R.string.snack_updates_check_failed))
        }
    }

    private fun maybeShowWelcomeMessage() {
        val ctx = getApplication<Application>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        if (!prefs.getBoolean(Constants.HAS_SEEN_WELCOME_MESSAGE, false)) {
            uiState.value = uiState.value.copy(showWelcomeDialog = true)
        }
    }

    private fun markWelcomeSeen() {
        val ctx = getApplication<Application>()
        PreferenceManager.getDefaultSharedPreferences(ctx)
            .edit()
            .putBoolean(Constants.HAS_SEEN_WELCOME_MESSAGE, true)
            .apply()
    }

    private suspend fun getChangelog(): String = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        var connection: HttpURLConnection? = null
        try {
            val url = URL(ctx.getString(R.string.changelog_url))
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val TAG = "UpdaterViewModel"
    }
}
