package com.fsryan.testtools

import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import okio.Buffer
import java.lang.reflect.Type
import com.squareup.moshi.Types

class Fixtures(private val moshi: Moshi) {

    fun <T> get(resource: String, type: Type): T {
        val inStream = Fixtures::class.java.classLoader.getResourceAsStream(resource)
        val buf = Buffer()
        buf.readFrom(inStream)
        val reader = JsonReader.of(buf)
        reader.use { json ->
            return moshi.adapter<T>(type).fromJson(json)!!
        }
    }

    fun <T> getList(resource: String, type: Type): List<T> {
        val inStream = Fixtures::class.java.classLoader.getResourceAsStream(resource)
        val buf = Buffer()
        buf.readFrom(inStream)
        val reader = JsonReader.of(buf)
        reader.use { json ->
            return moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, type)).fromJson(json)!!
        }
    }
}
