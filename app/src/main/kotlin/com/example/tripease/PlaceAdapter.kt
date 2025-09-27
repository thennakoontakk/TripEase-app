package com.example.tripease

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class PlaceAdapter(
    private var places: List<TravelPlace>,
    private val onPlaceClick: (TravelPlace) -> Unit,
    private val onFavoriteClick: (TravelPlace) -> Unit
) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPlaceImage: ShapeableImageView = itemView.findViewById(R.id.ivPlaceImage)
        val tvPlaceName: TextView = itemView.findViewById(R.id.tvPlaceName)
        val tvPlaceCategory: TextView = itemView.findViewById(R.id.tvPlaceCategory)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]
        
        holder.tvPlaceName.text = place.name
        holder.tvPlaceCategory.text = place.category
        holder.ratingBar.rating = place.rating.toFloat()
        holder.tvRating.text = place.rating.toFloat().toString()
        holder.tvDistance.text = "${place.distance} km"
        
        // Set place image (placeholder for now)
        holder.ivPlaceImage.setImageResource(R.drawable.ic_place_placeholder)
        
        // Handle favorite toggle
        holder.ivFavorite.setOnClickListener {
            onFavoriteClick(place)
            updateFavoriteIcon(holder.ivFavorite, place.isFavorite)
        }
        
        // Update favorite icon
        updateFavoriteIcon(holder.ivFavorite, place.isFavorite)
        
        // Handle item click
        holder.itemView.setOnClickListener {
            onPlaceClick(place)
        }
    }

    override fun getItemCount(): Int = places.size

    private fun updateFavoriteIcon(imageView: ImageView, isFavorite: Boolean) {
        if (isFavorite) {
            imageView.setImageResource(R.drawable.ic_favorite)
        } else {
            imageView.setImageResource(R.drawable.ic_favorite_border)
        }
    }

    fun updatePlaces(newPlaces: List<TravelPlace>) {
        places = newPlaces
        notifyDataSetChanged()
    }
}