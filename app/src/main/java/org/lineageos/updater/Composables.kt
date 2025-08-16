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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
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
    val maintainer = SystemProperties.get("persist.sys.axion_maintainer", "Unknown")
    val version = SystemProperties.get("ro.axion.build.version", "2.0")
    val deviceModel = SystemProperties.get("ro.product.model", "Unknown Device")
    val updated = uiState.latestUpdate == null

    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            UpdaterTopBar(
                title = "Software update",
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
    val progress = uiState.downloadProgress

    Scaffold(
        topBar = {
            UpdaterTopBar(
                title = "Updates",
                onBack = { callbacks.onScreenChange("Home") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (latestUpdate != null) {
                UpdateCard(
                    status = status,
                    update = latestUpdate,
                    progress = progress,
                    callbacks = callbacks
                )
            } else {
                EmptyScreenIllustration(callbacks = callbacks)
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
                fontWeight = FontWeight.Bold
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
                        text = { Text("Preferences") },
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
    val changelogLines = changelog.lines()

    val primaryColor = MaterialTheme.colorScheme.primary
    val versionParts = version.split(".")
    val firstNumber = versionParts.getOrNull(0) ?: "2"
    val rest = versionParts.drop(1).joinToString(".", prefix = ".")

    val infiniteTransition = rememberInfiniteTransition()
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 12.dp)
            ) {
                Text(
                    text = firstNumber,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 100.sp, fontWeight = FontWeight.Bold
                    ),
                    color = primaryColor
                )
                Text(
                    text = rest,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 100.sp, fontWeight = FontWeight.Bold
                    )
                )
            }

            WaveAnimation(
                wavePhase = wavePhase,
                color = primaryColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.TopStart)
                    .offset(y = 100.dp)
            )

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (updated) "Up-to-date" else "Update available",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 28.sp
                )
                Text(
                    text = "AxionOS",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = "$deviceModel by $maintainer",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 16.sp
                )
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
                contentDescription = "Swipe up to see changelog",
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = arrowOffset.dp)
                    .alpha(arrowAlpha),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Swipe up to see what's new",
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
            text = "What's New?",
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
                val intervals = listOf("Never", "Daily", "Weekly", "Monthly")
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = intervals.getOrNull(autoCheckInterval) ?: "Never",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Auto-check interval") },
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
                    title = "Auto-delete updates",
                    checked = autoDelete,
                    onCheckedChange = { autoDelete = it }
                )

                PreferenceSwitch(
                    title = "Warn about metered networks",
                    checked = meteredNetworkWarning,
                    onCheckedChange = { meteredNetworkWarning = it }
                )

                if (Utils.isABDevice()) {
                    PreferenceSwitch(
                        title = "A/B performance mode",
                        checked = abPerfMode,
                        onCheckedChange = { abPerfMode = it }
                    )
                }

                if (!context.resources.getBoolean(R.bool.config_hideRecoveryUpdate)) {
                    PreferenceSwitch(
                        title = "Update recovery",
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PillToolbar(
    selectedScreen: String,
    callbacks: UpdaterCallbacks
) {
    val items = listOf(
        ToolbarItem("Home", Icons.Filled.Home) { callbacks.onScreenChange("Home") },
        ToolbarItem("Update", Icons.Filled.Update) { callbacks.onScreenChange("Update") },
        ToolbarItem("Import", Icons.Filled.FileUpload) { callbacks.onImportLocal(); callbacks.onScreenChange("Update") }
    )

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
                Box {
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
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateCard(
    status: UpdateStatus,
    update: UpdateInfo,
    progress: Float,
    callbacks: UpdaterCallbacks
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val strokeWidthPx = with(LocalDensity.current) { 3.dp.toPx() }
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    update.getName(),
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
                        progress = progress,
                        callbacks = callbacks
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Version: ${update.getVersion()}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(Icons.Default.Storage, "Size", formatFileSize(update.getFileSize()))
            InfoRow(Icons.Default.DateRange, "Date", formatTimestamp(update.getTimestamp()))
            InfoRow(Icons.Default.Info, "Type", update.getType())

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(Icons.Default.CloudDownload, "Status", update.getStatus().toString())
            InfoRow(Icons.Default.Timer, "ETA", if (update.getEta() > 0) "${update.getEta()}s" else "—")
            InfoRow(Icons.Default.Speed, "Speed", if (update.getSpeed() > 0) "${update.getSpeed() / 1024} KB/s" else "—")

            if (status == UpdateStatus.INSTALLING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (progress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.primary.copy(0.7f),
                    stroke = stroke,
                    amplitude = { 0.8f },
                    wavelength = 20.dp,
                    waveSpeed = 20.dp
                )
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
                text = { Text("Export") },
                onClick = {
                    onDismiss()
                    callbacks.onExportUpdate(currentUpdate)
                }
            )
        }

        if (currentProgress > 0f) {
            DropdownMenuItem(
                text = { Text("Delete") },
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
    status: UpdateStatus?,
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
                Icon(Icons.Default.CloudDownload, contentDescription = "Download")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download", fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.DOWNLOADING,
        UpdateStatus.STARTING -> {
            Button(
                onClick = { callbacks.onPause(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pause", fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.PAUSED -> {
            Button(
                onClick = { callbacks.onResume(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resume", fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.INSTALLED -> {
            Button(
                onClick = { callbacks.onInstalled(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reboot")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reboot", fontWeight = FontWeight.Bold)
            }
        }
        UpdateStatus.VERIFIED -> {
            Button(
                onClick = { callbacks.onVerified(update) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Install")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Install", fontWeight = FontWeight.Bold)
            }
        }
        else -> {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("N/A")
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
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp * 1000))
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
            text = "No updates available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = callbacks.onRefresh) {
            Text("Refresh updates")
        }
    }
}
