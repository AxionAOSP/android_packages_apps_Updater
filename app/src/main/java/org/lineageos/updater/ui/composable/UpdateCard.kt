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

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.axion.compose.theme.AxionColors
import org.lineageos.updater.R
import org.lineageos.updater.model.UpdateInfo
import org.lineageos.updater.model.UpdateStatus
import org.lineageos.updater.shared.model.UpdaterCallbacks
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateCard(
    status: UpdateStatus,
    update: UpdateInfo,
    downloadProgress: Float,
    installProgress: Int,
    callbacks: UpdaterCallbacks
) {
    val currentUpdate by rememberUpdatedState(update)

    val showProgress = status == UpdateStatus.INSTALLING ||
        status == UpdateStatus.DOWNLOADING ||
        status == UpdateStatus.STARTING ||
        downloadProgress > 0f

    val indicatorProgress: () -> Float = when (status) {
        UpdateStatus.INSTALLING -> { { installProgress / 100f } }
        else -> { { downloadProgress } }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = AxionColors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = update.getName() ?: stringResource(R.string.local_update_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    val fileSize = update.getFileSize().takeIf { it > 0L }?.let { formatFileSize(it) }
                    val timestamp = update.getTimestamp().takeIf { it > 0L }?.let { formatTimestamp(it) }
                    val type = update.getType() ?: stringResource(R.string.local_update_name)
                    val metadata = listOfNotNull(
                        update.getVersion()?.let { "v$it" },
                        fileSize,
                        timestamp,
                        type
                    ).joinToString(" \u2022 ")
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showProgress) {
                    if (status == UpdateStatus.STARTING && downloadProgress == 0f) {
                        LoadingIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = indicatorProgress,
                            modifier = Modifier.size(48.dp),
                            color = if (status == UpdateStatus.INSTALLING)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round
                        )
                    }
                } else if (currentUpdate.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED ||
                    downloadProgress >= 1f
                ) {
                    IconButton(onClick = { callbacks.onExportUpdate(currentUpdate) }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(R.string.menu_export_update),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showProgress) {
                Spacer(Modifier.height(16.dp))

                LinearWavyProgressIndicator(
                    progress = indicatorProgress,
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Text(
                    text = "${(indicatorProgress() * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            ActionButtons(
                status = status,
                update = update,
                downloadProgress = downloadProgress,
                callbacks = callbacks
            )
        }
    }
}

@Composable
private fun ActionButtons(
    status: UpdateStatus,
    update: UpdateInfo,
    downloadProgress: Float,
    callbacks: UpdaterCallbacks
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (downloadProgress > 0f && status != UpdateStatus.INSTALLED) {
            TextButton(onClick = { callbacks.onDelete(update) }) {
                Icon(Icons.Default.Delete, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.menu_delete_update))
            }
        }

        Spacer(Modifier.weight(1f))

        when (status) {
            UpdateStatus.UNKNOWN,
            UpdateStatus.DELETED -> {
                Button(
                    onClick = { callbacks.onStartDownload(update) },
                    enabled = update.getAvailableOnline()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_download))
                }
            }
            UpdateStatus.DOWNLOADING,
            UpdateStatus.STARTING -> {
                FilledTonalButton(onClick = { callbacks.onPause(update) }) {
                    Icon(Icons.Default.Pause, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_pause))
                }
            }
            UpdateStatus.PAUSED -> {
                Button(onClick = { callbacks.onResume(update) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_resume))
                }
            }
            UpdateStatus.PAUSED_ERROR,
            UpdateStatus.VERIFICATION_FAILED -> {
                Button(onClick = { callbacks.onStartDownload(update) }) {
                    Icon(Icons.Default.Download, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_retry))
                }
            }
            UpdateStatus.VERIFIED -> {
                Button(onClick = { callbacks.onVerified(update) }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_install))
                }
            }
            UpdateStatus.INSTALLED -> {
                Button(onClick = { callbacks.onInstalled(update) }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reboot))
                }
            }
            UpdateStatus.INSTALLATION_FAILED,
            UpdateStatus.INSTALLATION_CANCELLED,
            UpdateStatus.INSTALLATION_SUSPENDED -> {
                Button(onClick = { callbacks.onVerified(update) }) {
                    Icon(Icons.Default.Download, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_retry))
                }
            }
            else -> {
                FilledTonalButton(onClick = { }, enabled = false) {
                    Text(stringResource(R.string.action_na))
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.0f MB", mb)
        else -> "${bytes / 1024} KB"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}
