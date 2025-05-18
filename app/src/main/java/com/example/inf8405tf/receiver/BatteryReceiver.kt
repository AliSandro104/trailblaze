package com.example.inf8405tf.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.domain.BatteryLog
import com.example.inf8405tf.ui.viewmodel.BatteryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class BatteryReceiver @Inject constructor(private val database: AppDatabase, private val viewModel: BatteryViewModel) : BroadcastReceiver() {

    // Lorsqu'il y a un changement de pourcentage de la batterie, on insère un nouveau log dans la base de données et on met à jour la donnée dans le viewModel pour qu'elle soit reflétée dans le UI
    override fun onReceive(context: Context, intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            database.batteryLogDao().insertLog(
                BatteryLog(
                    timestamp = Date(),
                    level = level
                )
            )
            withContext(Dispatchers.Main) {
                viewModel.updateBatteryInfo()
            }
        }
    }
}