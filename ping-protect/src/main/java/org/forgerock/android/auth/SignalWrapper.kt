package org.forgerock.android.auth

import android.content.Context
import com.pingidentity.signalssdk.sdk.GetDataCallback
import com.pingidentity.signalssdk.sdk.InitCallback
import com.pingidentity.signalssdk.sdk.POInitParams
import com.pingidentity.signalssdk.sdk.PingOneSignals
import com.pingidentity.signalssdk.sdk.TokenReadyListener

class SignalWrapper {
    /**
     * Initialize PingOneSignals SDK by passing in a context and init parameters.
     *
     * @param context    Android context used for initialization
     * @param initParams initialization configuration parameters
     */
    internal fun init(
        context: Context,
        initParams: POInitParams?,
    ) {
        PingOneSignals.init(context, initParams)
    }

    /**
     * Pause behavioral data collection
     */
    internal fun pauseBehavioralData() {
        PingOneSignals.pauseBehavioralData()
    }

    /**
     * Resume behavioral data collection
     */
    internal fun resumeBehavioralData() {
        PingOneSignals.resumeBehavioralData()
    }

    /**
     * @return unique identifier for the entire lifetime of the SDK
     */
    internal fun getInstanceUUID(): String {
        return PingOneSignals.getInstanceUUID()
    }

    /**
     * Set an event listener in order to get calls upon successful SDK
     * initialization or whenever something went wrong in the SDK
     *
     * @param initCallback callback for SDK reports
     */
    internal fun setInitCallback(initCallback: InitCallback) {
        PingOneSignals.setInitCallback(initCallback)
    }

    /**
     * Remove the event listener that was set via [PingOneSignals.setInitCallback]
     */
    internal fun removeInitCallback() {
        PingOneSignals.removeInitCallback()
    }

    internal fun setTokenReadyListener(listener: TokenReadyListener?) {
        PingOneSignals.setTokenReadyListener(listener)
    }

    internal fun getToken(): String {
        return PingOneSignals.getToken()
    }

    internal fun getData(callback: GetDataCallback) {
        PingOneSignals.getData(callback)
    }
}
