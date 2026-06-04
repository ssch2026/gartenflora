package de.gartenflora.domain.usecase

import de.gartenflora.data.repository.PlantRepository
import javax.inject.Inject

class EnrichWithGeminiUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(scientificName: String): Result<String> =
        repository.generateCareNotes(scientificName)
}
