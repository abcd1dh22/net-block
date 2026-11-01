package com.zihad.netblock.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zihad.netblock.AppItem
import com.zihad.netblock.AppsViewModel
import com.zihad.netblock.BlockMode
import com.zihad.netblock.BlockStats
import kotlinx.coroutines.delay
import java.util.Locale

/** Home tab: protection status, per-network status and live activity. */
@Composable
fun DashboardScreen(
    vm: AppsViewModel,
    apps: List<AppItem>,
    rules: Map<String, BlockMode>,
    running: Boolean,
    onToggleProtection: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    val net by vm.network.collectAsState()
    val speed by vm.speed.collectAsState()
    val stats by BlockStats.stats.collectAsState()
    val log by BlockStats.log.collectAsState()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }

    val labels = remember(apps) { apps.associate { it.pkg to it.label } }
    val totalAttempts = stats.values.sumOf { it.count }
    val wifiBlocked = rules.count { it.value == BlockMode.ALL || it.value == BlockMode.WIFI }
    val mobileBlocked = rules.count { it.value == BlockMode.ALL || it.value == BlockMode.MOBILE }
    val activeNet = when {
        net.wifi -> 0
        net.mobile -> 1
        else -> -1
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
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
                RoundIconButton(Icons.Filled.BarChart, "Activity", onOpenActivity)
                Spacer(Modifier.width(10.dp))
                RoundIconButton(Icons.Filled.Settings, "Settings", onOpenSettings)
            }
            Spacer(Modifier.height(16.dp))

            BigStatusCard(running = running, blocked = rules.size, attempts = totalAttempts)
            Spacer(Modifier.height(12.dp))
            ProtectionButton(running = running, onClick = onToggleProtection)

            Spacer(Modifier.height(24.dp))
            SectionLabel("NETWORK")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkCard(
                    icon = Icons.Filled.Wifi,
                    title = "Wi-Fi",
                    connected = net.wifi,
                    blocked = wifiBlocked,
                    active = activeNet == 0,
                    modifier = Modifier.weight(1f)
                )
                NetworkCard(
                    icon = Icons.Filled.SignalCellularAlt,
                    title = "Mobile Data",
                    connected = net.mobile,
                    blocked = mobileBlocked,
                    active = activeNet == 1,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("LIVE ACTIVITY")
            LiveCard(
                downBps = speed.downBps,
                upBps = speed.upBps,
                recent = log.take(4),
                labels = labels,
                now = now,
                onSeeAll = onOpenActivity
            )

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.card)
                    .clickable(onClick = onOpenApps)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(c.blue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Apps, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Manage Apps", style = MaterialTheme.typography.bodyLarge, color = c.label)
                    Text(
                        "${apps.size} apps · ${rules.size} blocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.secondary
                    )
                }
                Text("›", style = MaterialTheme.typography.titleLarge, color = c.secondary)
            }
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(c.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = c.blue, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = LocalAppColors.current.secondary,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun BigStatusCard(running: Boolean, blocked: Int, attempts: Int) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.card)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            if (running) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val scale by pulse.animateFloat(
                    1f, 1.35f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "scale"
                )
                val alpha by pulse.animateFloat(
                    0.35f, 0f, infiniteRepeatable(tween(2000), RepeatMode.Restart), label = "alpha"
                )
                Box(
                    Modifier
                        .size(88.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(c.green.copy(alpha = alpha))
                )
            }
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(if (running) c.green.copy(alpha = 0.16f) else c.fill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = if (running) c.green else c.secondary,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (running) "Protection On" else "Protection Off",
            style = MaterialTheme.typography.titleLarge,
            color = c.label
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                running -> "Your blocked apps are offline."
                blocked == 0 -> "Choose apps to block, then turn protection on."
                else -> "Ready to block $blocked ${if (blocked == 1) "app" else "apps"}."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = c.separator, thickness = 0.5.dp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            BigStat("$blocked", "Apps blocked", Modifier.weight(1f))
            Box(
                Modifier
                    .width(0.5.dp)
                    .height(44.dp)
                    .background(c.separator)
                    .align(Alignment.CenterVertically)
            )
            BigStat("$attempts", "Attempts stopped", Modifier.weight(1f))
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier) {
    val c = LocalAppColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = c.label)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.secondary)
    }
}

@Composable
private fun NetworkCard(
    icon: ImageVector,
    title: String,
    connected: Boolean,
    blocked: Int,
    active: Boolean,
    modifier: Modifier
) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.card)
            .then(if (active) Modifier.border(1.5.dp, c.blue, shape) else Modifier)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (connected) c.blue else c.secondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.label, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (connected) c.green else c.secondary)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (connected) "Connected" else "Not connected",
                style = MaterialTheme.typography.bodySmall,
                color = c.secondary
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "$blocked",
            style = MaterialTheme.typography.titleLarge,
            color = if (blocked > 0) c.red else c.label
        )
        Text(
            if (blocked == 1) "app blocked" else "apps blocked",
            style = MaterialTheme.typography.bodySmall,
            color = c.secondary
        )
        if (active) {
            Spacer(Modifier.height(8.dp))
            Text("Active now", style = MaterialTheme.typography.bodySmall, color = c.blue)
        }
    }
}

@Composable
private fun LiveCard(
    downBps: Long,
    upBps: Long,
    recent: List<com.zihad.netblock.LogEntry>,
    labels: Map<String, String>,
    now: Long,
    onSeeAll: () -> Unit
) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .animateContentSize()
    ) {
        Row(Modifier.padding(16.dp)) {
            SpeedBlock("↓  Download", downBps, c.green, Modifier.weight(1f))
            SpeedBlock("↑  Upload", upBps, c.blue, Modifier.weight(1f))
        }
        HorizontalDivider(color = c.separator, thickness = 0.5.dp)
        Text(
            "JUST BLOCKED",
            style = MaterialTheme.typography.bodySmall,
            color = c.secondary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        if (recent.isEmpty()) {
            Text(
                "Nothing blocked yet. Attempts from blocked apps appear here instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            )
        } else {
            recent.forEach { entry ->
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        pkg = entry.pkg,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (entry.pkg == BlockStats.UNKNOWN) "Unknown app" else labels[entry.pkg] ?: entry.pkg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${entry.proto}  ${entry.dest}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = c.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(ago(now - entry.timeMs), style = MaterialTheme.typography.bodySmall, color = c.secondary)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        HorizontalDivider(color = c.separator, thickness = 0.5.dp)
        Text(
            "See All Activity",
            style = MaterialTheme.typography.bodyLarge,
            color = c.blue,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSeeAll)
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun SpeedBlock(label: String, bps: Long, color: Color, modifier: Modifier) {
    val c = LocalAppColors.current
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.secondary)
        Spacer(Modifier.height(2.dp))
        Text(formatSpeed(bps), style = MaterialTheme.typography.titleLarge, color = color)
    }
}

private fun formatSpeed(bps: Long): String = when {
    bps < 0 -> "—"
    bps < 1024 -> "$bps B/s"
    bps < 1024 * 1024 -> String.format(Locale.US, "%.0f KB/s", bps / 1024.0)
    else -> String.format(Locale.US, "%.1f MB/s", bps / 1048576.0)
}

private fun ago(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return when {
        s < 5 -> "now"
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m"
        else -> "${s / 3600}h"
    }
}
