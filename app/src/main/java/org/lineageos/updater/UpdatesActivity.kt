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

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.lifecycle.lifecycleScope
import org.lineageos.updater.controller.UpdaterController
import org.lineageos.updater.controller.UpdaterService
import org.lineageos.updater.download.DownloadClient
import org.lineageos.updater.misc.Constants
import org.lineageos.updater.misc.Utils
import org.lineageos.updater.model.Update
import org.lineageos.updater.model.UpdateInfo
import org.lineageos.updater.model.UpdateStatus
import org.lineageos.updater.R
import java.io.File
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlinx.coroutines.*
import org.json.JSONObject

class UpdatesActivity : ComponentActivity(), UpdateImporter.Callbacks {

    private var mUpdaterService: UpdaterService? = null
    private var mUpdateImporter: UpdateImporter? = null
    private var mToBeExported: UpdateInfo? = null

    private val uiState = mutableStateOf(UiState())

    private val mExportUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { exportUpdate(it) }
    }

    private val mImportUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        mUpdateImporter?.onResult(9061, result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        mUpdateImporter = UpdateImporter(this, this)

        lifecycleScope.launch {
            uiState.value = uiState.value.copy(isLoadingChangelog = true)
            val changelogResult = getChangelog()
            uiState.value = uiState.value.copy(
                changelog = changelogResult,
                isLoadingChangelog = false
            )
        }

        setContent {
            val state by uiState

            val callbacks = UpdaterCallbacks(
                onStartDownload = { update ->
                    mUpdaterService?.updaterController?.takeIf { Utils.isNetworkAvailable(this@UpdatesActivity) }
                        ?.startDownload(update.downloadId)
                        ?.also { uiState.value = uiState.value.copy(currentScreen = "Update") }
                        ?: showToast("No internet connection or service not ready.")
                },
                onPause = { update ->
                    mUpdaterService?.updaterController?.pauseDownload(update.downloadId)
                        ?: showToast("Unable to pause download.")
                },
                onResume = { update ->
                    mUpdaterService?.updaterController?.takeIf { Utils.isNetworkAvailable(this@UpdatesActivity) }
                        ?.startDownload(update.downloadId)
                        ?.also { uiState.value = uiState.value.copy(currentScreen = "Update") }
                        ?: showToast("No internet connection or service not ready.")
                },
                onDelete = { update ->
                    mUpdaterService?.updaterController?.deleteUpdate(update.downloadId)
                        ?: showToast("Unable to delete download.")
                },
                onInstalled = { update ->
                    (getSystemService(Context.POWER_SERVICE) as PowerManager).reboot(null)
                },
                onVerified = { update ->
                    Utils.triggerUpdate(this@UpdatesActivity, update.downloadId)
                },
                onFinish = {
                    uiState.value = uiState.value.copy(
                        currentScreen = "Home",
                        downloadProgress = 0f,
                        downloadedMB = 0,
                        totalMB = 0
                    )
                    finish()
                },
                onScreenChange = { uiState.value = uiState.value.copy(currentScreen = it) },
                onRefresh = { fetchList(true) },
                onShowPreferences = { uiState.value = uiState.value.copy(showPreferencesDialog = true) },
                onImportLocal = { importUpdate() },
                onExportUpdate = { exportUpdate(it) }
            )

            UpdaterTheme {
                UpdaterApp(
                    uiState = state,
                    preferences = PreferencesData(),
                    callbacks = callbacks,
                    changelog = when {
                        state.isLoadingChangelog -> "Loading changelog..."
                        state.changelog.isEmpty() -> "No changelog available."
                        else -> state.changelog
                    }
                )
            }

            if (state.showImportDialog) ImportProgressDialog(
                onDismiss = { uiState.value = uiState.value.copy(showImportDialog = false); mUpdateImporter?.stopImport() }
            )
            if (state.showPreferencesDialog) PreferencesDialog(
                onDismiss = { uiState.value = uiState.value.copy(showPreferencesDialog = false) },
                onSave = ::savePreferences
            )
            if (state.showWelcomeDialog) WelcomeDialog(
                onDismiss = { uiState.value = uiState.value.copy(showWelcomeDialog = false); markWelcomeSeen(); maybeShowNotificationPermissionPrompt() }
            )
            state.importSuccessUpdate?.let { update ->
                ImportSuccessDialog(
                    update = update,
                    onInstall = {
                        getUpdatesList()
                        Utils.triggerUpdate(this, update.downloadId)
                        uiState.value = uiState.value.copy(importSuccessUpdate = null)
                    },
                    onCancel = {
                        UpdaterController.getInstance(this).deleteUpdate(update.downloadId)
                        uiState.value = uiState.value.copy(importSuccessUpdate = null)
                    }
                )
            }
            state.toastMessage?.let {
                LaunchedEffect(it) {
                    Toast.makeText(this@UpdatesActivity, it, Toast.LENGTH_SHORT).show()
                    uiState.value = uiState.value.copy(toastMessage = null)
                }
            }
        }

        maybeShowWelcomeMessage()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mUpdateImporter?.onResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, UpdaterService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, IntentFilter().apply {
            addAction(UpdaterController.ACTION_UPDATE_STATUS)
            addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS)
            addAction(UpdaterController.ACTION_INSTALL_PROGRESS)
            addAction(UpdaterController.ACTION_UPDATE_REMOVED)
        })
    }

    override fun onPause() {
        if (uiState.value.showImportDialog) {
            uiState.value = uiState.value.copy(showImportDialog = false)
            mUpdateImporter?.stopImport()
        }
        super.onPause()
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
        mUpdaterService?.let { unbindService(mConnection) }
        super.onStop()
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mUpdaterService = (service as UpdaterService.LocalBinder).service
            getUpdatesList()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mUpdaterService = null
        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID)
            when (intent.action) {
                UpdaterController.ACTION_UPDATE_STATUS -> {
                    onDlStateChange(downloadId)
                    checkUpdates()
                }
                UpdaterController.ACTION_DOWNLOAD_PROGRESS -> {
                    uiState.value = uiState.value.copy(
                        downloadProgress = intent.getFloatExtra(UpdaterController.EXTRA_PROGRESS, 0f),
                        downloadedMB = (intent.getLongExtra(UpdaterController.EXTRA_DOWNLOADED_BYTES, 0L) / (1024 * 1024)).toInt(),
                        totalMB = (intent.getLongExtra(UpdaterController.EXTRA_TOTAL_BYTES, 0L) / (1024 * 1024)).toInt()
                    )
                }
                UpdaterController.ACTION_INSTALL_PROGRESS -> {
                    uiState.value = uiState.value.copy(
                        downloadProgress = intent.getFloatExtra(UpdaterController.EXTRA_PROGRESS, 0f),
                        installProgress = intent.getIntExtra(UpdaterController.EXTRA_INSTALL_PROGRESS, 0)
                    )
                }
                UpdaterController.ACTION_UPDATE_REMOVED -> checkUpdates()
            }
        }
    }

    private fun checkUpdates() {
        val latestUpdate = mUpdaterService
            ?.updaterController
            ?.updates
            ?.maxByOrNull { it.timestamp }
            ?: null
        uiState.value = uiState.value.copy(
            latestUpdate = latestUpdate,
            updateStatus = latestUpdate?.getStatus() ?: UpdateStatus.UNKNOWN
        )
    }

    private fun onDlStateChange(downloadId: String?) {
        val status = mUpdaterService?.updaterController?.getUpdate(downloadId ?: return)?.getStatus() ?: UpdateStatus.UNKNOWN
        val message = when (status) {
            UpdateStatus.PAUSED_ERROR -> getString(R.string.snack_download_failed)
            UpdateStatus.VERIFICATION_FAILED -> getString(R.string.snack_download_verification_failed)
            UpdateStatus.VERIFIED -> getString(R.string.snack_download_verified)
            else -> null
        }
        message?.let { showToast(it) }
    }

    private fun loadUpdatesList(jsonFile: File, manualRefresh: Boolean) {
        try {
            val controller = mUpdaterService?.updaterController ?: return
            val updates = Utils.parseJson(jsonFile, true)
            val updatesOnline = updates.map { it.downloadId }
            val newUpdates = updates.any { controller.addUpdate(it) }
            controller.setUpdatesAvailableOnline(updatesOnline, true)
            if (manualRefresh) showToast(getString(if (newUpdates) R.string.snack_updates_found else R.string.snack_no_updates_found))
            checkUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Error while parsing json list", e)
        }
    }

    private fun getUpdatesList() {
        val jsonFile = Utils.getCachedUpdateList(this)
        if (jsonFile.exists()) loadUpdatesList(jsonFile, false) else fetchList(false)
    }

    private fun processNewJson(json: File, jsonNew: File, manualRefresh: Boolean) {
        try {
            loadUpdatesList(jsonNew, manualRefresh)
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putLong(Constants.PREF_LAST_UPDATE_CHECK, System.currentTimeMillis())
                .apply()

            if (json.exists() && Utils.isUpdateCheckEnabled(this) && Utils.checkForNewUpdates(json, jsonNew)) {
                UpdatesCheckReceiver.updateRepeatingUpdatesCheck(this)
            }
            UpdatesCheckReceiver.cancelUpdatesCheck(this)
            jsonNew.renameTo(json)
        } catch (e: Exception) {
            Log.e(TAG, "Could not read json", e)
            showToast(getString(R.string.snack_updates_check_failed))
        }
    }

    private fun fetchList(manualRefresh: Boolean) {
        val jsonFile = Utils.getCachedUpdateList(this)
        val jsonFileTmp = File("${jsonFile.absolutePath}${UUID.randomUUID()}")
        val url = Utils.getServerURL(this)
        uiState.value = uiState.value.copy(isRefreshing = true)
        try {
            DownloadClient.Builder()
                .setUrl(url)
                .setDestination(jsonFileTmp)
                .setDownloadCallback(object : DownloadClient.DownloadCallback {
                    override fun onFailure(cancelled: Boolean) = runOnUiThread {
                        if (!cancelled) showToast(getString(R.string.snack_updates_check_failed))
                        uiState.value = uiState.value.copy(isRefreshing = false)
                    }
                    override fun onResponse(headers: DownloadClient.Headers) {}
                    override fun onSuccess() = runOnUiThread {
                        processNewJson(jsonFile, jsonFileTmp, manualRefresh)
                        uiState.value = uiState.value.copy(isRefreshing = false)
                    }
                })
                .build()
                .start()
        } catch (exception: IOException) {
            Log.e(TAG, "Could not build download client")
            showToast(getString(R.string.snack_updates_check_failed))
            uiState.value = uiState.value.copy(isRefreshing = false)
        }
    }

    override fun onImportStarted() {
        uiState.value = uiState.value.copy(showImportDialog = true)
    }

    override fun onImportCompleted(update: Update?) {
        uiState.value = uiState.value.copy(showImportDialog = false)
        if (update == null) showToast(getString(R.string.local_update_import_failure))
        else uiState.value = uiState.value.copy(importSuccessUpdate = update)
    }

    private fun exportUpdate(update: UpdateInfo) {
        mToBeExported = update
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, update.name)
        }
        mExportUpdate.launch(intent)
    }

    private fun importUpdate() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        mImportUpdate.launch(intent)
    }

    private fun exportUpdate(uri: Uri) {
        startService(Intent(this, ExportUpdateService::class.java).apply {
            action = ExportUpdateService.ACTION_START_EXPORTING
            putExtra(ExportUpdateService.EXTRA_SOURCE_FILE, mToBeExported?.file)
            putExtra(ExportUpdateService.EXTRA_DEST_URI, uri)
        })
    }

    private fun savePreferences(preferences: PreferencesData) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .putInt(Constants.PREF_AUTO_UPDATES_CHECK_INTERVAL, preferences.autoCheckInterval)
            .putBoolean(Constants.PREF_AUTO_DELETE_UPDATES, preferences.autoDelete)
            .putBoolean(Constants.PREF_METERED_NETWORK_WARNING, preferences.meteredNetworkWarning)
            .putBoolean(Constants.PREF_AB_PERF_MODE, preferences.abPerfMode)
            .apply()
        if (Utils.isUpdateCheckEnabled(this)) {
            UpdatesCheckReceiver.scheduleRepeatingUpdatesCheck(this)
        } else {
            UpdatesCheckReceiver.cancelRepeatingUpdatesCheck(this)
            UpdatesCheckReceiver.cancelUpdatesCheck(this)
        }
        val controller = mUpdaterService?.updaterController ?: return
        if (Utils.isABDevice()) {
            controller!!.setPerformanceMode(preferences.abPerfMode)
        }
    }

    private fun maybeShowWelcomeMessage() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean(Constants.HAS_SEEN_WELCOME_MESSAGE, false)) uiState.value = uiState.value.copy(showWelcomeDialog = true)
    }

    private fun markWelcomeSeen() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(Constants.HAS_SEEN_WELCOME_MESSAGE, true)
            .apply()
    }

    private fun showToast(message: String) {
        uiState.value = uiState.value.copy(toastMessage = message)
    }

    private suspend fun getChangelog(): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(getString(R.string.changelog_url))
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            connection.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showToast("Notifications enabled")
            } else {
                showToast("Notifications permission denied")
            }
            markNotificationPermissionRequested()
        }

    private fun maybeShowNotificationPermissionPrompt() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val requested = prefs.getBoolean(Constants.HAS_REQUESTED_NOTIFICATION_PERMISSION, false)
        if (!requested) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                markNotificationPermissionRequested()
            }
        }
    }

    private fun markNotificationPermissionRequested() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(Constants.HAS_REQUESTED_NOTIFICATION_PERMISSION, true)
            .apply()
    }

    companion object {
        private const val TAG = "UpdatesActivity"
    }
}
