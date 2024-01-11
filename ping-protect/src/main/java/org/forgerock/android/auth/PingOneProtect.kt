package org.forgerock.android.auth

import android.content.Context
import com.pingidentity.signalssdk.sdk.GetDataCallback
import com.pingidentity.signalssdk.sdk.InitCallback
import com.pingidentity.signalssdk.sdk.POInitParams
import com.pingidentity.signalssdk.sdk.PingOneSignals
import kotlinx.coroutines.suspendCancellableCoroutine
import org.forgerock.android.auth.callback.PingOneInitException
import org.forgerock.android.auth.callback.PingOneProtectEvaluationCallback
import org.forgerock.android.auth.callback.PingOneProtectException
import org.forgerock.android.auth.callback.PingOneProtectInitCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PingOneProtect @JvmOverloads constructor( val signalWrapper: SignalWrapper = SignalWrapper()) {
    fun start(context: Context, params: POInitParams) {
        signalWrapper.setInitCallback(
            object : InitCallback {
                override fun onInitialized() {
                    Logger.info("PingOneSignals", "PingOneSignals Initialized")
                    PingOneParamState.params = params
                }

                override fun onError(
                    p0: String,
                    p1: String,
                    p2: String,
                ) {
                    Logger.error("PingOneSignals", "PingOneSignals failed $p0 $p1 $p2 ")
                    PingOneParamState.params = null
                }
            },
        )
        PingOneSignals.init(context, params)
    }

    /**
     * Pause behavioral data collection
     */
     fun pauseBehavioralData() {
        signalWrapper.pauseBehavioralData()
    }

    /**
     * Resume behavioral data collection
     */
     fun resumeBehavioralData() {
        signalWrapper.resumeBehavioralData()
    }

    internal suspend fun setInitCallback(pauseBehaviourData: Boolean) {
        return suspendCancellableCoroutine { init ->
            PingOneParamState.params?.let {
                // need to confirm this ?
                if(pauseBehaviourData) {
                    resumeBehavioralData()
                }
                init.resume(Unit)
            } ?: init.resumeWithException(PingOneInitException("Failed to initialize"))
        }
    }

    internal suspend fun getData(callback: PingOneProtectEvaluationCallback): String {
        return suspendCancellableCoroutine {
            signalWrapper.getData(
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

}

// State Pattern
internal object PingOneParamState {
    internal var params: POInitParams? = null
}
