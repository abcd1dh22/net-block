package com.zihad.netblock.ui

import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zihad.netblock.AppItem
import com.zihad.netblock.BlockStats
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shows how often blocked apps tried to reach the internet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySheet(apps: List<AppItem>, onDismiss: () -> Unit) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val stats by BlockStats.stats.collectAsState()
    val log by BlockStats.log.collectAsState()
    val since by BlockStats.since.collectAsState()

    val labels = remember(apps) { apps.associate { it.pkg to it.label } }
    fun nameOf(pkg: String) = if (pkg == BlockStats.UNKNOWN) "Unknown app" else labels[pkg] ?: pkg

    val total = stats.values.sumOf { it.count }
    val byApp = stats.values.sortedByDescending { it.count }.take(30)
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val now = System.currentTimeMillis()

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
                    "Activity",
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

            // Summary
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("$total", style = MaterialTheme.typography.headlineMedium, color = c.label)
                Text(
                    if (total == 1) "blocked connection attempt" else "blocked connection attempts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.secondary
                )
                if (since > 0L) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Since " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(since)),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.secondary
                    )
                }
            }

            if (total == 0) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "No attempts yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "When a blocked app tries to connect, it shows up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.height(20.dp))
                SectionLabel("BY APP")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.card)
                ) {
                    byApp.forEachIndexed { i, stat ->
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(
                                pkg = stat.pkg,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    nameOf(stat.pkg),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = c.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Last " + DateUtils.getRelativeTimeSpanString(
                                        stat.lastMs, now, DateUtils.MINUTE_IN_MILLIS
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.secondary
                                )
                            }
                            Text("${stat.count}", style = MaterialTheme.typography.titleMedium, color = c.label)
                        }
                        if (i < byApp.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = c.separator,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("RECENT")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.card)
                ) {
                    log.forEachIndexed { i, entry ->
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                timeFormat.format(Date(entry.timeMs)),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = c.secondary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    nameOf(entry.pkg),
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
                        }
                        if (i < log.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = c.separator,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.card)
                        .clickable { BlockStats.clear(context) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Clear Activity", style = MaterialTheme.typography.bodyLarge, color = c.red)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                if (Build.VERSION.SDK_INT >= 29)
                    "Only the app, protocol and address of each attempt are kept, on this phone."
                else
                    "On Android 8 and 9, attempts can't be matched to one app when several apps are blocked. They show as Unknown app.",
                style = MaterialTheme.typography.bodySmall,
                color = c.secondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
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
