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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.LocalAppContext
import com.google.android.recaptcha.RecaptchaException
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.callback.ReCaptchaEnterpriseCallback

@Composable
fun ReCaptchaEnterpriseCallback(callback: ReCaptchaEnterpriseCallback) {

    val application = LocalAppContext.current
    var text by remember {
        mutableStateOf("Launching Recaptcha...")
    }

    var showProgress by remember {
        mutableStateOf(true)
    }

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
            Text(text = text)
            Spacer(Modifier.height(8.dp))
            if(showProgress) {
                CircularProgressIndicator()
            }
            LaunchedEffect(true) {
                try {
                    callback.execute(application = application)
                    text = "Recaptcha Validation Completed..."
                    showProgress = false
                }
                catch (e: Exception) {
                    text = "Recaptcha Validation Failed..."
                    showProgress = false
                    if(e is RecaptchaException) {
                        Logger.error("RecaptchaException", "${e.errorCode}:${e.message}")
                    }
                    Logger.error("RecaptchaException", e.message)
                }
            }
        }
    }
}
