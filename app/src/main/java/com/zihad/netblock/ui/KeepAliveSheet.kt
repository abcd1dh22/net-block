package com.zihad.netblock.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** True when Android may put Net Block to sleep to save battery. */
internal fun isBatteryRestricted(context: Context): Boolean {
    val pm = context.getSystemService(PowerManager::class.java)
    return !pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun notificationsAllowed(context: Context) =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun open(context: Context, vararg attempts: Intent) {
    for (intent in attempts) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
            // Try the next option.
        }
    }
}

private data class Brand(val name: String, val keys: List<String>, val steps: List<String>)

private val brands = listOf(
    Brand(
        "Xiaomi · Redmi · POCO", listOf("xiaomi", "redmi", "poco"),
        listOf(
            "Open Settings → Apps → Manage apps → Net Block and turn on Autostart.",
            "On the same page, open Battery saver and choose No restrictions.",
            "Open Recent apps, long-press the Net Block card and tap the lock icon."
        )
    ),
    Brand(
        "Samsung", listOf("samsung"),
        listOf(
            "Open Settings → Apps → Net Block → Battery and choose Unrestricted.",
            "Open Settings → Battery → Background usage limits and remove Net Block from sleeping apps."
        )
    ),
    Brand(
        "Oppo · Realme · OnePlus", listOf("oppo", "realme", "oneplus"),
        listOf(
            "Open Settings → Apps → App management → Net Block → Battery usage and allow background activity.",
            "Turn on Auto-launch for Net Block."
        )
    ),
    Brand(
        "Vivo · iQOO", listOf("vivo", "iqoo"),
        listOf(
            "Open Settings → Battery → Background power consumption and allow Net Block.",
            "Turn on Autostart for Net Block in the app permissions."
        )
    ),
    Brand(
        "Huawei · Honor", listOf("huawei", "honor"),
        listOf(
            "Open Settings → Battery → App launch → Net Block and choose Manage manually.",
            "Turn on Auto-launch, Secondary launch and Run in background."
        )
    ),
    Brand(
        "Tecno · Infinix · itel", listOf("tecno", "infinix", "itel"),
        listOf(
            "Open Settings → Apps → Net Block → Battery and choose No restrictions.",
            "In Phone Master or the Autostart manager, turn on Net Block."
        )
    ),
    Brand(
        "Pixel · Motorola · Nokia · others", listOf("google", "motorola", "nokia", "nothing", "sony", "asus", "lenovo"),
        listOf("Open Settings → Apps → Net Block → App battery usage and choose Unrestricted.")
    )
)

/** Warning shown on the home tab when battery limits could stop blocking. */
@Composable
internal fun BatteryBanner(onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.red.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = c.red, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Battery limits may stop blocking", style = MaterialTheme.typography.titleMedium, color = c.label)
            Text("Tap to fix in a few steps", style = MaterialTheme.typography.bodySmall, color = c.secondary)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = c.secondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepAliveSheet(onDismiss: () -> Unit) {
    val c = LocalAppColors.current
    val context = LocalContext.current

    var restricted by remember { mutableStateOf(isBatteryRestricted(context)) }
    var notifications by remember { mutableStateOf(notificationsAllowed(context)) }
    var showOthers by remember { mutableStateOf(false) }

    // Re-check when the user comes back from the system settings.
    LifecycleResumeEffect(Unit) {
        restricted = isBatteryRestricted(context)
        notifications = notificationsAllowed(context)
        onPauseOrDispose { }
    }

    val maker = Build.MANUFACTURER.lowercase()
    val mine = brands.firstOrNull { b -> b.keys.any { maker.contains(it) } } ?: brands.last()
    val others = brands.filter { it != mine }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bg
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "Keep Running",
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
            Spacer(Modifier.height(8.dp))
            Text(
                "Some phones close background apps to save battery, which stops blocking. These settings prevent that.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.secondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(16.dp))

            // Status rows
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.card)
            ) {
                StatusRow(
                    ok = !restricted,
                    title = if (restricted) "Battery: Restricted" else "Battery: Unrestricted",
                    subtitle = if (restricted) "Android may put Net Block to sleep." else "Net Block can keep running.",
                    action = if (restricted) "Allow" else null,
                    onAction = {
                        open(
                            context,
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${context.packageName}")),
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = c.separator, thickness = 0.5.dp)
                StatusRow(
                    ok = notifications,
                    title = if (notifications) "Notification: On" else "Notification: Off",
                    subtitle = if (notifications) "A status notification shows while blocking." else "Turn it on so Android keeps blocking running.",
                    action = if (!notifications) "Open" else null,
                    onAction = {
                        open(
                            context,
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionText("ON YOUR PHONE · ${mine.name.uppercase()}")
            StepsCard(mine.steps)

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.card)
                    .clickable {
                        open(
                            context,
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Open App Info", style = MaterialTheme.typography.bodyLarge, color = c.blue)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                if (showOthers) "Hide other phones" else "Show steps for other phones",
                style = MaterialTheme.typography.bodyMedium,
                color = c.blue,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showOthers = !showOthers }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            )
            if (showOthers) {
                others.forEach { brand ->
                    Spacer(Modifier.height(12.dp))
                    SectionText(brand.name.uppercase())
                    StepsCard(brand.steps)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Menu names differ a little between phone models and Android versions.",
                style = MaterialTheme.typography.bodySmall,
                color = c.secondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun StatusRow(ok: Boolean, title: String, subtitle: String, action: String?, onAction: () -> Unit) {
    val c = LocalAppColors.current
    val tint = if (ok) c.green else c.red
    val icon: ImageVector = if (ok) Icons.Filled.Check else Icons.Filled.Warning
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.label)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.secondary)
        }
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                action,
                style = MaterialTheme.typography.titleMedium,
                color = c.blue,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun StepsCard(steps: List<String>) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
    ) {
        steps.forEachIndexed { i, step ->
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = c.blue)
                Spacer(Modifier.width(12.dp))
                Text(step, style = MaterialTheme.typography.bodyMedium, color = c.label)
            }
            if (i < steps.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(start = 40.dp), color = c.separator, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun SectionText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = LocalAppColors.current.secondary,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
    )
}
