package com.example.inf8405tf.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.inf8405tf.R
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.ui.viewmodel.TrackingViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val trackingViewModel: TrackingViewModel by activityViewModels()
    private var polyline: Polyline? = null
    private var trackIdInWaiting: String? = null
    private var startMarker: com.google.android.gms.maps.model.Marker? = null

    @Inject
    lateinit var database: AppDatabase

    private var lastLocation: LatLng? = null

    private var shouldClearMap = false

    private lateinit var clockView: TextView
    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var clockRunnable: Runnable


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) enableMyLocation()
            else Log.d("MapsFragment", "Localisation n'est pas activée")
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_maps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        clockView   = view.findViewById(R.id.clock_view)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        startClock()

        // observer les points de données gps
        trackingViewModel.pathPoints.observe(viewLifecycleOwner) { locationPoints ->
            if (!::mMap.isInitialized) return@observe

            // convertit les points en objet LatLng et les ajoute dans polyline pour le traçage
            if (locationPoints.size >= 2) {
                val path = locationPoints.map { LatLng(it.latitude, it.longitude) }

                if (polyline == null) {
                    polyline = mMap.addPolyline(PolylineOptions()
                        .addAll(path).
                        color(ContextCompat.getColor(requireContext(), R.color.teal_700)))
                } else {
                    polyline?.points = path
                }
            }
        }

        // observer le parcours historique à afficher
        trackingViewModel.selectedTrackId.observe(viewLifecycleOwner) { trackId ->
            if (!::mMap.isInitialized) {
                // garde le parcours à afficher si la carte GoogleMap n'est pas intialisée
                trackIdInWaiting = trackId
                return@observe
            }

            if (trackId != null) {
                afficherParcoursEnregistre(trackId)
            }
        }

        // observe l'événement pour enlever le traçage, enclenché par HistoriqueParcours et DebuterParcours
        trackingViewModel.clearMapEvent.observe(viewLifecycleOwner) {
            if (::mMap.isInitialized) {
                clearMap()
            } else {
                shouldClearMap = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startClock()
    }

    override fun onPause() {
        super.onPause()
        stopClock()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Activer la boussole
        mMap.uiSettings.isCompassEnabled = true
        enableMyLocation()

        if (shouldClearMap) {
            clearMap()
            shouldClearMap = false
        }

        // affiche parcours enregistré si défini avant que la carte soit intialisée
        trackIdInWaiting?.let {
            afficherParcoursEnregistre(it)
            trackIdInWaiting = null
        }
    }

    // Focus camera sur notre location de depart
    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    lastLocation = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation!!, 17f))
                } ?: Log.d("MapsFragment", "Location is null")
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Affiche l'heure actuelle
    private fun startClock() {
        clockRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(now))
                clockView.text = timeString
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)
    }

    private fun stopClock() {
        clockHandler.removeCallbacks(clockRunnable)
    }


    // retrouver les LocationPoints à partir du trackId et convertir en LatLng pour afficher sur la carte
    private fun afficherParcoursEnregistre(trackId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val points = database.locationPointDao().getLocationPointsForTrack(trackId)
            val latLngs = points.map { LatLng(it.latitude, it.longitude) }

            withContext(Dispatchers.Main) {
                if (latLngs.isNotEmpty()) {
                    clearMap()

                    startMarker = mMap.addMarker(
                        com.google.android.gms.maps.model.MarkerOptions()
                            .position(latLngs.first())
                            .title("Start")
                    )

                    mMap.addPolyline(
                        PolylineOptions()
                            .addAll(latLngs)
                            .color(ContextCompat.getColor(requireContext(), R.color.teal_700))
                            .width(8f)
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 16f))
                }
            }
        }
    }

    // enlever le chemin affiché sur la carte
    private fun clearMap() {
        polyline = null
        startMarker = null
        mMap.clear()
    }
}
