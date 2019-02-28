package com.fsryan.repostalker.interactor

import io.reactivex.Completable
import io.reactivex.Single

interface NavInteractor {
    fun requestBackNav(): Single<String>
    fun pushAndRegisterBackNavInterest(id: String): Completable
    fun registerBackNavInterest(id: String, exactMatch: Boolean = false): Completable
}