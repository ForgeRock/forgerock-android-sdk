package com.example.app.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingVerifyRoute(viewModel: PingVerifyViewModel) {

    var qrCodeValue by rememberSaveable { mutableStateOf("https://apps.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/verify/verify-webapp/v2/index.html?txnid=45826867-646d-4b31-aa1b-e1808d78120a&url=https%3A%2F%2Fapi.pingone.ca%2Fv1%2FidValidations%2FwebVerifications&code=696754&envId=02fb4743-189a-4bc7-9d6c-a919edfe6447") }

    val activity = LocalContext.current as FragmentActivity

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = qrCodeValue,
            onValueChange = { value -> qrCodeValue = value },
            label = { Text("QRCode") },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(modifier = Modifier.align(Alignment.End),
            onClick = {
                viewModel.verifyQRCode(qrCodeValue, activity)
            }) {
            Text("Submit")
        }
    }

}