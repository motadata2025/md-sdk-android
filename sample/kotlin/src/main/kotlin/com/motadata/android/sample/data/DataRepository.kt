/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.motadata.android.sample.data

import com.motadata.android.sample.data.db.LocalDataSource
import com.motadata.android.sample.data.model.Log
import com.motadata.android.sample.data.remote.RemoteDataSource
import com.motadata.android.sample.datalist.DataSourceType
import com.motadata.android.trace.GlobalMotadataTracer
import com.motadata.android.trace.api.scope.MotadataScope
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

internal class DataRepository(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource
) {

    @Suppress("SimpleRedundantLet")
    fun getLogs(query: String): Flowable<List<Log>> {
        var spanScope: MotadataScope? = null
        return Single.concat(
            localDataSource.fetchLogs(),
            remoteDataSource.getLogs(query)
                .map { it.data }
                .doOnSuccess {
                    localDataSource.persistLogs(it)
                }
                .doOnSubscribe {
                    val tracer = GlobalMotadataTracer.get()
                    val span = tracer
                        .buildSpan("Fetch recent logs")
                        .start()
                    spanScope = tracer.activateSpan(span)
                }
                .doFinally {
                    GlobalMotadataTracer.get().activeSpan()?.let {
                        it.finish()
                    }
                    spanScope?.close()
                }
        )
            .subscribeOn(Schedulers.io())
    }

    fun getDataSource(): DataSourceType {
        return localDataSource.getType()
    }

    fun setDataSource(type: DataSourceType) {
        localDataSource.setType(type)
    }
}
