package com.eqnitymusic.damn.data.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import retrofit2.http.Streaming

interface ApiService {
    @GET("api/json")
    fun getYtmp3Info(@Query("url") url: String): Call<Ytmp3Response>

    @Streaming
    @GET
    fun downloadFile(@Url url: String): Call<ResponseBody>

    @GET
    fun getEqnityInfo(@Url url: String): Call<EqnityInfoResponse>
}

data class Ytmp3Response(
    val status: String,
    val title: String?,
    val artist: String?,
    val duration: String?,
    val downloadUrl: String?,
    val thumbnail: String?,
    val error: String?
)

data class EqnityInfoResponse(
    val name: String,
    val tagline: String,
    val about: String,
    val skills: List<Skill>,
    val contact: ContactInfo,
    val links: Links
)

data class Skill(
    val title: String,
    val description: String,
    val technologies: String
)

data class ContactInfo(
    val github: String,
    val discord: String
)

data class Links(
    val github: String,
    val discord: String
)