package com.example.tripease

data class TravelPlace(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double = 0.0,
    val imageUrl: String? = null,
    var distance: Double = 0.0,
    val address: String? = null,
    val website: String? = null
)

data class OpenTripMapResponse(
    val features: List<Feature>
)

data class Feature(
    val properties: Properties,
    val geometry: Geometry
)

data class Properties(
    val xid: String,
    val name: String,
    val kinds: String,
    val rate: Int = 0
)

data class Geometry(
    val coordinates: List<Double>
)

data class PlaceDetails(
    val xid: String,
    val name: String,
    val address: Address?,
    val rate: Int,
    val kinds: String,
    val sources: Sources?,
    val otm: String?,
    val wikipedia: String?,
    val image: String?,
    val preview: Preview?,
    val wikipedia_extracts: WikipediaExtracts?
)

data class Address(
    val city: String?,
    val state: String?,
    val country: String?,
    val postcode: String?,
    val country_code: String?
)

data class Sources(
    val geometry: String?,
    val attributes: List<String>?
)

data class Preview(
    val source: String?,
    val height: Int?,
    val width: Int?
)

data class WikipediaExtracts(
    val title: String?,
    val text: String?,
    val html: String?
)