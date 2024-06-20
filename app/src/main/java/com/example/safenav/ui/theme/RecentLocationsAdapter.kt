package com.example.safenav.ui.theme

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.safenav.R
import android.graphics.Color
data class RecentLocation(
    val latitude: Double,
    val longitude: Double,
    val street: String,
    val timestamp: String // Aquí podrías usar el tipo de dato adecuado para representar la fecha y la hora
)
class RecentLocationsAdapter(
    context: Context,
    private val recentLocations: List<RecentLocation>
) : ArrayAdapter<RecentLocation>(context, R.layout.list_item_recent_location, recentLocations) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_recent_location, parent, false)

        val recentLocation = recentLocations[position]

        val latitudeTextView: TextView = view.findViewById(R.id.latitude_text_view)
        val longitudeTextView: TextView = view.findViewById(R.id.longitude_text_view)
        val streetNameTextView: TextView = view.findViewById(R.id.street_name_text_view)
        val dateTextView: TextView = view.findViewById(R.id.date_text_view)

        // Establecer el texto y el color para cada TextView
        latitudeTextView.text = "Latitud: ${recentLocation.latitude}"
        latitudeTextView.setTextColor(Color.BLACK) // Establecer color de texto a negro

        longitudeTextView.text = "Longitud: ${recentLocation.longitude}"
        longitudeTextView.setTextColor(Color.BLACK) // Establecer color de texto a negro

        streetNameTextView.text = "Nombre de la calle: ${recentLocation.street}"
        streetNameTextView.setTextColor(Color.BLACK) // Establecer color de texto a negro

        dateTextView.text = "Fecha: ${recentLocation.timestamp}"
        dateTextView.setTextColor(Color.BLACK) // Establecer color de texto a negro

        return view
    }
}