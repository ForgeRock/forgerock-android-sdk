package com.example.app.callback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.LocalAppContext
import com.google.android.recaptcha.RecaptchaException
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.callback.ReCaptchaEnterpriseCallback

@Composable
fun ReCaptchaEnterpriseCallback(callback: ReCaptchaEnterpriseCallback,
                                onCompleted: () -> Unit) {


    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val application = LocalAppContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {

        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Launching Recaptcha...")
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
            LaunchedEffect(true) {
                try {
                    callback.execute(application = application)
                }
                catch (e: Exception) {
                    if(e is RecaptchaException) {
                        Logger.error("RecaptchaException", "${e.errorCode}:${e.message}")
                    }
                    Logger.error("RecaptchaException", e.message)
                }
                currentOnCompleted()
            }
        }
    }
}
