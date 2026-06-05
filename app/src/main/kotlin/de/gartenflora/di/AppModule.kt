package de.gartenflora.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.gartenflora.BuildConfig
import de.gartenflora.data.local.AppDatabase
import de.gartenflora.data.local.GardenCellDao
import de.gartenflora.data.local.GardenZoneDao
import de.gartenflora.data.local.PlantObservationDao
import de.gartenflora.data.remote.GeminiApiService
import de.gartenflora.data.remote.PlantIdApiService
import de.gartenflora.data.remote.PlantNetApiService
import de.gartenflora.data.repository.GardenRepository
import de.gartenflora.data.repository.GardenRepositoryImpl
import de.gartenflora.data.repository.PlantRepository
import de.gartenflora.data.repository.PlantRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    @Named("default_okhttp")
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** Dedicated OkHttp client that injects the Plant.id Api-Key on every request. */
    @Provides
    @Singleton
    @Named("plantid_okhttp")
    fun providePlantIdOkHttpClient(
        @Named("plantid_api_key") apiKey: String
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Api-Key", apiKey)
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ── Retrofit instances ────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("plantnet_retrofit")
    fun providePlantNetRetrofit(
        @Named("default_okhttp") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://my-api.plantnet.org/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @Named("gemini_retrofit")
    fun provideGeminiRetrofit(
        @Named("default_okhttp") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @Named("plantid_retrofit")
    fun providePlantIdRetrofit(
        @Named("plantid_okhttp") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://plant.id/api/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // ── API services ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun providePlantNetApiService(
        @Named("plantnet_retrofit") retrofit: Retrofit
    ): PlantNetApiService = retrofit.create(PlantNetApiService::class.java)

    @Provides
    @Singleton
    fun provideGeminiApiService(
        @Named("gemini_retrofit") retrofit: Retrofit
    ): GeminiApiService = retrofit.create(GeminiApiService::class.java)

    @Provides
    @Singleton
    fun providePlantIdApiService(
        @Named("plantid_retrofit") retrofit: Retrofit
    ): PlantIdApiService = retrofit.create(PlantIdApiService::class.java)

    // ── API keys ──────────────────────────────────────────────────────────────

    @Provides
    @Named("plantnet_api_key")
    fun providePlantNetApiKey(): String = BuildConfig.PLANTNET_API_KEY

    @Provides
    @Named("gemini_api_key")
    fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides
    @Named("plantid_api_key")
    fun providePlantIdApiKey(): String = BuildConfig.PLANTID_API_KEY
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "gartenflora.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun providePlantObservationDao(db: AppDatabase): PlantObservationDao =
        db.plantObservationDao()

    @Provides
    @Singleton
    fun provideGardenZoneDao(db: AppDatabase): GardenZoneDao =
        db.gardenZoneDao()

    @Provides
    @Singleton
    fun provideGardenCellDao(db: AppDatabase): GardenCellDao =
        db.gardenCellDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlantRepository(impl: PlantRepositoryImpl): PlantRepository

    @Binds
    @Singleton
    abstract fun bindGardenRepository(impl: GardenRepositoryImpl): GardenRepository
}
