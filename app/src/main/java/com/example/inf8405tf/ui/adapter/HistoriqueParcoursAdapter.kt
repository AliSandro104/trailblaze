package com.example.inf8405tf.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inf8405tf.R
import com.example.inf8405tf.domain.Track
import com.example.inf8405tf.utils.TrackInfoUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HistoriqueParcoursAdapter(
    private var tracks: List<Track>,
    private var trackInfoUtils: TrackInfoUtils,
    private val onAfficherParcoursClicked: (trackId: String) -> Unit
) : RecyclerView.Adapter<HistoriqueParcoursAdapter.ParcoursViewHolder>() {

    class ParcoursViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nomText: TextView = view.findViewById(R.id.parcours_nom)
        val dateText: TextView = view.findViewById(R.id.parcours_date)
        val distanceText: TextView = view.findViewById(R.id.parcours_distance)
        val dureeText: TextView = view.findViewById(R.id.parcours_duree)
        val allureText: TextView = view.findViewById(R.id.parcours_allure)
        val additionalInfoSection: View = view.findViewById(R.id.track_additional_info_section)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParcoursViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_list_item_parcours, parent, false)

        return ParcoursViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParcoursViewHolder, position: Int) {
        val track = tracks[position]

        // Configure les vues principales comme avant
        holder.nomText.text = track.trackName ?: "Nom inconnu"
        holder.dateText.text = trackInfoUtils.formatShortDateTime(track.startTimestamp)
        holder.distanceText.text = trackInfoUtils.formatDistance(track.distance)
        holder.dureeText.text = trackInfoUtils.formatDuration(track.duration)
        holder.allureText.text = trackInfoUtils.formatSpeedNoLabel(track.averageSpeed)
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_speed).visibility =
            View.GONE
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_start_time).visibility =
            View.VISIBLE
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_end_time).visibility =
            View.VISIBLE
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_steps_count).visibility =
            View.VISIBLE

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_start_time).text =
            trackInfoUtils.formatTime("Heure début", track.startTimestamp)
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_end_time).text =
            trackInfoUtils.formatTime("Heure fin", track.endTimestamp)
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_steps_count).text = trackInfoUtils.formatStepsCountLabel(track.stepsCount)

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_acceleration).text =
            trackInfoUtils.formatAcceleration("X̄ Accélération", track.averageAcceleration)
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_weather_condition).text =
            trackInfoUtils.formatWeatherCondition("X̄ Conditions climatiques", track.weatherCondition)
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_temperature).text =
            trackInfoUtils.formatTemperature("X̄ Température", track.temperature)
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_humidity).text =
            trackInfoUtils.formatHumidity("X̄ Humidité", track.averageHumidity)
        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_wind).text =
            trackInfoUtils.formatWindSpeed("X̄ Vent", track.averageWindSpeed)

        // Gérer la visibilité de la section extensible
        val detailsLayout = holder.itemView.findViewById<LinearLayout>(R.id.parcours_details_layout)
        val separatorView = holder.itemView.findViewById<View>(R.id.parcours_separator)

        if (track.isExpanded) {
            detailsLayout.visibility = View.VISIBLE
            separatorView.visibility = View.VISIBLE // Montrer la barre
        } else {
            detailsLayout.visibility = View.GONE
            separatorView.visibility = View.GONE // Cacher la barre
        }

        // Gérer le clic pour basculer l'état d'extension
        holder.itemView.setOnClickListener {
            track.isExpanded = !track.isExpanded
            notifyItemChanged(position)
        }

        // Gerer clic sur le bouton pour consulter les details d'un parcours sauvegardé
        val actionButton = holder.itemView.findViewById<FloatingActionButton>(R.id.parcours_action_button)
        actionButton.setOnClickListener {
            onAfficherParcoursClicked(track.trackId)
        }
    }

    override fun getItemCount() = tracks.size

    fun updateData(newTracks: List<Track>) {
        tracks = newTracks
        notifyDataSetChanged()
    }
}
