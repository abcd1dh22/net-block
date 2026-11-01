package com.zihad.netblock.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zihad.netblock.AppItem
import com.zihad.netblock.AppsViewModel
import com.zihad.netblock.BlockMode
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
    val apps by vm.apps.collectAsState()
    val rules by vm.rules.collectAsState()
    val loading by vm.loading.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.All) }
    var showSettings by remember { mutableStateOf(false) }

    val visible = remember(apps, rules, query, filter) {
        apps.filter { app ->
            (filter == Filter.All || app.pkg in rules) &&
                (query.isBlank() || app.label.contains(query, ignoreCase = true))
        }
    }

    if (showSettings) {
        SettingsDialog(vm = vm, onDismiss = { showSettings = false })
    }

    Scaffold(containerColor = Ink) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_background),
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Net Block",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextMain,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextMuted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                StatusCard(running = running, rules = rules, onToggle = onToggleProtection)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Panel,
                        unfocusedContainerColor = Panel,
                        focusedBorderColor = Signal,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        cursorColor = Signal
                    )
                )
                Spacer(Modifier.height(8.dp))
                Segmented(
                    options = listOf("All apps (${apps.size})", "Blocked (${rules.size})"),
                    selected = filter.ordinal,
                    onSelect = { filter = Filter.entries[it] }
                )
                Spacer(Modifier.height(4.dp))
            }

            when {
                loading -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Signal)
                    }
                }
                visible.isEmpty() -> item {
                    val noBlocked = filter == Filter.Blocked && query.isBlank()
                    EmptyState(
                        title = if (noBlocked) "No blocked apps yet" else "No apps found",
                        body = if (noBlocked) "Switch to All apps and turn on the apps you want to cut off."
                        else "Try a different name."
                    )
                }
                else -> items(visible, key = { it.pkg }, contentType = { "app" }) { app ->
                    AppRow(
                        app = app,
                        mode = rules[app.pkg],
                        onChange = { onSetRule(app.pkg, it) }
                    )
                }
            }
        }
    }
}

private val BrandStart = Color(0xFF0057FF)
private val BrandEnd = Color(0xFF00B8A3)

@Composable
private fun StatusCard(running: Boolean, rules: Map<String, BlockMode>, onToggle: () -> Unit) {
    val both = rules.count { it.value == BlockMode.ALL }
    val wifi = rules.count { it.value == BlockMode.WIFI }
    val mobile = rules.count { it.value == BlockMode.MOBILE }

    val shape = RoundedCornerShape(28.dp)
    val background = if (running) {
        Modifier.background(Brush.linearGradient(listOf(BrandStart, BrandEnd)), shape)
    } else {
        Modifier.background(Panel, shape)
    }
    val main = if (running) Color.White else TextMain
    val soft = if (running) Color.White.copy(alpha = 0.78f) else TextMuted

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = soft, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (running) "PROTECTION ON" else "PROTECTION OFF",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = soft
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        running -> "Selected apps are blocked"
                        rules.isEmpty() -> "Pick apps to block"
                        else -> "Ready to block ${rules.size} ${if (rules.size == 1) "app" else "apps"}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = main
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (running) "Tap the button to turn off" else "Tap the button to turn on",
                    style = MaterialTheme.typography.bodyMedium,
                    color = soft
                )
            }
            Spacer(Modifier.width(16.dp))
            PowerButton(running = running, onClick = onToggle)
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (running) Color.White.copy(alpha = 0.16f) else PanelHigh.copy(alpha = 0.6f))
                .padding(vertical = 12.dp)
        ) {
            Stat("Both", both, main, soft, Modifier.weight(1f))
            Stat("Wi-Fi only", wifi, main, soft, Modifier.weight(1f))
            Stat("Mobile only", mobile, main, soft, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, main: Color, soft: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleLarge, color = main)
        Text(label, style = MaterialTheme.typography.bodySmall, color = soft)
    }
}

@Composable
private fun PowerButton(running: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        if (running) {
            val pulse = rememberInfiniteTransition(label = "pulse")
            val scale by pulse.animateFloat(
                1f, 1.5f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "scale"
            )
            val alpha by pulse.animateFloat(
                0.35f, 0f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "alpha"
            )
            Box(
                Modifier
                    .size(64.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (running) Color.White else Signal)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = if (running) "Turn blocking off" else "Turn blocking on",
                tint = if (running) BrandStart else Ink,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) Signal else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Ink else TextMuted
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: AppItem, mode: BlockMode?, onChange: (BlockMode?) -> Unit) {
    val isBlocked = mode != null
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Panel,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(if (isBlocked) null else BlockMode.ALL) }
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(
                    pkg = app.pkg,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (mode != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Block, null, tint = Coral, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                when (mode) {
                                    BlockMode.ALL -> "Blocked on Wi-Fi and mobile data"
                                    BlockMode.WIFI -> "Blocked on Wi-Fi only"
                                    BlockMode.MOBILE -> "Blocked on mobile data only"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Coral
                            )
                        }
                    } else {
                        Text(
                            app.pkg,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = isBlocked,
                    onCheckedChange = { onChange(if (it) BlockMode.ALL else null) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Ink,
                        checkedTrackColor = Coral,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = PanelHigh,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
            if (mode != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("Both", mode == BlockMode.ALL) { onChange(BlockMode.ALL) }
                    ModeChip("Wi-Fi", mode == BlockMode.WIFI) { onChange(BlockMode.WIFI) }
                    ModeChip("Mobile data", mode == BlockMode.MOBILE) { onChange(BlockMode.MOBILE) }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PanelHigh,
            labelColor = TextMuted,
            selectedContainerColor = Coral,
            selectedLabelColor = Ink
        )
    )
}

@Composable
private fun SettingsDialog(vm: AppsViewModel, onDismiss: () -> Unit) {
    val autoStart by vm.autoStart.collectAsState()
    val update by vm.update.collectAsState()
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("Settings", color = TextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Start after reboot", style = MaterialTheme.typography.titleMedium, color = TextMain)
                        Text(
                            "Turn blocking back on when the phone restarts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = vm::setAutoStart,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Ink,
                            checkedTrackColor = Signal,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = PanelHigh,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
                HorizontalDivider(color = PanelHigh)
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                when (val u = update) {
                    UpdateState.Idle -> {}
                    UpdateState.Checking -> Text("Checking…", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    UpdateState.UpToDate -> Text("You have the latest version.", color = Signal, style = MaterialTheme.typography.bodyMedium)
                    is UpdateState.Available -> Text("Version ${u.version} is available.", color = Signal, style = MaterialTheme.typography.bodyMedium)
                    is UpdateState.Failed -> Text(u.message, color = Coral, style = MaterialTheme.typography.bodyMedium)
                }
                val available = update as? UpdateState.Available
                Button(
                    onClick = { if (available != null) uriHandler.openUri(available.url) else vm.checkForUpdates() },
                    enabled = update != UpdateState.Checking,
                    colors = ButtonDefaults.buttonColors(containerColor = Signal, contentColor = Ink)
                ) {
                    Text(if (available != null) "Download update" else "Check for updates")
                }
                Text(
                    "Checking contacts GitHub once. Nothing else in the app uses the internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Signal) }
        }
    )
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextMain)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}
