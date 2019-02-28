package com.fsryan.repostalker.current.event

import com.autodsl.annotation.AutoDsl
import com.fsryan.repostalker.data.GithubUser

@AutoDsl
data class CurrentViewEvent(val showEmpty: Boolean, val userDetails: UserDetails?) {
    companion object {
        fun forUserDetails(userDetails: UserDetails) = currentViewEvent {
            showEmpty = false
            this.userDetails = userDetails
        }

        fun empty() = currentViewEvent {
            showEmpty = true
            userDetails = null
        }

        fun forDataLoading() = currentViewEvent {
            showEmpty = false
            userDetails = null
        }
    }

    fun showUserDetails() = userDetails != null
    fun hideEmpty() = !showEmpty
    fun hideUserDetails() = showEmpty
    fun showLoading() = !showUserDetails() && !showEmpty
    fun hideLoading() = !showLoading()
}

@AutoDsl
data class UserDetails(val userName: String, val avatarBytes: ByteArray?, val email: String?, val location: String?) {
    fun hasAvatar() = avatarBytes != null
    fun hasEmail() = email != null
    fun hasLocation() = location != null
}

fun GithubUser.toUserDetails(avatarBytes: ByteArray) = UserDetails(login, avatarBytes, email, location)