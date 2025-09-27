package com.example.tripease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.imageview.ShapeableImageView

class PlaceCarouselAdapter(
    private var places: List<FirebasePlace> = emptyList(),
    private val onPlaceClick: (FirebasePlace) -> Unit = {}
) : RecyclerView.Adapter<PlaceCarouselAdapter.PlaceViewHolder>() {

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPlaceImage: ShapeableImageView = itemView.findViewById(R.id.ivPlaceImage)
        private val tvPlaceName: TextView = itemView.findViewById(R.id.tvPlaceName)
        private val tvProvince: TextView = itemView.findViewById(R.id.tvProvince)

        fun bind(place: FirebasePlace) {
            try {
                tvPlaceName.text = place.name ?: "Unknown Place"
                tvProvince.text = place.province ?: "Unknown Province"

                // Add logging for debugging
                android.util.Log.d("PlaceCarouselAdapter", "Binding place: ${place.name}, Image URL: ${place.imageURL}")
                android.util.Log.d("PlaceCarouselAdapter", "Download URL: ${place.downloadImageURL}")

                // Load image using Glide with proper error handling
                val imageUrl = place.downloadImageURL
                if (imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_place_placeholder)
                        .error(R.drawable.ic_place_placeholder)
                        .transform(CenterCrop(), RoundedCorners(32))
                        .into(ivPlaceImage)
                } else {
                    // Set placeholder if no image URL
                    ivPlaceImage.setImageResource(R.drawable.ic_place_placeholder)
                }

                // Set click listener
                itemView.setOnClickListener {
                    onPlaceClick(place)
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaceCarouselAdapter", "Error binding place: ${place.name}", e)
                // Set fallback values
                tvPlaceName.text = "Error loading place"
                tvProvince.text = ""
                ivPlaceImage.setImageResource(R.drawable.ic_place_placeholder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_card, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(places[position])
    }

    override fun getItemCount(): Int = places.size

    fun updatePlaces(newPlaces: List<FirebasePlace>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}