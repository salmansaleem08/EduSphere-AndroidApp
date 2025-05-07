package com.salmansaleem.edusphere

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class EduSphereApplication : Application() {
    private val TAG = "EduSphereApplication"
    private lateinit var networkReceiver: NetworkChangeReceiver

    override fun onCreate() {
        super.onCreate()
        // Register network change receiver
        networkReceiver = NetworkChangeReceiver()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
        Log.d(TAG, "NetworkChangeReceiver registered")
    }

    override fun onTerminate() {
        super.onTerminate()
        // Unregister receiver to prevent leaks
        try {
            unregisterReceiver(networkReceiver)
            Log.d(TAG, "NetworkChangeReceiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering NetworkChangeReceiver: ${e.message}")
        }
    }
}

class NetworkChangeReceiver : BroadcastReceiver() {
    private val TAG = "NetworkChangeReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                Log.d(TAG, "Network connected, triggering immediate sync")
                triggerImmediateSync(context)
            } else {
                Log.d(TAG, "Network disconnected")
            }
        }
    }

    private fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("immediate_profile_sync", ExistingWorkPolicy.KEEP, syncRequest)
        Log.d(TAG, "Enqueued immediate sync work")
    }
}