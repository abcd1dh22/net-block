package com.zihad.netblock

import android.Manifest
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zihad.netblock.ui.HomeScreen
import com.zihad.netblock.ui.NetBlockTheme

class MainActivity : ComponentActivity() {

    private val vm: AppsViewModel by viewModels()

    private val vpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) BlockVpnService.start(this)
        }

    // Asked once before turning protection on, so the status notification can show.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startProtection() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NetBlockTheme {
                val running by BlockVpnService.isRunning.collectAsState()
                HomeScreen(
                    vm = vm,
                    running = running,
                    onToggleProtection = { toggleProtection(running) },
                    onSetRule = { pkg, mode -> setRule(pkg, mode, running) }
                )
            }
        }
    }

    private fun toggleProtection(running: Boolean) {
        if (running) {
            BlockVpnService.stop(this)
            return
        }
        if (vm.rules.value.isEmpty()) {
            Toast.makeText(this, "Select at least one app to block first", Toast.LENGTH_SHORT).show()
            return
        }
        val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startProtection()
        }
    }

    private fun startProtection() {
        val intent = VpnService.prepare(this)
        if (intent == null) BlockVpnService.start(this) else vpnPermission.launch(intent)
    }

    private fun setRule(pkg: String, mode: BlockMode?, running: Boolean) {
        vm.setRule(pkg, mode)
        if (running) {
            // Apply the change right away.
            if (vm.rules.value.isEmpty()) BlockVpnService.stop(this) else BlockVpnService.start(this)
        }
    }
}
