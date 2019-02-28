package com.fsryan.repostalker.data

import com.fsryan.forsuredb.api.adapter.FSSerializer
import com.fsryan.forsuredb.api.adapter.FSSerializerFactory
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.lang.UnsupportedOperationException
import java.lang.reflect.Type
import java.util.*

// these come from https://api.github.com/users/{login}
@JsonClass(generateAdapter = true)
data class GithubUser(
    val login: String,
    val id: Long,
    @Json(name = "node_id") val nodeId: String,
    @Json(name = "avatar_url") val avatarUrl: String,
    @Json(name = "gravatar_id") val gravatarId: String,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "followers_url") val followersUrl: String,
    @Json(name = "following_url") val followingUrl: String,
    @Json(name = "gists_url") val gistsUrl: String,
    @Json(name = "starred_url") val starredUrl: String,
    @Json(name = "subscriptions_url") val subscriptionsUrl: String,
    @Json(name = "organizations_url") val organizationsUrl: String,
    @Json(name = "repos_url") val reposUrl: String,
    @Json(name = "received_events_url") val receivedEventsUrl: String,
    @Json(name = "type") val type: String,
    @Json(name = "site_admin") val isSiteAdmin: Boolean,
    val name: String?,
    val company: String?,
    val blog: String?,
    val location: String?,
    val email: String?,
//    val hireable: Any?,
    val bio: String?,
    @Json(name = "public_repos") val publicRepoCount: Int?,
    @Json(name = "public_gists") val publicGists: Int?,
    val followers: Int?,
    val following: Int?,
    @Json(name = "created_at") val createdAt: Date?,
    @Json(name = "updated_at") val updatedAt: Date?) {

    fun isOrganization() = "Organization" == type
}

// These come from https://api.github.com/orgs/{org}/members
// They also come from https://api.github.com/users/{login}/followers
@JsonClass(generateAdapter = true)
data class GithubMember(
    val login: String,
    val id: Long,
    @Json(name = "node_id") val nodeId: String,
    @Json(name = "avatar_url") val avatarUrl: String,
    @Json(name = "gravatar_id") val gravatarId: String,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "followers_url") val followersUrl: String,
    @Json(name = "following_url") val followingUrl: String,
    @Json(name = "gists_url") val gistsUrl: String,
    @Json(name = "starred_url") val starredUrl: String,
    @Json(name = "subscriptions_url") val subscriptionsUrl: String,
    @Json(name = "organizations_url") val organizationsUrl: String,
    @Json(name = "repos_url") val reposUrl: String,
    @Json(name = "received_events_url") val receivedEventsUrl: String,
    @Json(name = "type") val type: String,
    @Json(name = "site_admin") val isSiteAdmin: Boolean
)

class FSJsonSerializerFactory : FSSerializerFactory {
    companion object {
        private val inst = FSSerializerImpl()
    }

    override fun create(): FSSerializer = inst
}

class AdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? = when (type) {
        GithubMember::class.java -> GithubMemberJsonAdapter(moshi)
        GithubUser::class.java -> GithubUserJsonAdapter(moshi)
        else -> moshi.nextAdapter<Any>(this, type, annotations)
    }
}

class FSSerializerImpl : FSSerializer {

    companion object {
        private val moshi = Moshi.Builder()
            .add(AdapterFactory())
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .build()
    }

    // tell forsuredb to store document as a string instead of as a byte array
    override fun storeAsBlob() = false
    // tell forsuredb how to create a string from an object to be stored in the database
    override fun createStringDoc(type: Type, value: Any): String = moshi.adapter<Any>(type).toJson(value)
    // tell forsuredb how to create an object from a string stored in the database
    override fun <T : Any?> fromStorage(type: Type, json: String) = moshi.adapter<Any>(type).fromJson(json) as T?
    // the below will not be called by the framework
    override fun createBlobDoc(type: Type?, value: Any?): ByteArray = throw UnsupportedOperationException("Serialize to string only")
    override fun <T : Any?> fromStorage(type: Type?, objectBytes: ByteArray?): T = throw UnsupportedOperationException("Serialize to string only")
}