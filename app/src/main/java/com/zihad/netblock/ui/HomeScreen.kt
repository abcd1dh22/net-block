package com.zihad.netblock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zihad.netblock.AppItem
import com.zihad.netblock.AppsViewModel
import com.zihad.netblock.BlockMode
import com.zihad.netblock.BlockStats
import com.zihad.netblock.BuildConfig
import com.zihad.netblock.R
import com.zihad.netblock.UpdateState

private enum class Filter { All, Blocked }

@Composable
fun HomeScreen(
    vm: AppsViewModel,
    running: Boolean,
    onToggleProtection: () -> Unit,
    onSetRule: (String, BlockMode?) -> Unit
) {
    val c = LocalAppColors.current
    val apps by vm.apps.collectAsState()
    val rules by vm.rules.collectAsState()
    val loading by vm.loading.collectAsState()
    val stats by BlockStats.stats.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.All) }
    var showSettings by remember { mutableStateOf(false) }
    var showActivity by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(!vm.onboarded) }

    val visible = remember(apps, rules, query, filter) {
        apps.filter { app ->
            (filter == Filter.All || app.pkg in rules) &&
                (query.isBlank() || app.label.contains(query, ignoreCase = true))
        }
    }

    if (showSettings) {
        SettingsSheet(
            vm = vm,
            onDismiss = { showSettings = false },
            onShowGuide = { showSettings = false; showGuide = true }
        )
    }
    if (showActivity) {
        ActivitySheet(apps = apps, onDismiss = { showActivity = false })
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(containerColor = c.bg) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Net Block",
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.label,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c.card)
                            .clickable { showActivity = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.BarChart,
                            contentDescription = "Activity",
                            tint = c.blue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c.card)
                            .clickable { showSettings = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = c.blue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                StatusCard(running = running, rules = rules)
                Spacer(Modifier.height(12.dp))
                ProtectionButton(running = running, onClick = onToggleProtection)
                Spacer(Modifier.height(24.dp))
                SearchField(query = query, onChange = { query = it })
                Spacer(Modifier.height(12.dp))
                Segmented(
                    options = listOf("All apps (${apps.size})", "Blocked (${rules.size})"),
                    selected = filter.ordinal,
                    onSelect = { filter = Filter.entries[it] }
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "APPS",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.secondary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                )
            }

            when {
                loading -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.blue)
                    }
                }
                visible.isEmpty() -> item {
                    val noBlocked = filter == Filter.Blocked && query.isBlank()
                    EmptyState(
                        title = if (noBlocked) "No Blocked Apps" else "No Results",
                        body = if (noBlocked) "Switch to All apps and turn on the apps you want to cut off."
                        else "Try a different name."
                    )
                }
                else -> itemsIndexed(
                    visible,
                    key = { _, app -> app.pkg },
                    contentType = { _, _ -> "app" }
                ) { index, app ->
                    AppRow(
                        app = app,
                        mode = rules[app.pkg],
                        attempts = stats[app.pkg]?.count ?: 0,
                        first = index == 0,
                        last = index == visible.lastIndex,
                        onChange = { onSetRule(app.pkg, it) }
                    )
                }
            }
        }
    }
    if (showGuide) {
        Onboarding(onFinish = {
            showGuide = false
            vm.markOnboarded()
        })
    }
    }
}

@Composable
private fun StatusCard(running: Boolean, rules: Map<String, BlockMode>) {
    val c = LocalAppColors.current
    val both = rules.count { it.value == BlockMode.ALL }
    val wifi = rules.count { it.value == BlockMode.WIFI }
    val mobile = rules.count { it.value == BlockMode.MOBILE }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Protection", style = MaterialTheme.typography.titleMedium, color = c.label)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (running) c.green else c.secondary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (running) "On" else "Off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (running) c.green else c.secondary
                    )
                }
            }
        }
        HorizontalDivider(color = c.separator, thickness = 0.5.dp)
        Row(Modifier.padding(vertical = 12.dp)) {
            Stat("Both", both, Modifier.weight(1f))
            Stat("Wi-Fi only", wifi, Modifier.weight(1f))
            Stat("Mobile only", mobile, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier) {
    val c = LocalAppColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleLarge, color = c.label)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.secondary)
    }
}

