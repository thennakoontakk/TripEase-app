package com.example.tripease

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenTripMapService {
    
    @GET("0.1/en/places/radius")
    suspend fun getPlacesInRadius(
        @Query("radius") radius: Int = 10000, // 10km radius
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("kinds") kinds: String = "tourist_attractions,museums,historic,natural,cultural,architecture",
        @Query("format") format: String = "geojson",
        @Query("limit") limit: Int = 50,
        @Query("apikey") apiKey: String
    ): Response<OpenTripMapResponse>
    
    @GET("0.1/en/places/xid/{xid}")
    suspend fun getPlaceDetails(
        @Path("xid") xid: String,
        @Query("apikey") apiKey: String
    ): Response<PlaceDetails>
    
    companion object {
        const val BASE_URL = "https://api.opentripmap.com/"
        const val API_KEY = "5ae2e3f221c38a28845f05b6c4b8b8b8b8b8b8b8" // Replace with your actual API key
    }
}