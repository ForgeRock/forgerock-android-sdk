package com.example.app.setting

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pingidentity.sdk.pingoneverify.PingOneVerifyClient
import com.pingidentity.sdk.pingoneverify.PingOneVerifyClient.Builder.BuilderCallback
import com.pingidentity.sdk.pingoneverify.errors.DocumentSubmissionError
import com.pingidentity.sdk.pingoneverify.listeners.DocumentSubmissionListener
import com.pingidentity.sdk.pingoneverify.models.DocumentSubmissionResponse
import com.pingidentity.sdk.pingoneverify.models.DocumentSubmissionStatus
import com.pingidentity.sdk.pingoneverify.settings.ButtonAppearance
import com.pingidentity.sdk.pingoneverify.settings.UIAppearanceSettings

class PingVerifyViewModel(context: Context) : ViewModel() {

    fun verifyQRCode(qrCode: String, activity: FragmentActivity) {
        PingOneVerifyClient.Builder(false)
            .setRootActivity(activity)
            .setUIAppearance(getUiAppearanceSettings())
            .setListener(object: DocumentSubmissionListener {
                override fun onDocumentSubmitted(response: DocumentSubmissionResponse?) {
                    Log.d("onDocumentSubmitted Jey--->", response.toString())
                }

                override fun onSubmissionComplete(status: DocumentSubmissionStatus?) {
                    Log.d("onSubmissionComplete Jey--->", status.toString())
                }

                override fun onSubmissionError(error: DocumentSubmissionError?) {
                    Log.e("onSubmissionError Jey--->", error?.message.toString())
                }
            })
            .setQrString(qrCode)
            .startVerification(object : BuilderCallback {
                override fun onSuccess(client: PingOneVerifyClient) {
                    Log.d("initPingOneClient Jey", "success")
                }

                override fun onError(errorMessage: String) {
                    Log.e("initPingOneClient Failed Jey", errorMessage)
                }
            })
    }

    private fun getUiAppearanceSettings(): UIAppearanceSettings? {
        return UIAppearanceSettings()
            .setBackgroundColor("#FFFFFF")
            .setSolidButtonAppearance(ButtonAppearance("#F1C40F", "#F1C40F", "#95A5A6"))
            .setBorderedButtonAppearance(ButtonAppearance("#00FFFFFF", "#28B463", "#28B463"))
    }

    companion object {
        fun factory(
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PingVerifyViewModel(context.applicationContext) as T
            }
        }
    }

}
