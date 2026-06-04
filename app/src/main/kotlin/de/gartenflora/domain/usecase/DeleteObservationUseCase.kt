package de.gartenflora.domain.usecase

import de.gartenflora.data.repository.PlantRepository
import javax.inject.Inject

class DeleteObservationUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> =
        repository.deleteObservation(id)
}
