package com.fsryan.repostalker.data.system

import io.reactivex.Single

interface NetworkIfInfo {
    fun isConnectedToNetwork(): Single<Boolean>
}