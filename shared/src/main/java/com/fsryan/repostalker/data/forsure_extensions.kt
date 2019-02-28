package com.fsryan.repostalker.data

import com.fsryan.forsuredb.api.BaseDocStoreSetter
import com.fsryan.forsuredb.api.RecordContainer
import com.fsryan.forsuredb.api.Retriever
import io.reactivex.Observable
import io.reactivex.Single

// renames object function (not a keyword in java) to obj
fun <U, R : RecordContainer, T, S : BaseDocStoreSetter<U, R, T>> BaseDocStoreSetter<U, R, T>.obj(obj: T): S =
    this.`object`(obj) as S


fun <T> Retriever.flattenAsObservable(flatteningFunction: (Retriever) -> T): Observable<T> {
    moveToPosition(-1)
    return Single.just(this).flattenAsObservable { retriever ->
        object : Iterable<T> {
            override fun iterator(): Iterator<T> {
                return object : Iterator<T> {
                    override fun hasNext(): Boolean {
                        return !retriever.isClosed && !retriever.isLast && !retriever.isAfterLast
                    }

                    override fun next(): T {
                        retriever.moveToNext()
                        return flatteningFunction(retriever)
                    }
                }
            }
        }
    }
}