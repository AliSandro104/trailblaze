package com.example.inf8405tf.ui.fragment

import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inf8405tf.R
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.databinding.FragmentEnregistrerParcoursBinding
import com.example.inf8405tf.domain.TrackSessionData
import com.example.inf8405tf.domain.Track
import com.example.inf8405tf.service.WeatherService
import com.example.inf8405tf.utils.DialogUtils
import com.example.inf8405tf.utils.UserSession
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit
import android.Manifest
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.widget.TextView
import com.example.inf8405tf.domain.SensorData
import com.example.inf8405tf.domain.WeatherData
import com.example.inf8405tf.service.AccelerationService
import com.example.inf8405tf.service.LocationService
import com.example.inf8405tf.service.NetworkStateObserver
import com.example.inf8405tf.service.StepCounterService
import com.example.inf8405tf.ui.adapter.DebuterParcoursAdapter
import com.example.inf8405tf.ui.viewmodel.TrackingViewModel
import com.example.inf8405tf.utils.TrackAverageCalculatorUtils
import com.example.inf8405tf.utils.PulsatingEffectUtils
import com.example.inf8405tf.utils.TrackInfoUtils
import com.example.inf8405tf.utils.TrackStateUtils
import com.example.inf8405tf.utils.UuidUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class DebuterParcoursFragment : Fragment() {
    private var _binding: FragmentEnregistrerParcoursBinding? = null
    private val trackingViewModel: TrackingViewModel by activityViewModels()
    private var isUpdating = false
    private var isNetworkAvailable = true

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var dialogUtils: DialogUtils

    @Inject
    lateinit var trackInfoUtils: TrackInfoUtils

    @Inject
    lateinit var networkStateObserver: NetworkStateObserver

    @Inject
    lateinit var weatherService: WeatherService

    @Inject
    lateinit var stepCounterService: StepCounterService

    @Inject
    lateinit var accelerationService: AccelerationService

    @Inject
    lateinit var locationService: LocationService

    @Inject
    lateinit var uuidUtils: UuidUtils

    @Inject
    lateinit var trackStateUtils: TrackStateUtils

    private var startTimestamp: Date = Date()
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var running: Boolean = false
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private lateinit var adapter: DebuterParcoursAdapter
    private var temperature: Float? = null
    private var weatherCondition: String? = ""
    private var humidity: Float? = null
    private var windSpeed: Float? = null
    private var currentSpeed: Float? = 0.0f
    private var totalDistance: Float? = 0.0f
    private var lastLocation: Location? = null
    private var acceleration: Float? = 0.0f
    private var stepsCount: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnregistrerParcoursBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialisation des services applicatifs
        initializeServices()

        // Évaluer si l'application à les permissions nécessaires pour compter le nombre de pas
        stepCounterCheckAndRequestPermission()

        // Configuration bottom sheet parcours enregistrement
        val firstBottomSheet =
            view.findViewById<FrameLayout>(R.id.enregistrer_parcours_bottom_sheet)

        val bottomSheetBehavior = BottomSheetBehavior.from(firstBottomSheet)

        bottomSheetBehavior.apply {
            peekHeight =
                resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height) // Hauteur en mode COLLAPSED
            state = BottomSheetBehavior.STATE_COLLAPSED
            isDraggable = false
            isFitToContents = false
        }

        // Configuration bottom sheet parcours enregistrement détails
        val secondBottomSheet =
            view.findViewById<FrameLayout>(R.id.enregistrer_details_parcours_bottom_sheet)
        val secondBottomSheetBehavior = BottomSheetBehavior.from(secondBottomSheet)

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.enregistrer_details_parcours_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = DebuterParcoursAdapter(mutableListOf(), trackInfoUtils)
        recyclerView.adapter = adapter

        secondBottomSheetBehavior.apply {
            peekHeight =
                resources.getDimensionPixelSize(R.dimen.enregistrer_details_parcours_bottom_sheet_peek_height) // Hauteur en mode COLLAPSED
            state = BottomSheetBehavior.STATE_HIDDEN
            isDraggable = true
            isHideable = true
            isFitToContents = true
        }

        // binding des boutons pour une session de marche
        val enregistrerParcoursfabButton: FloatingActionButton =
            view.findViewById(R.id.enregistrer_parcours_fab_button)
        val stopEnregistrerParcoursfabButton: FloatingActionButton =
            view.findViewById(R.id.stop_enregistrer_parcours_fab_button)
        val resumerTerminerEnregistrerParcoursLayout: LinearLayout =
            view.findViewById(R.id.resumer_terminer_enregistrer_parcours_layout)
        val resumerEnregistrerParcoursFabButton: MaterialButton =
            view.findViewById(R.id.resumer_enregistrer_parcours_button)
        val terminerEnregistrerParcoursFabButton: MaterialButton =
            view.findViewById(R.id.terminer_enregistrer_parcours_button)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.terminer_enregistrement_parcours_overlay)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Récupérer l'animation depuis les ressources
        val animationDrawable = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.animated_gradient
        ) as AnimationDrawable

        animationDrawable.setEnterFadeDuration(10)
        animationDrawable.setExitFadeDuration(1000)

        // logique début d'enregistrement
        enregistrerParcoursfabButton.setOnClickListener { _ ->
            Log.i("Parcours", "Préparation de l'enregistrement...")

            // création de dialog pour le décompte
            val countdownDialog = Dialog(requireContext())
            countdownDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            countdownDialog.setContentView(R.layout.countdown_overlay)
            countdownDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            countdownDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            countdownDialog.setCancelable(false)

            val countdownText = countdownDialog.findViewById<TextView>(R.id.countdown_text)
            countdownDialog.show()

            // configuration du décompte avant de débuter un enregistrement
            object : CountDownTimer(3000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = millisUntilFinished / 1000 + 1
                    countdownText.text = secondsRemaining.toString()
                }

                // configuration de la fin du décompte
                override fun onFinish() {
                    Log.i("Parcours", "Enregistrement débuté!")

                    // si parcours historique est sélectionné, effacer le traçage avant un nouvel enregistrement
                    trackingViewModel.triggerClearMap()

                    countdownDialog.dismiss()
                    secondBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                    startTimestamp = Date()
                    // Démarrer le timer et enregistrer le temps de départ
                    startTime = System.currentTimeMillis() - elapsedTime
                    running = true
                    startUpdates()
                    startTimer()

                    secondBottomSheetBehavior.isHideable = !secondBottomSheetBehavior.isHideable
                    enregistrerParcoursfabButton.visibility = View.GONE
                    stopEnregistrerParcoursfabButton.visibility = View.VISIBLE
                    view.findViewById<ConstraintLayout>(R.id.enregistrer_parcours_layout).background =
                        animationDrawable
                    animationDrawable.start()

                    // création d'un nouveau Track et enregistrer dans la BD
                    val track = trackFactory()
                    lifecycleScope.launch {
                        createNewTrack(track)
                    }

                    // commencer le traçage sur la carte
                    trackingViewModel.startTracking(track.trackId)
                }
            }.start()
        }

        // logique pour mettre enregistrement en pause
        stopEnregistrerParcoursfabButton.setOnClickListener { _ ->
            Log.i("Parcours", "Enregistrement arrêté")

            // Arrêter le timer et toutes les détections
            running = false
            elapsedTime = System.currentTimeMillis() - startTime

            stopEnregistrerParcoursfabButton.visibility = View.GONE
            resumerTerminerEnregistrerParcoursLayout.visibility = View.VISIBLE
            view.findViewById<ConstraintLayout>(R.id.enregistrer_parcours_layout)
                .setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.deep_orange_500)
                )
            animationDrawable.stop()
            stepCounterService.stop()
            trackingViewModel.stopTracking()
            accelerationService.stop()
        }

        // logique pour reprendre un enregistrement arrêté
        resumerEnregistrerParcoursFabButton.setOnClickListener { _ ->
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.IO) {
                    // retrouver le track id actif et relancer le traçage du mouvement sur la carte
                    val startedTrack = trackStateUtils.getStartedTrack(database)
                    trackingViewModel.startTracking(startedTrack.trackId)
                }
            }

            // Reprendre le timer
            startTime = System.currentTimeMillis() - elapsedTime
            running = true
            startUpdates()
            startTimer()

            resumerTerminerEnregistrerParcoursLayout.visibility = View.GONE
            stopEnregistrerParcoursfabButton.visibility = View.VISIBLE
            secondBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            view.findViewById<ConstraintLayout>(R.id.enregistrer_parcours_layout).background =
                animationDrawable
            animationDrawable.start()
        }

        val closeButton: ImageView = dialog.findViewById(R.id.terminer_parcours_close_dialog_button)
        val courseTitleInputLayout =
            dialog.findViewById<TextInputLayout>(R.id.course_title_input_layout)
        val courseTitleInput = dialog.findViewById<TextInputEditText>(R.id.course_title_input)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // logique enregistrer un parcours
        terminerEnregistrerParcoursFabButton.setOnClickListener { _ ->
            dialog.findViewById<TextView>(R.id.session_duration).text =
                getDuration(elapsedTime).let {
                    val minutes = it / 60
                    val seconds = it % 60
                    String.format("%02d:%02d", minutes, seconds)
                }

            dialog.findViewById<TextView>(R.id.session_steps).text = stepsCount.let { "$it pas" }

            dialog.findViewById<TextView>(R.id.session_distance).text = totalDistance.let {
                String.format(
                    "%.1f km",
                    it?.div(1000) ?: "Durée inconnue"
                )  // mètres -> kilomètres
            }

            dialog.show()

            trackingViewModel.stopTracking()
            stopUpdates()
        }

        dialog.findViewById<MaterialButton>(R.id.enregistrer_parcours_button).setOnClickListener {
            val title = courseTitleInput.text.toString().trim()

            if (title.isEmpty()) {
                courseTitleInputLayout.error = "Titre obligatoire"
                courseTitleInputLayout.isErrorEnabled = true

                Handler(Looper.getMainLooper()).postDelayed({
                    courseTitleInputLayout.error = null
                }, 3000)

                return@setOnClickListener
            }

            courseTitleInputLayout.error = null
            courseTitleInputLayout.isErrorEnabled = false

            dialog.dismiss()

            lifecycleScope.launch {
                // réinitialiser la liste de parcours historique suite à l'ajout d'un nouveau parcours
                parentFragmentManager.setFragmentResult("refresh_historique", Bundle())

                updateTrack(title)

                findNavController().navigate(R.id.nav_accueil)

                dialogUtils.showBottomDialog(
                    requireContext(),
                    "Parcours enregistré",
                    R.color.success_green,
                    R.drawable.ic_check_circle
                )
            }
        }

        // logique supprimer parcours arrêté
        dialog.findViewById<MaterialButton>(R.id.supprimer_parcours_button).setOnClickListener {
            dialog.dismiss()

            lifecycleScope.launch {
                deleteTrack()
            }

            findNavController().navigate(R.id.nav_accueil)

            dialogUtils.showBottomDialog(
                requireContext(),
                "Parcours supprimé",
                R.color.deep_orange_500,
                R.drawable.ic_delete_24dp
            )
        }
    }

    override fun onStart() {
        super.onStart()

        networkStateObserver.register(object : NetworkStateObserver.NetworkStateCallback {
            val networkStatusIcon =
                requireView().findViewById<ImageView>(R.id.network_status_icon)

            override fun onNetworkAvailable() {
                networkStatusIcon.visibility = View.GONE
                PulsatingEffectUtils.startPulsatingEffect(networkStatusIcon)
                if (running && !isNetworkAvailable) {
                    updateWeather()
                    networkStateObserver.showNetworkStateToast(true)
                    isNetworkAvailable = true
                }
            }

            override fun onNetworkUnavailable() {
                isNetworkAvailable = false
                PulsatingEffectUtils.stopPulsatingEffect(networkStatusIcon)
                networkStatusIcon.visibility = View.VISIBLE
                if (running) {
                    // Mettre les données dépendantes de ces réseaux à null : Cellular Network, Wi-Fi, Bluetooth & NFC.
                    weatherCondition = null
                    temperature = null
                    humidity = null
                    windSpeed = null
                    updateTrackSessionData(System.currentTimeMillis() - startTime)
                    networkStateObserver.showNetworkStateToast(false)
                }
            }
        })
    }

    private fun initializeServices() {
        stepCounterService = StepCounterService(requireContext())
        stepCounterService.onStepCountChanged = { steps ->
            stepsCount = steps
        }

        accelerationService = AccelerationService(requireContext())
        locationService = LocationService(requireContext())
    }

    private fun trackFactory(): Track {
        val username = UserSession.getUsername()
        if (username == null) {
            Log.e("Track", "Username is undefined")
            throw Error("Track: " + "Username is undefined")
        }

        return Track(
            trackId = uuidUtils.generateUUID(),
            username = username,
            trackStatus = "Started",
            startTimestamp = startTimestamp
        )
    }

    private suspend fun createNewTrack(track: Track) {
        withContext(Dispatchers.IO) {
            database.trackDao().insertTrack(track)
        }
    }

    // Fonction pour démarrer le timer
    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (running) {
                    val currentTime = System.currentTimeMillis()
                    val elapsed = currentTime - startTime

                    updateTrackSessionData(elapsed)

                    // Redémarrer la fonction après 1 seconde
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(timerRunnable)
    }

    // Mis a jour des donnees
    private fun updateTrackSessionData(elapsedTime: Long) {
        val trackSessionData = TrackSessionData(
            weatherCondition = if (isNetworkAvailable) weatherCondition else null,
            duration = getDuration(elapsedTime),
            speed = currentSpeed,
            acceleration = acceleration,
            dateTime = startTimestamp,
            temperature = if (isNetworkAvailable) temperature else null,
            steps = stepsCount,
            distance = totalDistance,
            humidity = if (isNetworkAvailable) humidity else null,
            windSpeed = if (isNetworkAvailable) windSpeed else null
        )

        adapter.updateData(listOf(trackSessionData))
    }

    private fun getDuration(elapsedTime: Long): Int {
        return (elapsedTime / 1000).toInt()
    }

    // Mis a jour des conditions climatiques aux 20mins
    private val weatherRunnable = object : Runnable {
        override fun run() {
            updateWeather()
            handler.postDelayed(this, TimeUnit.MINUTES.toMillis(20))
        }
    }

    // Mis a jour de la vitesse + distance chaque seconde
    private val sensorRunnable = object : Runnable {
        override fun run() {
            updateSensors()
            handler.postDelayed(this, TimeUnit.SECONDS.toMillis(1))
        }
    }

    private fun startUpdates() {
        isUpdating = true
        if (stepCounterCheckAndRequestPermission()) {
            stepCounterService.start()
        }

        updateWeather()
        updateSensors()

        accelerationService.start { acceleration ->
            this.acceleration = acceleration
        }

        handler.postDelayed(weatherRunnable, TimeUnit.MINUTES.toMillis(20))
        handler.postDelayed(sensorRunnable, TimeUnit.SECONDS.toMillis(1))
    }

    private fun stopUpdates() {
        isUpdating = false
        handler.removeCallbacks(weatherRunnable)
        handler.removeCallbacks(sensorRunnable)
        stepCounterService.stop()
        accelerationService.stop()
    }

    private fun updateWeather() {
        locationService.getLastLocation { location ->
            location?.let {
                val lat = it.latitude
                val lon = it.longitude

                weatherService.fetchTemperature(
                    lat,
                    lon
                ) { temp, description, humidity, windSpeed, success ->

                    temperature = temp
                    weatherCondition = description
                    this.humidity = humidity
                    this.windSpeed = windSpeed

                    // Insertion dans BD
                    lifecycleScope.launch {
                        insertDataWithRetry(database) { trackId ->
                            persistWeatherData(trackId)
                        }
                    }

                    if (!success) {
                        Log.e(
                            "Météo",
                            "Impossible de récupérer la météo après plusieurs tentatives."
                        )
                    }
                }
            }
        }
    }

    private fun updateSensors() {
        locationService.getLocationData { location, speed ->
            location?.let {
                currentSpeed = speed

                // Calcul de la distance
                lastLocation?.let { previous ->
                    val distance = previous.distanceTo(it)
                    totalDistance = (totalDistance ?: 0f) + distance
                }

                lastLocation = it
            }
        }

        // Insertion dans BD
        lifecycleScope.launch {
            insertDataWithRetry(database) { trackId ->
                persistSensorData(trackId)
            }
        }
    }

    private suspend fun persistSensorData(trackId: String) {
        withContext(Dispatchers.IO) {
            database.sensorDataDao().insertSensorData(
                SensorData(
                    trackId = trackId,
                    timestamp = Date(),
                    accelerometer = if (isNetworkAvailable) acceleration else null,
                )
            )
        }
    }

    private suspend fun persistWeatherData(trackId: String) {
        withContext(Dispatchers.IO) {
            database.weatherDataDao().insertWeatherData(
                WeatherData(
                    trackId = trackId,
                    timestamp = Date(),
                    temperature = if (isNetworkAvailable) temperature else null,
                    humidity = if (isNetworkAvailable) humidity else null,
                    windSpeed = if (isNetworkAvailable) windSpeed else null,
                    weatherCondition = if (isNetworkAvailable) weatherCondition else null,
                )
            )
        }
    }

    private fun stepCounterCheckAndRequestPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    100
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != 100) {
            Toast.makeText(
                requireContext(),
                "Manque de permission pour le compte de pas",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun updateTrack(trackName: String) {
        withContext(Dispatchers.IO) {
            val startedTrack = trackStateUtils.getStartedTrack(database)

            val locationPoints = trackingViewModel.pathPoints.value ?: emptyList()
            if (locationPoints.size < 2) {
                Log.w("Track", "Pas assez de points pour enregistrer le parcours")
                return@withContext
            }

            val sensorDataPoints =
                database.sensorDataDao().getSensorDataForTrack(startedTrack.trackId)
            val weatherDataPoints =
                database.weatherDataDao().getWeatherDataForTrack(startedTrack.trackId)

            startedTrack.trackName = trackName
            startedTrack.endTimestamp = Date()
            startedTrack.duration = getDuration(elapsedTime)
            startedTrack.distance = totalDistance
            startedTrack.stepsCount = stepsCount
            startedTrack.averageSpeed =
                TrackAverageCalculatorUtils.calculateAverageSpeed(locationPoints)
            startedTrack.averageAcceleration =
                TrackAverageCalculatorUtils.calculateAverageAccelerometer(sensorDataPoints)
            startedTrack.weatherCondition =
                TrackAverageCalculatorUtils.calculateMostFrequentWeatherCondition(
                    weatherDataPoints
                )
            startedTrack.temperature =
                TrackAverageCalculatorUtils.calculateAverageTemperature(weatherDataPoints)
            startedTrack.averageHumidity =
                TrackAverageCalculatorUtils.calculateAverageHumidity(weatherDataPoints)
            startedTrack.averageWindSpeed =
                TrackAverageCalculatorUtils.calculateAverageWindSpeed(weatherDataPoints)
            startedTrack.trackStatus = "Completed"

            database.trackDao().updateTrack(startedTrack)

            trackingViewModel.resetPath()
            Log.i("Track", "Track updated")
        }
    }

    private suspend fun deleteTrack() {
        withContext(Dispatchers.IO) {
            val username = UserSession.getUsername()

            if (username == null) {
                Log.e("Track", "Le nom d'utilisateur n'est pas définit")
                return@withContext
            }

            database.trackDao().deleteStartedTrackForUser(username)

            Log.i("Track", "Parcours supprimé")
        }
    }

    private suspend fun insertDataWithRetry(
        database: AppDatabase,
        persistDataFunction: suspend (trackId: String) -> Unit
    ) {
        val maxRetries = 5
        val retryDelayMillis = 1000L
        var retries = 0
        var success = false

        while (retries < maxRetries && !success) {
            try {
                withContext(Dispatchers.IO) {
                    val startedTrack = trackStateUtils.getStartedTrack(database)

                    persistDataFunction(startedTrack.trackId)
                }
                // Si aucune exception n'est levée, la tentative est un succès
                success = true
            } catch (e: Exception) {
                retries++
                Log.w(
                    "DatabaseRetry",
                    "Échec de l'opération, tentative $retries/$maxRetries : ${e.message}"
                )
                if (retries < maxRetries) {
                    // Attendre 1 seconde avant de réessayer
                    delay(retryDelayMillis)
                } else {
                    Log.e("DatabaseRetry", "Opération abandonnée après $maxRetries tentatives.")
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        networkStateObserver.unregister()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stepCounterService.cleanup()
        accelerationService.stop()
        trackingViewModel.triggerClearMap()
        trackingViewModel.stopTracking()
        trackingViewModel.resetPath()
        _binding = null
    }
}