package com.example.inf8405tf.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inf8405tf.data.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _batteryInfo = MutableLiveData<BatteryInfo>() // Permet de modifier la donnée (attribut privé)
    val batteryInfo: LiveData<BatteryInfo> get() = _batteryInfo // Permet au UI de subscribe et d'être automatiquement notifiée lorsque cette donnée change
    //https://developer.android.com/reference/androidx/lifecycle/LiveData

    // lorsque cette méthode est appelée, elle met à jour la donnée batteryInfo qui sera affichée dans le UI
    fun updateBatteryInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val firstLog = database.batteryLogDao().getFirstLog()
            val lastLog = database.batteryLogDao().getLastLog()

            if (lastLog != null) {
                val batteryPercentage = lastLog.level
                val batteryConsumption = when {
                    firstLog == null -> 0 // Quand c'est le premier log, la consommation est encore à 0%
                    else -> firstLog.level - batteryPercentage  // Sinon on calcule la différence de pourcentage entre le premier log et le dernier
                                                                // pour avoir la consommation totale depuis le lancement de l'app
                }

                // Met à jour la donnée batteryInfo qui sera affiché dans le UI
                withContext(Dispatchers.Main) {
                    _batteryInfo.value = BatteryInfo(batteryPercentage, batteryConsumption)
                }
            }
        }
    }
}

data class BatteryInfo(val batteryPercentage: Int, val batteryConsumption: Int)
