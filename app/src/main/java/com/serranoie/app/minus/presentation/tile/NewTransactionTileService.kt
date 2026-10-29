package com.serranoie.app.minus.presentation.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.quickadd.QuickExpenseActivity

class NewTransactionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.state = Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.app_name)
            }
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val launchSheet = Runnable { openQuickExpense() }
        if (isLocked) {
            unlockAndRun(launchSheet)
        } else {
            launchSheet.run()
        }
    }

    private fun openQuickExpense() {
        val intent = Intent(this, QuickExpenseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
