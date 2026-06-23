package com.makd.afinity.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makd.afinity.data.repository.AppDataRepository
import timber.log.Timber

class HomeDataReloadWorker
constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val appDataRepository: AppDataRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("HomeDataReloadWorker: starting home data reload (attempt ${runAttemptCount + 1})")

        return try {
            appDataRepository.reloadHomeData()

            if (appDataRepository.libraries.value.isNotEmpty()) {
                Timber.d("HomeDataReloadWorker: libraries loaded successfully")
                Result.success()
            } else {
                Timber.w("HomeDataReloadWorker: libraries empty after reload, retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "HomeDataReloadWorker: reload failed")
            Result.retry()
        }
    }
}