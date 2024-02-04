/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import com.pingidentity.signalssdk.sdk.GetDataCallback
import com.pingidentity.signalssdk.sdk.InitCallback
import com.pingidentity.signalssdk.sdk.POInitParams
import com.pingidentity.signalssdk.sdk.PingOneSignals
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * PIProtect is for initializing and interacting with Ping Protect SDK
 */
class PIProtect {
    companion object {

        const val TAG: String = "PIProtect"

        // Save the init state of the protect.
        internal var protectParamState: POInitParams? = null

        /**
         * Initialize Ping Protect SDK
         *
         * @param context The Application Context
         * @param parameter The `PIInitParams` containing parameters for the init
         */
        @JvmStatic
        suspend fun start(
            context: Context,
            parameter: PIInitParams? = null,
        ) {
            return suspendCancellableCoroutine { init ->
                protectParamState?.let {
                    init.resume(Unit)
                } ?: run {
                    val outputParam = getInitParam(parameter)
                    PingOneSignals.setInitCallback(
                        object : InitCallback {
                            override fun onInitialized() {
                                Logger.info(TAG, "PingOneSignals Initialized")
                                protectParamState = outputParam
                                init.resume(Unit)
                            }
                            override fun onError(
                                p0: String,
                                p1: String,
                                p2: String,
                            ) {
                                Logger.error(TAG, "PingOneSignals failed $p0 $p1 $p2")
                                protectParamState = null
                                init.resumeWithException(PingOneProtectInitException("PingOneSignals failed $p0 $p1 $p2"))
                            }
                        },
                    )
                    PingOneSignals.init(context, outputParam)
                }
            }
        }

        private fun getInitParam(parameter: PIInitParams? = null): POInitParams {
            val init: POInitParams = parameter?.let {
                val params = POInitParams()
                params.apply {
                    envId = parameter.envId
                    isBehavioralDataCollection = parameter.isBehavioralDataCollection
                    isConsoleLogEnabled = parameter.isConsoleLogEnabled
                    customHost = parameter.customHost
                    isLazyMetadata = parameter.isLazyMetadata
                    deviceAttributesToIgnore = parameter.deviceAttributesToIgnore
                }
            } ?: POInitParams()
            return init
        }

        /**
         * Get the signal data from PingSDK
         *
         */
        internal suspend fun getData(): String {
            return suspendCancellableCoroutine {
                PingOneSignals.getData(
                    object : GetDataCallback {
                        override fun onSuccess(result: String) {
                            it.resume(result)
                        }

                        override fun onFailure(result: String) {
                            it.resumeWithException(PingOneProtectEvaluationException(result))
                        }
                    },
                )
            }
        }

        /**
         * Pause behavioral data collection
         */
        fun pauseBehavioralData() {
            PingOneSignals.pauseBehavioralData()
        }

        /**
         * Resume behavioral data collection
         */
        fun resumeBehavioralData() {
            PingOneSignals.resumeBehavioralData()
        }
    }
}

/**
 * Parameters for starting PIProtect SDK
 */
data class PIInitParams(
    val envId: String? = null,
    val deviceAttributesToIgnore: List<String>? = null,
    val customHost: String? = null,
    val isConsoleLogEnabled: Boolean = false,
    val isLazyMetadata: Boolean = false,
    val isBehavioralDataCollection: Boolean = true,
)
