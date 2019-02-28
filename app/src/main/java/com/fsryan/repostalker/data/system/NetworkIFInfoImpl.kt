package com.fsryan.repostalker.data.system

import android.annotation.SuppressLint
import android.content.Context

import android.net.ConnectivityManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.annotation.VisibleForTesting
import io.reactivex.Single

fun createNetworkIfInfo(context: Context): NetworkIfInfo = NetworkIFInfoImpl(context)

private class NetworkIFInfoImpl(context: Context, @VisibleForTesting private val sdkInt: Int = Build.VERSION.SDK_INT) : NetworkIfInfo {
    private val context: Context = context.applicationContext

    @SuppressLint("NewApi") // <-- checked by sdk int
    override fun isConnectedToNetwork(): Single<Boolean> = Single.fromCallable<Boolean> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return@fromCallable if (sdkInt >= Build.VERSION_CODES.M) hasActiveNetwork23(cm)
                else hasActiveNetwork(cm)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasActiveNetwork23(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork
        return activeNetwork != null && cm.getNetworkInfo(activeNetwork).isConnected
    }

    private fun hasActiveNetwork(cm: ConnectivityManager): Boolean {
        val activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}