package com.fsryan.repostalker.data.network

import com.fsryan.repostalker.data.GithubMember
import com.fsryan.repostalker.data.GithubUser
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.*

interface GithubService {
    @GET("orgs/{org}/members")
    @Headers("Accept: application/vnd.github.v3+json")
    fun membersOfOrg(@Path("org") org: String): Single<List<GithubMember>>

    @GET("users/{login}")
    @Headers("Accept: application/vnd.github.v3+json")
    fun user(@Path("login") login: String): Single<GithubUser>

    @GET("users/{login}/followers")
    @Headers("Accept: application/vnd.github.v3+json")
    fun followersOfUser(@Path("login") login: String): Single<List<GithubMember>>

    @GET
    fun downloadFile(@Url url: String): Single<ResponseBody>
}