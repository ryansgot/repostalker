package com.fsryan.repostalker.data.network

import okhttp3.Interceptor
import okhttp3.Response

class ReferrerInterceptor(private val referer: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response= chain.proceed(chain.request()
            .newBuilder()
            .addHeader("Referer", referer)
            .build())
}