@Composable
private fun ProtectionButton(running: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val background by animateColorAsState(
        if (running) c.red.copy(alpha = 0.14f) else c.blue,
        label = "buttonBg"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (running) "Turn Off Protection" else "Turn On Protection",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (running) c.red else Color.White
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val c = LocalAppColors.current
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = c.label, fontSize = 17.sp),
        cursorBrush = SolidColor(c.blue),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.fill)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = c.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search", fontSize = 17.sp, color = c.secondary)
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "Clear",
                        tint = c.secondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable { onChange("") }
                    )
                }
            }
        }
    )
}

/** iOS-style segmented control. */
@Composable
private fun Segmented(
    options: List<String>,
    selected: Int,
    compact: Boolean = false,
    onSelect: (Int) -> Unit
) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 8.dp else 10.dp))
            .background(c.fill)
            .padding(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp))
                    .background(if (isSelected) c.selectedSegment else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = if (compact) 6.dp else 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = c.label,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppItem,
    mode: BlockMode?,
    attempts: Int,
    first: Boolean,
    last: Boolean,
    onChange: (BlockMode?) -> Unit
) {
    val c = LocalAppColors.current
    val top = if (first) 16.dp else 0.dp
    val bottom = if (last) 16.dp else 0.dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
    ) {
        Column(
            Modifier
                .clickable { onChange(if (mode != null) null else BlockMode.ALL) }
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = if (mode != null) 12.dp else 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    pkg = app.pkg,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (mode != null) {
                        Text(
                            when (mode) {
                                BlockMode.ALL -> "Blocked on Wi-Fi and mobile data"
                                BlockMode.WIFI -> "Blocked on Wi-Fi only"
                                BlockMode.MOBILE -> "Blocked on mobile data only"
                            } + (if (attempts > 0) " · $attempts ${if (attempts == 1) "attempt" else "attempts"}" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.red
                        )
                    } else {
                        Text(
                            app.pkg,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                IosSwitch(
                    checked = mode != null,
                    onCheckedChange = { onChange(if (it) BlockMode.ALL else null) },
                    onColor = c.red
                )
            }
            if (mode != null) {
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("Both", "Wi-Fi", "Mobile data"),
                    selected = mode.ordinal,
                    compact = true,
                    onSelect = { onChange(BlockMode.entries[it]) }
                )
            }
        }
        if (!last) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                color = c.separator,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onColor: Color) {
    val c = LocalAppColors.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = onColor,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = c.switchOff,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(vm: AppsViewModel, onDismiss: () -> Unit, onShowGuide: () -> Unit) {
    val c = LocalAppColors.current
    val autoStart by vm.autoStart.collectAsState()
    val update by vm.update.collectAsState()
    val uriHandler = LocalUriHandler.current
    val available = update as? UpdateState.Available

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bg
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.label,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    "Done",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.blue,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.card)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Start After Reboot", style = MaterialTheme.typography.bodyLarge, color = c.label)
                        Text(
                            "Turn protection back on when the phone restarts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.secondary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IosSwitch(checked = autoStart, onCheckedChange = vm::setAutoStart, onColor = c.green)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = c.separator, thickness = 0.5.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyLarge, color = c.label, modifier = Modifier.weight(1f))
                    Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyLarge, color = c.secondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.card)
                    .clickable(onClick = onShowGuide)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("How It Works", style = MaterialTheme.typography.bodyLarge, color = c.label, modifier = Modifier.weight(1f))
                Text("›", style = MaterialTheme.typography.bodyLarge, color = c.secondary)
            }

            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.card)
                    .clickable(enabled = update != UpdateState.Checking) {
                        if (available != null) uriHandler.openUri(available.url) else vm.checkForUpdates()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        update == UpdateState.Checking -> "Checking…"
                        available != null -> "Download Update"
                        else -> "Check for Updates"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (update == UpdateState.Checking) c.secondary else c.blue,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))
            val (footnote, footColor) = when (val u = update) {
                UpdateState.Idle -> "Checking contacts GitHub once. Nothing else in the app uses the internet." to c.secondary
                UpdateState.Checking -> "Contacting GitHub…" to c.secondary
                UpdateState.UpToDate -> "You have the latest version." to c.secondary
                is UpdateState.Available -> "Version ${u.version} is available." to c.green
                is UpdateState.Failed -> u.message to c.red
            }
            Text(
                footnote,
                style = MaterialTheme.typography.bodySmall,
                color = footColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.label)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = c.secondary,
            textAlign = TextAlign.Center
        )
    }
}
