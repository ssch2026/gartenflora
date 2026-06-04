package de.gartenflora.domain.usecase

import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.PlantObservation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetObservationsUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    operator fun invoke(): Flow<List<PlantObservation>> =
        repository.observeAllObservations()

    fun search(query: String): Flow<List<PlantObservation>> =
        repository.searchObservations(query)

    fun observeById(id: Long): Flow<PlantObservation?> =
        repository.observeObservation(id)
}
