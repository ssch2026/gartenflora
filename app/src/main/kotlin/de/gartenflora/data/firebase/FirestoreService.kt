package de.gartenflora.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.gartenflora.domain.model.PlantObservation
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /** Sign in anonymously if no current user. Safe to call multiple times. */
    suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    private fun uid(): String? = auth.currentUser?.uid

    // ── Observations ─────────────────────────────────────────────────────────

    suspend fun syncObservation(observation: PlantObservation) {
        val uid = uid() ?: return
        // Photo upload requires Firebase Storage (Blaze plan). Skipped on Spark plan —
        // photos stay local only. Enable by upgrading to Blaze in the Firebase Console.
        val doc = observation.toMap(photoUrls = emptyList())
        firestore
            .collection("users").document(uid)
            .collection("observations").document(observation.id.toString())
            .set(doc, SetOptions.merge())
            .await()
    }

    suspend fun deleteObservation(id: Long) {
        val uid = uid() ?: return
        firestore
            .collection("users").document(uid)
            .collection("observations").document(id.toString())
            .delete()
            .await()
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun PlantObservation.toMap(photoUrls: List<String>): Map<String, Any?> = mapOf(
        "id"             to id,
        "scientificName" to scientificName,
        "commonNameDe"   to commonNameDe,
        "family"         to family,
        "genus"          to genus,
        "customName"     to customName,
        "confidence"     to confidence,
        "gbifId"         to gbifId,
        "photoPaths"     to photoPaths,
        "photoUrls"      to photoUrls,
        "latitude"       to latitude,
        "longitude"      to longitude,
        "gardenSpot"     to gardenSpot,
        "careNotes"      to careNotes,
        "userNotes"      to userNotes,
        "createdAt"      to createdAt
    )
}
