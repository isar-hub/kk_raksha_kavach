package com.isar.kkrakshakavach.retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiClass {
    @GET("api/shorten")
    fun shortenUrl(
        @Query("url") url: String
    ): retrofit2.Call<ShortenedUrlResponse>
}

data class ShortenedUrlResponse(
    val shortenedUrl: String // Adjust based on API response
)
