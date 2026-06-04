package de.gartenflora.domain.usecase

import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.PlantObservation
import javax.inject.Inject

class SaveObservationUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(observation: PlantObservation): Result<Long> =
        repository.saveObservation(observation)
}
