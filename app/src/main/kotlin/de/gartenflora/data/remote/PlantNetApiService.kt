package de.gartenflora.data.remote

import de.gartenflora.data.remote.dto.PlantNetProject
import de.gartenflora.data.remote.dto.PlantNetResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlantNetApiService {

    @Multipart
    @POST("v2/identify/{project}")
    suspend fun identify(
        @Path("project") project: String,
        @Query("api-key") apiKey: String,
        @Query("lang") lang: String = "de",
        @Part images: List<MultipartBody.Part>,
        @Part organs: List<MultipartBody.Part>
    ): PlantNetResponse

    @GET("v2/projects")
    suspend fun getProjects(
        @Query("api-key") apiKey: String,
        @Query("lang") lang: String = "de"
    ): List<PlantNetProject>
}
