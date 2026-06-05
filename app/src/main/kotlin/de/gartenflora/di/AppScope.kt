package de.gartenflora.di

import javax.inject.Qualifier

/** Coroutine scope tied to the application lifetime (for fire-and-forget background work). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
