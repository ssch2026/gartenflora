package de.gartenflora.data.remote

import de.gartenflora.data.remote.dto.PlantIdHealthRequest
import de.gartenflora.data.remote.dto.PlantIdHealthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface PlantIdApiService {

    /**
     * Health assessment — detects diseases, pests, nutrient deficiencies.
     * Api-Key header is injected by the OkHttp interceptor in AppModule.
     */
    @POST("v3/health_assessment")
    suspend fun healthAssessment(
        @Body request: PlantIdHealthRequest
    ): PlantIdHealthResponse
}
