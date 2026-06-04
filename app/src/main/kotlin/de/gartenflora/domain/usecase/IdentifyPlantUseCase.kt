package de.gartenflora.domain.usecase

import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.domain.model.PlantCandidate
import javax.inject.Inject

class IdentifyPlantUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(
        imagePaths: List<String>,
        organs: List<String>,
        project: String = "all"
    ): Result<List<PlantCandidate>> {
        if (imagePaths.isEmpty()) {
            return Result.failure(IllegalArgumentException("Mindestens ein Bild erforderlich"))
        }
        if (organs.size != imagePaths.size) {
            return Result.failure(
                IllegalArgumentException("Anzahl der Organe muss der Anzahl der Bilder entsprechen")
            )
        }
        return repository.identifyPlant(imagePaths, organs, project)
    }
}
