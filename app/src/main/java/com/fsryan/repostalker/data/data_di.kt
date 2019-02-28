package com.fsryan.repostalker.data

import android.content.Context
import android.util.Log
import com.fsryan.repostalker.App
import com.fsryan.repostalker.BuildConfig
import com.fsryan.repostalker.data.db.DbDao
import com.fsryan.repostalker.data.db.createDbDao
import com.fsryan.repostalker.data.nav.Navigator
import com.fsryan.repostalker.data.network.GithubService
import com.fsryan.repostalker.data.network.ReferrerInterceptor
import com.fsryan.repostalker.data.prefs.UserPrefs
import com.fsryan.repostalker.data.prefs.createUserPrefs
import com.fsryan.repostalker.data.system.NetworkIfInfo
import com.fsryan.repostalker.data.system.createNetworkIfInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*

@Module
class DataModule(context: Context) {
    private val appContext = context.applicationContext

    @Provides
    @App.Scope
    fun navigator(): Navigator = Navigator.create(AndroidSchedulers.mainThread())

    @Provides
    @App.Scope
    fun userPrefs(): UserPrefs = createUserPrefs(appContext)

    @Provides
    @App.Scope
    fun dbDao(): DbDao = createDbDao()

    // Actually unused--I cut this for time
    @Provides
    @App.Scope
    fun networkIfINfo(): NetworkIfInfo = createNetworkIfInfo(appContext)

    // If you wanted to test that the service would behave appropriately, you
    // could use com.squareup.okhttp3:mockwebserver to mock responses.
    @Provides
    @App.Scope
    fun githubService(retrofit: Retrofit): GithubService = retrofit.create(GithubService::class.java)

    @Provides
    @App.Scope
    fun retrofit(okhttpClient: OkHttpClient, moshiConverterFactory: MoshiConverterFactory): Retrofit = Retrofit.Builder()
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(moshiConverterFactory)
        .client(okhttpClient)
        .baseUrl("https://api.github.com")
        .build()

    @Provides
    @App.Scope
    fun okHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
                .addInterceptor(ReferrerInterceptor("android-app://${appContext.packageName}"))

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { Log.i("repostalker_network", it) }
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(loggingInterceptor)
        }

        // TODO: add call timeouts, etc
        return builder.build()
    }

    @Provides
    @App.Scope
    fun moshiConverterFactory(moshi: Moshi): MoshiConverterFactory = MoshiConverterFactory.create(moshi)

    @Provides
    @App.Scope
    fun moshi(): Moshi = Moshi.Builder()
        .add(AdapterFactory())
        .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .build()
}