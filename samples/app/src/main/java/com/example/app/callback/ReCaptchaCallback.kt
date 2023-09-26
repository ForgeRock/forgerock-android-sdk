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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.callback.ReCaptchaCallback

@Composable
fun ReCaptchaCallback(callback: ReCaptchaCallback, node: Node,
                      onCompleted: () -> Unit) {


    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val context = LocalContext.current

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
                callback.proceed(context, object : FRListener<Void?> {
                    override fun onSuccess(result: Void?) {
                        currentOnCompleted()
                    }

                    override fun onException(e: Exception) {
                        currentOnCompleted()
                    }
                })
            }
        }
    }
}
