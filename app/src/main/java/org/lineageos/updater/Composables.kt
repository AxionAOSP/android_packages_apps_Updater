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

import android.icu.text.SimpleDateFormat
import android.os.SystemProperties
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.preference.PreferenceManager
import org.lineageos.updater.misc.Constants
import org.lineageos.updater.misc.Utils
import org.lineageos.updater.model.Update
import org.lineageos.updater.model.UpdateInfo
import org.lineageos.updater.model.UpdateStatus
import kotlin.math.*
import java.util.Date
import java.util.Locale

@Composable
fun UpdaterApp(
    uiState: UiState,
    preferences: PreferencesData,
    callbacks: UpdaterCallbacks,
    changelog: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.currentScreen) {
            "Home" -> HomeScreen(uiState, callbacks, changelog)
            "Update" -> UpdateScreen(uiState, callbacks)
        }
        PillToolbar(
            selectedScreen = uiState.currentScreen,
            callbacks = callbacks
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: UiState,
    callbacks: UpdaterCallbacks,
    changelog: String
) {
    val maintainer = SystemProperties.get("persist.sys.axion_maintainer", "Unknown").replace("_", " ")
    val version = SystemProperties.get("ro.axion.build.version", "2.0")
    val deviceModel = SystemProperties.get("ro.product.model", "Unknown Device")
    val updated = uiState.latestUpdate == null

    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            UpdaterTopBar(
                title = stringResource(R.string.system_updates),
                onBack = callbacks.onFinish,
                onPreferences = callbacks.onShowPreferences
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VersionCard(
                version = version,
                maintainer = maintainer,
                deviceModel = deviceModel,
                changelog = changelog,
                updated = updated
            )

            Spacer(Modifier.height(48.dp))

            ChangelogTrigger(
                onSwipeUp = { showSheet = true }
            )
        }
    }

    if (showSheet) {
        ChangelogBottomSheet(
            changelog = changelog,
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    uiState: UiState,
    callbacks: UpdaterCallbacks
) {
    val latestUpdate = uiState.latestUpdate
    val status = uiState.updateStatus ?: UpdateStatus.UNKNOWN
    val downloadProgress = uiState.downloadProgress
    val installProgress = uiState.installProgress ?: 0

    Scaffold(
        topBar = {
            UpdaterTopBar(
                title = stringResource(R.string.updates_title),
                onBack = { callbacks.onScreenChange("Home") }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = callbacks.onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (latestUpdate != null) {
                    UpdateCard(
                        status = status,
                        update = latestUpdate,
                        downloadProgress = downloadProgress,
                        installProgress = installProgress,
                        callbacks = callbacks
                    )
                } else {
                    EmptyScreenIllustration(callbacks = callbacks)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onPreferences: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Thin
            )
        },
        navigationIcon = {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (onPreferences != null) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_preferences)) },
                        onClick = {
                            menuExpanded = false
                            onPreferences()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    )
}

@Composable
fun VersionCard(
    version: String,
    maintainer: String,
    deviceModel: String,
    changelog: String,
    updated: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val versionParts = version.split(".")
    val major = versionParts.getOrNull(0) ?: "2"
    val minor = versionParts.drop(1).joinToString(".", prefix = ".")

    val infiniteTransition = rememberInfiniteTransition()
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val displayVersion = SystemProperties.get("ro.axion.version")
    val isOfficial = displayVersion?.let {
        it.contains("official", ignoreCase = true) &&
        !it.contains("unofficial", ignoreCase = true)
    } ?: false
    val isBeta = displayVersion?.contains("beta", ignoreCase = true) ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 24.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = major,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 96.sp, fontWeight = FontWeight.Thin
                        ),
                        color = primaryColor
                    )
                    Text(
                        text = minor,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 96.sp, fontWeight = FontWeight.Thin
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (isBeta) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier
                                .padding(top = 20.dp, end = 24.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Science,
                                    contentDescription = "Experimental build",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Open BETA",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                WaveAnimation(
                    wavePhase = wavePhase,
                    color = primaryColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.Center)
                        .padding(top = 24.dp)
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(50),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AxionOS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Divider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Text(
                            text = stringResource(
                                if (updated) R.string.up_to_date else R.string.update_available
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (updated) {
                                Color(0xFF4CAF50)
                            } else {
                                Color(0xFFFFC107)
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = deviceModel + " • " + stringResource(
                                if (isOfficial) R.string.official_specifier else R.string.build_specifier
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " • $maintainer",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .widthIn(max = 160.dp)
                                .basicMarquee()
                        )
                    }

                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun WaveAnimation(
    wavePhase: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val amplitude = h / 3f
        val wavelength = w * 2f
        val phaseOffset = (wavePhase / (2 * PI).toFloat()) * wavelength
        val startX = -wavelength

        val path = Path().apply {
            moveTo(startX, h / 2)
            for (x in startX.toInt()..w.toInt()) {
                val y = h / 2 + amplitude *
                    sin(2 * PI * (x + phaseOffset) / wavelength).toFloat()
                lineTo(x.toFloat(), y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ChangelogTrigger(onSwipeUp: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    if (dragAmount < -20f) {
                        onSwipeUp()
                        change.consume()
                    }
                }
            }
            .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(R.string.changelog_guide),
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = arrowOffset.dp)
                    .alpha(arrowAlpha),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.changelog_guide),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(
    changelog: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        contentWindowInsets = { WindowInsets.navigationBars },
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = .32f),
        modifier = Modifier.padding(top = 72.dp),
        containerColor = MaterialTheme.colorScheme.background,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        content = {
            ChangelogScreen(
                changelog = changelog,
                onCgBack = onDismiss
            )
        }
    )
}

@Composable
fun ChangelogScreen(
    changelog: String,
    onCgBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.changelog_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )

        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            MarkdownChangelog(
                onCgBack = onCgBack,
                text = changelog,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MarkdownChangelog(
    text: String,
    modifier: Modifier = Modifier,
    onCgBack: () -> Unit
) {
    val lines = text.lines()
    val scrollState = rememberScrollState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (scrollState.value == 0 && available.y > 0) {
                    onCgBack()
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
            .verticalScroll(scrollState)
    ) {
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> MarkdownText(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                    bottomSpacing = 8.dp
                )
                line.startsWith("## ") -> MarkdownText(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    bottomSpacing = 6.dp
                )
                line.startsWith("### ") -> MarkdownText(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.titleLarge,
                    bottomSpacing = 4.dp
                )
                line.startsWith("#### ") -> MarkdownText(
                    text = line.removePrefix("#### "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                line.startsWith("###### ") -> MarkdownText(
                    text = line.removePrefix("###### "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Thin
                )
                line.startsWith("---") -> HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                line.isBlank() -> Spacer(modifier = Modifier.height(6.dp))
                line.trimStart().startsWith("- ") -> {
                    val indentLevel = (line.length - line.trimStart().length) / 2
                    MarkdownText(
                        text = line.trimStart().removePrefix("- ").trimStart(),
                        style = MaterialTheme.typography.bodyLarge,
                        isBullet = true,
                        indentLevel = indentLevel
                    )
                }
                else -> MarkdownText(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium
                )
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
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }

    Column {
        if (isBullet) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(
                    start = 12.dp + (indentLevel * 12).dp,
                    bottom = 4.dp
                )
            ) {
                Text("• ", style = style)
                Text(
                    text = annotated,
                    style = style,
                    color = color,
                    fontWeight = fontWeight
                )
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
            Spacer(modifier = Modifier.height(bottomSpacing))
        }
    }
}

@Composable
fun ImportProgressDialog(onDismiss: () -> Unit) {
    AlertDialog(
        iconContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = { },
        title = { Text(stringResource(R.string.local_update_import)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDialog(
    onDismiss: () -> Unit,
    onSave: (PreferencesData) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
        iconContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = {
            onSave(PreferencesData(autoCheckInterval, autoDelete, meteredNetworkWarning, abPerfMode, updateRecovery))
            onDismiss()
        },
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
                        value = intervals.getOrNull(autoCheckInterval) ?: stringResource(R.string.menu_auto_updates_check_interval_never),
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
            TextButton(
                onClick = {
                    onSave(PreferencesData(autoCheckInterval, autoDelete, meteredNetworkWarning, abPerfMode, updateRecovery))
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.pref_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pref_cancel))
            }
        }
    )
}

@Composable
fun PreferenceSwitch(
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
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
        iconContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.welcome_title)) },
        text = { Text(stringResource(R.string.welcome_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
        iconContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = { onCancel() },
        title = { Text(stringResource(R.string.local_update_import)) },
        text = {
            Text(stringResource(R.string.local_update_import_success, update.version))
        },
        confirmButton = {
            TextButton(onClick = onInstall) {
                Text(stringResource(R.string.local_update_import_install))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
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
        iconContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.local_update_import_warning_title)) },
        text = { Text(stringResource(R.string.local_update_import_warning_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.info_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PillToolbar(
    selectedScreen: String,
    callbacks: UpdaterCallbacks
) {
    var showImportDialog by remember { mutableStateOf(false) }

    val items = listOf(
        ToolbarItem("Home", Icons.Filled.Home) { callbacks.onScreenChange("Home") },
        ToolbarItem("Update", Icons.Filled.Update) { callbacks.onScreenChange("Update") },
        ToolbarItem("Import", Icons.Filled.FileUpload) {
            showImportDialog = true
        }
    )

    if (showImportDialog) {
        ImportWarningDialog(
            onDismiss = { 
                callbacks.onImportLocal()
                callbacks.onScreenChange("Update")
                showImportDialog = false
            },
            onCancel = {
                callbacks.onScreenChange("Home")
                showImportDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        HorizontalFloatingToolbar(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            colors = FloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.surface,
                toolbarContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                fabContainerColor = MaterialTheme.colorScheme.tertiary,
                fabContentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            expanded = true,
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val containerColor by animateColorAsState(
                            targetValue = if (selectedScreen == item.name) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (selectedScreen == item.name) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primary
                        )

                        val scale = remember { Animatable(1f) }
                        LaunchedEffect(selectedScreen) {
                            if (selectedScreen == item.name) {
                                scale.snapTo(0.8f)
                                scale.animateTo(
                                    targetValue = 1.2f,
                                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                                )
                                scale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(100, easing = LinearOutSlowInEasing)
                                )
                            }
                        }

                        Surface(
                            onClick = { item.onClick() },
                            shape = CircleShape,
                            color = containerColor,
                            contentColor = contentColor,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale.value
                                    scaleY = scale.value
                                }
                                .width(48.dp)
                                .height(56.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.name,
                                    modifier = Modifier.size(40.dp).padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateCard(
    status: UpdateStatus,
    update: UpdateInfo,
    downloadProgress: Float,
    installProgress: Int,
    callbacks: UpdaterCallbacks
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val strokeWidthPx = with(LocalDensity.current) { 3.dp.toPx() }
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    val isLocalUpdate = update.getType() == null

    val indicatorProgress: () -> Float = when (status) {
        UpdateStatus.INSTALLING -> { { installProgress / 100f } }
        else -> { { downloadProgress } }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    update.getName() ?: "Local update",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    UpdateDropdownMenu(
                        menuExpanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        update = update,
                        progress = downloadProgress,
                        callbacks = callbacks
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Version: ${update.getVersion() ?: "—"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(Icons.Default.Storage, "Size", update.getFileSize()?.let { formatFileSize(it) } ?: "—")

            InfoRow(
                Icons.Default.DateRange,
                "Date",
                update.getTimestamp()?.let { formatTimestamp(it) }
                    ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )

            InfoRow(Icons.Default.Info, "Type", update.getType() ?: "Local update")

            InfoRow(Icons.Default.CloudDownload, "Status", status.toString())

            if (!isLocalUpdate) {
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(Icons.Default.Timer, "ETA", update.getEta()?.takeIf { it > 0 }?.let { "${it}s" } ?: "—")
                InfoRow(Icons.Default.Speed, "Speed", update.getSpeed()?.takeIf { it > 0 }?.let { "${it / 1024} KB/s" } ?: "—")
                if (status == UpdateStatus.INSTALLING || downloadProgress > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val isInstalling = status == UpdateStatus.INSTALLING
                    val trackColor = if (isInstalling) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(0.7f)
                    }
                    LinearWavyProgressIndicator(
                        progress = indicatorProgress,
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = trackColor,
                        stroke = stroke,
                        amplitude = { 0.8f },
                        wavelength = 20.dp,
                        waveSpeed = 20.dp
                    )
                    Text(
                        text = "${(indicatorProgress() * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    status == UpdateStatus.INSTALLING -> {
                        LinearWavyProgressIndicator(
                            progress = { installProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.tertiary,
                            stroke = stroke
                        )
                        Text(
                            text = "${installProgress}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {
                        LinearWavyProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ActionButtons(
                status = status,
                update = update,
                callbacks = callbacks
            )
        }
    }
}

@Composable
fun UpdateDropdownMenu(
    menuExpanded: Boolean,
    onDismiss: () -> Unit,
    update: UpdateInfo,
    progress: Float,
    callbacks: UpdaterCallbacks
) {
    val currentProgress by rememberUpdatedState(progress)
    val currentUpdate by rememberUpdatedState(update)

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = onDismiss
    ) {
        if (currentUpdate.getPersistentStatus() == UpdateStatus.Persistent.VERIFIED
            || currentProgress >= 1f) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_export_update)) },
                onClick = {
                    onDismiss()
                    callbacks.onExportUpdate(currentUpdate)
                }
            )
        }

        if (currentProgress > 0f) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_delete_update)) },
                onClick = {
                    onDismiss()
                    callbacks.onDelete(currentUpdate)
                }
            )
        }
    }
}

@Composable
private fun ActionButtons(
    status: UpdateStatus,
    update: UpdateInfo,
    callbacks: UpdaterCallbacks
) {
    when (status) {
        UpdateStatus.UNKNOWN,
        UpdateStatus.DELETED -> {
            Button(
                onClick = { callbacks.onStartDownload(update) },
                modifier = Modifier.fillMaxWidth(),
                enabled = update.getAvailableOnline()
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.action_download))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_download), fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.DOWNLOADING,
        UpdateStatus.STARTING -> {
            Button(
                onClick = { callbacks.onPause(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.action_pause))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_pause), fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.PAUSED -> {
            Button(
                onClick = { callbacks.onResume(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_resume))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_resume), fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.INSTALLED -> {
            Button(
                onClick = { callbacks.onInstalled(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = stringResource(R.string.reboot))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reboot), fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.VERIFIED -> {
            Button(
                onClick = { callbacks.onVerified(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.action_install))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_install), fontWeight = FontWeight.Bold)
            }
        }
        else -> {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_na))
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label: ", fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024
    val mb = kb / 1024
    return if (mb > 0) "$mb MB" else "$kb KB"
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}

@Composable
fun EmptyScreenIllustration(callbacks: UpdaterCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.list_no_updates),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = callbacks.onRefresh) {
            Text(stringResource(R.string.menu_refresh))
        }
    }
}
