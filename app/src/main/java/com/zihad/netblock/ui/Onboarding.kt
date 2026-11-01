package com.zihad.netblock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zihad.netblock.R
import kotlinx.coroutines.launch

private data class GuidePage(
    val title: String,
    val body: String,
    val icon: ImageVector?,
    val tint: Color
)

/** First-run guide. Also reachable from Settings → How It Works. */
@Composable
fun Onboarding(onFinish: () -> Unit) {
    val c = LocalAppColors.current
    val pages = listOf(
        GuidePage(
            "Block the Internet for Any App",
            "Pick the apps you want to cut off. Everything else keeps working normally.",
            null, c.blue
        ),
        GuidePage(
            "Choose Where to Block",
            "For each app, block Wi-Fi, mobile data, or both. See every blocked attempt in Activity.",
            Icons.Filled.Wifi, c.green
        ),
        GuidePage(
            "Why a VPN Permission?",
            "Android only lets firewall apps work through a local VPN. Nothing leaves your phone, and no root is needed. You'll see a one-time prompt when you first turn protection on.",
            Icons.Filled.Lock, c.red
        )
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            if (!isLast) {
                Text(
                    "Skip",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.blue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onFinish)
                        .padding(8.dp)
                )
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (page.icon == null) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_background),
                        contentDescription = null,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(26.dp))
                    )
                } else {
                    Box(
                        Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(page.tint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(page.icon, null, tint = Color.White, modifier = Modifier.size(56.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    page.title,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.label,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (i == pager.currentPage) c.blue else c.fill)
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.blue)
                .clickable {
                    if (isLast) onFinish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isLast) "Get Started" else "Continue",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
