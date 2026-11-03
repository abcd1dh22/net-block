package com.zihad.netblock

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Invisible helper opened by the Quick Settings tile to turn protection on.
 *
 * Android does not let a background app start a service, which is why starting straight
 * from the tile failed whenever the app had been closed. An activity that is on screen is
 * allowed to start it, so the tile opens this one, which starts the VPN and closes itself.
 */
class TileActionActivity : ComponentActivity() {

    private val vpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) BlockVpnService.start(this)
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Prefs(this).rules.isEmpty()) {
            Toast.makeText(this, "Select at least one app to block first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val consent = VpnService.prepare(this)
        if (consent == null) {
            BlockVpnService.start(this)
            finish()
        } else {
            vpnPermission.launch(consent)
        }
    }
}
