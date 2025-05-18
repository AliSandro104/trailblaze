package com.example.inf8405tf.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inf8405tf.R
import com.example.inf8405tf.domain.TrackSessionData
import com.example.inf8405tf.utils.TrackInfoUtils

class DebuterParcoursAdapter(
    private var sessions: List<TrackSessionData>,
    private val trackInfoUtils: TrackInfoUtils
) : RecyclerView.Adapter<DebuterParcoursAdapter.SessionViewHolder>() {

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sessionTime: TextView = view.findViewById(R.id.session_time)
        val sessionDuration: TextView = view.findViewById(R.id.session_duration)
        val sessionDistance: TextView = view.findViewById(R.id.session_distance)
        val sessionSteps: TextView = view.findViewById(R.id.session_steps)

        val additionalInfoSection: View = view.findViewById(R.id.track_additional_info_section)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_list_item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]

        holder.sessionTime.text = trackInfoUtils.formatLongDateTime(session.dateTime)
        holder.sessionDuration.text = trackInfoUtils.formatDuration(session.duration)
        holder.sessionDistance.text = trackInfoUtils.formatDistance(session.distance)
        holder.sessionSteps.text = trackInfoUtils.formatStepsCount(session.steps)

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_weather_condition).text =
            trackInfoUtils.formatWeatherCondition("Conditions climatiques", session.weatherCondition)

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_speed).text =
            trackInfoUtils.formatSpeed("Vitesse", session.speed)

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_acceleration).text =
            trackInfoUtils.formatAcceleration("Accélération", session.acceleration)

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_temperature).text =
            trackInfoUtils.formatTemperature("Température", session.temperature)

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_humidity).text =
            trackInfoUtils.formatHumidity("Humidité", session.humidity)

        holder.additionalInfoSection.findViewById<TextView>(R.id.track_additional_info_wind).text =
            trackInfoUtils.formatWindSpeed("Vent", session.windSpeed)
    }

    override fun getItemCount() = sessions.size

    fun updateData(newSessions: List<TrackSessionData>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}
