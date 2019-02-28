package com.fsryan.repostalker.testonly

import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.ResponseBody

class RetrofitHelper {
    companion object {
        val byteArray = ByteArray(0)
        fun fakeBinaryResp(): ResponseBody = ResponseBody.create(MediaType.get("application/octet-stream"), byteArray)
        fun fakeBinaryRespSingle(): Single<ResponseBody> = Single.just(fakeBinaryResp())
    }
}