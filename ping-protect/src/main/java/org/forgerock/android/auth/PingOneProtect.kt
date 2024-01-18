package org.forgerock.android.auth

import android.content.Context
import com.pingidentity.signalssdk.sdk.GetDataCallback
import com.pingidentity.signalssdk.sdk.InitCallback
import com.pingidentity.signalssdk.sdk.POInitParams
import com.pingidentity.signalssdk.sdk.PingOneSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.forgerock.android.auth.callback.PingOneInitException
import org.forgerock.android.auth.callback.PingOneProtectEvaluationCallback
import org.forgerock.android.auth.callback.PingOneProtectException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PingOneProtect
    @JvmOverloads
    constructor() {
        suspend fun initSDK(
            context: Context,
            params: PIProtectInitParams? = null,
        ) {
            return suspendCancellableCoroutine { init ->
                PIProtectInitParamState.params?.let {
                    if (params?.isBehavioralDataCollection == true) {
                        resumeBehavioralData()
                    }
                    init.resume(Unit)
                } ?: run {
                    PingOneSignals.setInitCallback(
                        object : InitCallback {
                            override fun onInitialized() {
                                Logger.info("PingOneSignals", "PingOneSignals Initialized")
                                PIProtectInitParamState.params = params
                                init.resume(Unit)
                            }

                            override fun onError(
                                p0: String,
                                p1: String,
                                p2: String,
                            ) {
                                Logger.error("PingOneSignals", "PingOneSignals failed $p0 $p1 $p2")
                                PIProtectInitParamState.params = null
                                init.resumeWithException(PingOneInitException("PingOneSignals failed $p0 $p1 $p2"))
                            }
                        },
                    )
                    val params = POInitParams()
                    params.apply {
                        envId = params.envId
                        isBehavioralDataCollection = params.isBehavioralDataCollection
                        isConsoleLogEnabled = params.isConsoleLogEnabled
                        customHost = params.customHost
                        isLazyMetadata = params.isLazyMetadata
                        deviceAttributesToIgnore = params.deviceAttributesToIgnore
                    }
                    PingOneSignals.init(context, params)
                }
            }
        }

        fun initSDK(
            context: Context,
            params: PIProtectInitParams? = null,
            listener: FRListener<Void>,
        ) {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    initSDK(context, params)
                    Listener.onSuccess(listener, null)
                } catch (e: Exception) {
                    Listener.onException(listener, e)
                }
            }
        }

        internal suspend fun getData(callback: PingOneProtectEvaluationCallback): String {
            return suspendCancellableCoroutine {
                PingOneSignals.getData(
                    object : GetDataCallback {
                        override fun onSuccess(result: String) {
                            if (callback.pauseBehavioralData == true) {
                                pauseBehavioralData()
                            }
                            it.resume(result)
                        }

                        override fun onFailure(result: String) {
                            it.resumeWithException(PingOneProtectException(result))
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

// State Pattern
internal object PIProtectInitParamState {
    internal var params: PIProtectInitParams? = null
}

data class PIProtectInitParams(
    val envId: String? = null,
    val deviceAttributesToIgnore: List<String>? = null,
    val customHost: String? = null,
    val isConsoleLogEnabled: Boolean = false,
    val isLazyMetadata: Boolean = false,
    val isBehavioralDataCollection: Boolean = true,
)
