package com.example.inf8405tf

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.inf8405tf.activity.LoginActivity
import com.example.inf8405tf.activity.ModifyProfileActivity
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.databinding.ActivityMainBinding
import com.example.inf8405tf.domain.AppSettings
import com.example.inf8405tf.receiver.BatteryReceiver
import com.example.inf8405tf.utils.UserSession
import com.example.inf8405tf.ui.viewmodel.BatteryInfo
import com.example.inf8405tf.ui.viewmodel.BatteryViewModel
import com.example.inf8405tf.utils.NetworkUsage
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var batteryReceiver: BatteryReceiver
    private val batteryViewModel: BatteryViewModel by viewModels()

    @Inject
    lateinit var database: AppDatabase

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Initialisation du menu de navigation avec deux onglets principaux
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_accueil,
                R.id.nav_debuter_parcours
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val appSettingsDao = database.appSettingsDao()
        val batteryLogDao = database.batteryLogDao()

        lifecycleScope.launch {
            if (appSettingsDao.getAppSettings()?.isInitialized == null) {
                println("📢 Initialisation de l'application!")
                setAppInitialized()
            } else {
                println("📢 Application déjà initialisée")
            }
            // Clear battery logs
            withContext(Dispatchers.IO) {
                batteryLogDao.clearLogs()
            }
        }

        updateNavHeader()

        // On initialise le batteryReceiver et on le register pour qu'il commence à listen aux changements de pourcentage de la batterie
        batteryReceiver = BatteryReceiver(database, batteryViewModel)

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        // Observe les changements de pourcentage de la batterie qui est updaté par le receiver et met à jour le UI
        batteryViewModel.batteryInfo.observe(this) { batteryInfo ->
            batteryInfo?.let {
                updateBatteryUI(it)
            }
        }

        // Met à jour le UI pour la première fois avec le pourcentage actuel de la batterie
        batteryViewModel.updateBatteryInfo()

        // Met à jour le uplink et downlink dans le menu de navigation à chaque seconde
        startNetworkUsageUpdates()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_accueil -> {
                    navController.navigate(R.id.nav_accueil)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_debuter_parcours -> {
                    navController.navigate(R.id.nav_debuter_parcours)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private suspend fun setAppInitialized() {
        withContext(Dispatchers.IO) {
            database.appSettingsDao().insertOrUpdateAppState(
                AppSettings(
                    isInitialized = true,
                    initializationTimestamp = Date()
                )
            )
        }
    }

    // On met à jour le menu de navigation avec les informations de l'utilisateur connecté à l'aide de la base de données
    private fun updateNavHeader() {
        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)

        val imageViewProfile = headerView.findViewById<ImageView>(R.id.imageViewProfile)
        val textViewUsername = headerView.findViewById<TextView>(R.id.textUsername)
        val textViewEmail = headerView.findViewById<TextView>(R.id.textEmail)
        val btnEditProfile = headerView.findViewById<MaterialButton>(R.id.btnEditProfile)

        lifecycleScope.launch {
            val userDao = database.userDao()
            val user = withContext(Dispatchers.IO) { userDao.getAuthenticatedUser() }

            user?.let {
                runOnUiThread {
                    textViewUsername.text = it.username
                    textViewEmail.text = it.email

                    // On pointe vers le chemin photo de profil pour l'afficher dans le menu de navigation
                    if (!it.profilePicturePath.isNullOrEmpty()) {
                        val uri = Uri.parse(it.profilePicturePath)
                        imageViewProfile.setImageURI(uri)
                        // Si l'utilisateur n'a pas fourni de photo de profil, on affiche une photo placeholder par défaut
                    } else {
                        imageViewProfile.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
            }
        }

        // Listener pour le bouton modifier le profil de l'utilisateur (commence l'activité ModifyProfileActivity)
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, ModifyProfileActivity::class.java))
        }
    }

    // On affiche les informations de la batterie dans le menu de navigation
    private fun updateBatteryUI(batteryInfo: BatteryInfo) {
        val footerView = binding.navFooter

        val textViewBatteryPercentage = footerView.findViewById<TextView>(R.id.nav_battery_percentage)
        val textViewBatteryConsumption = footerView.findViewById<TextView>(R.id.nav_battery_consumption)

        textViewBatteryPercentage.text = "Batterie restante: ${batteryInfo.batteryPercentage}%"
        textViewBatteryConsumption.text = "Batterie consommée: ${batteryInfo.batteryConsumption}%"
    }

    // On affiche le uplink et downlink dans le menu de navigation à chaque seconde
    // roule de façon asynchrone dans une coroutine pour ne pas bloquer l'app dans une boucle qui tourne à l'infini (while true)
    private fun startNetworkUsageUpdates() {
        lifecycleScope.launch {
            while (true) { // roule de façon continue
                val (uplink, downlink) = NetworkUsage.getFormattedUplinkDownlink()

                val footerView = binding.navFooter
                val textViewUplink = footerView.findViewById<TextView>(R.id.nav_uplink)
                val textViewDownlink = footerView.findViewById<TextView>(R.id.nav_downlink)

                textViewUplink.text = "Données envoyées: $uplink"
                textViewDownlink.text = "Données reçues: $downlink"

                kotlinx.coroutines.delay(1000) // délai d'une seconde après chaque itération
            }
        }
    }

    // Déconnecte l'utilisateur et efface ses données de session. Renvoie l'utilisateur à la page de connexion
    private fun logoutUser() {
        lifecycleScope.launch {
            val userDao = database.userDao()

            withContext(Dispatchers.IO) {
                userDao.logoutUser()
            }

            UserSession.clearSession()

            runOnUiThread {
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNavHeader()
        batteryViewModel.updateBatteryInfo()
    }

    override fun onPause() {
        super.onPause()
    }

    // important de unregister le receiver pour ne pas avoir de fuite de mémoire après un destroy de l'activité
    override fun onDestroy() {
        super.onDestroy()
        try {
            this.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.d("batteryReceiver", "Receiver pas enregistré après la destruction de l'activité")
        }
    }
}