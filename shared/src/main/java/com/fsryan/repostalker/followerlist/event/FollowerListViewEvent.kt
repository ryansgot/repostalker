package com.fsryan.repostalker.followerlist.event

import com.autodsl.annotation.AutoDsl
import com.fsryan.repostalker.data.GithubMember

@AutoDsl
data class FollowerListViewEvent(
    val showEmpty: Boolean,
    val showList: Boolean,
    val showLoading: Boolean,
    val clearList: Boolean,
    val followerDetails: FollowerDetails?,
    val enableFiltering: Boolean,
    val disableFiltering: Boolean,
    val clearFilterText: Boolean,
    val errorMessage: String?
) {
    companion object {

        fun forLoading(clearList: Boolean = false, clearFilterText: Boolean = true) = followerListViewEvent {
            showEmpty = false
            showList = false
            showLoading = true
            this.clearList = clearList
            enableFiltering = false
            disableFiltering = true
            this.clearFilterText = clearFilterText
            followerDetails = null
        }

        fun forShowingNewList(clearFilterText: Boolean = true, firstFollowerDetails: FollowerDetails? = null) = followerListViewEvent {
            showEmpty = firstFollowerDetails == null
            showList = firstFollowerDetails != null
            showLoading = false
            clearList = true
            enableFiltering = firstFollowerDetails != null
            disableFiltering = firstFollowerDetails == null
            followerDetails = firstFollowerDetails
            this.clearFilterText = clearFilterText
        }

        fun forAddingAFollower(followerDetails: FollowerDetails, clearList: Boolean = false) = followerListViewEvent {
            showEmpty = false
            showList = true
            showLoading = false
            this.clearList = clearList
            enableFiltering = true
            disableFiltering = false
            this.followerDetails = followerDetails
            clearFilterText = false
        }

        fun forFinishedAddingFollowers(hasLoadedFollowers: Boolean) = followerListViewEvent {
            showEmpty = !hasLoadedFollowers
            showList = hasLoadedFollowers
            showLoading = false
            clearList = false
            enableFiltering = hasLoadedFollowers
            disableFiltering = !hasLoadedFollowers
            clearFilterText = false
        }

        fun forErrorMessage(hasLoadedFollowers: Boolean, message: String?) = followerListViewEvent {
            showEmpty = !hasLoadedFollowers
            showList = hasLoadedFollowers
            showLoading = false
            clearList = false
            enableFiltering = hasLoadedFollowers
            disableFiltering = !hasLoadedFollowers
            clearFilterText = false
            errorMessage = message
        }
    }

    fun hasNewFollowerDetails() = followerDetails != null
    fun hideEmpty() = !showEmpty
    fun hideList() = !showList
    fun hideLoading() = !showLoading
    fun showErrorMessage() = errorMessage != null
}

@AutoDsl
data class FollowerDetails(val userName: String, val avatarBytes: ByteArray?, val email: String?, val location: String?) {
    fun hasAvatar() = avatarBytes != null && avatarBytes.isNotEmpty()
    fun hasEmail() = email != null
    fun hasLocation() = location != null
}

// TODO: get email and location
fun GithubMember.toFollowerDetails(avatarBytes: ByteArray) = FollowerDetails(login, avatarBytes, null, null)