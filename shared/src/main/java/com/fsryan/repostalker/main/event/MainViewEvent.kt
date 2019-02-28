package com.fsryan.repostalker.main.event

import com.autodsl.annotation.AutoDsl

@AutoDsl
data class MainViewEvent(
    val showSettings: Boolean = false,
    val hideSettings: Boolean = false,
    val cacheInvalidationInterval: Long = 0L,
    val navBack: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {

        fun forShowingSettings(cacheInvalidationInterval: Long) = mainViewEvent {
            showSettings = true
            hideSettings = false
            this.cacheInvalidationInterval = cacheInvalidationInterval
            navBack = false
            errorMessage = null
        }

        /**
         * typically, you'd want some sort of error ID here. An Error ID would
         * allow the View to use string resources to determine which error
         * message to show. However, this project assumes english-only.
         */
        fun forHidingSettings(errorMessage: String? = null) = mainViewEvent {
            showSettings = false
            hideSettings = true
            cacheInvalidationInterval = 0L
            navBack = false
            this.errorMessage = errorMessage
        }

        fun forNavBack() = mainViewEvent {
            showSettings = false
            hideSettings = false
            cacheInvalidationInterval = 0L
            navBack = true
            errorMessage = null
        }
    }

    fun showErrorMessage() = errorMessage != null
}
