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

import android.os.SystemProperties
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import org.lineageos.updater.R
import org.lineageos.updater.misc.Constants
import org.lineageos.updater.misc.Utils
import org.lineageos.updater.model.Update
import org.lineageos.updater.shared.model.PreferencesData

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportProgressDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.local_update_import)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(modifier = Modifier.size(48.dp))
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDialog(
    onDismiss: () -> Unit,
    onSave: (PreferencesData) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var autoCheckInterval by remember {
        mutableIntStateOf(Utils.getUpdateCheckSetting(context))
    }
    var autoDelete by remember {
        mutableStateOf(prefs.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false))
    }
    var meteredNetworkWarning by remember {
        mutableStateOf(prefs.getBoolean(Constants.PREF_METERED_NETWORK_WARNING,
            prefs.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true)))
    }
    var abPerfMode by remember {
        mutableStateOf(prefs.getBoolean(Constants.PREF_AB_PERF_MODE, false))
    }
    var updateRecovery by remember {
        mutableStateOf(if (Utils.isRecoveryUpdateExecPresent()) {
            SystemProperties.getBoolean(Constants.UPDATE_RECOVERY_PROPERTY, false)
        } else true)
    }

    AlertDialog(
        onDismissRequest = {
            onSave(PreferencesData(autoCheckInterval, autoDelete, meteredNetworkWarning, abPerfMode, updateRecovery))
            onDismiss()
        },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.menu_preferences)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var expanded by remember { mutableStateOf(false) }
                val intervals = listOf(
                    stringResource(R.string.menu_auto_updates_check_interval_never),
                    stringResource(R.string.menu_auto_updates_check_interval_daily),
                    stringResource(R.string.menu_auto_updates_check_interval_weekly),
                    stringResource(R.string.menu_auto_updates_check_interval_monthly)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = intervals.getOrNull(autoCheckInterval) ?: intervals[0],
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.menu_auto_updates_check)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        intervals.forEachIndexed { index, interval ->
                            DropdownMenuItem(
                                text = { Text(interval) },
                                onClick = {
                                    autoCheckInterval = index
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                PreferenceSwitch(
                    title = stringResource(R.string.menu_auto_delete_updates),
                    checked = autoDelete,
                    onCheckedChange = { autoDelete = it }
                )

                PreferenceSwitch(
                    title = stringResource(R.string.menu_metered_network_warning),
                    checked = meteredNetworkWarning,
                    onCheckedChange = { meteredNetworkWarning = it }
                )

                if (Utils.isABDevice()) {
                    PreferenceSwitch(
                        title = stringResource(R.string.menu_ab_perf_mode),
                        checked = abPerfMode,
                        onCheckedChange = { abPerfMode = it }
                    )
                }

                if (!context.resources.getBoolean(R.bool.config_hideRecoveryUpdate)) {
                    PreferenceSwitch(
                        title = stringResource(R.string.menu_update_recovery),
                        checked = updateRecovery,
                        onCheckedChange = {
                            if (Utils.isRecoveryUpdateExecPresent()) {
                                updateRecovery = it
                            } else {
                                Toast.makeText(context,
                                    context.getString(R.string.toast_forced_update_recovery),
                                    Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = Utils.isRecoveryUpdateExecPresent()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(PreferencesData(autoCheckInterval, autoDelete, meteredNetworkWarning, abPerfMode, updateRecovery))
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.pref_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.pref_cancel))
            }
        }
    )
}

@Composable
private fun PreferenceSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.welcome_title)) },
        text = { Text(stringResource(R.string.welcome_message)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.info_dialog_ok))
            }
        }
    )
}

@Composable
fun ImportSuccessDialog(
    update: Update,
    onInstall: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onCancel() },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.local_update_import)) },
        text = {
            Text(stringResource(R.string.local_update_import_success, update.version))
        },
        confirmButton = {
            Button(onClick = onInstall) {
                Text(stringResource(R.string.local_update_import_install))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun ImportWarningDialog(
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.local_update_import_warning_title)) },
        text = { Text(stringResource(R.string.local_update_import_warning_message)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.info_dialog_ok))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
