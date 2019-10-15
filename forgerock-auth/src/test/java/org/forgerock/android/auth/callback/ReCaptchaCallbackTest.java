/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.FRListenerFuture;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ReCaptchaCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject(" {\n" +
                "            \"type\": \"ReCaptchaCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"recaptchaSiteKey\",\n" +
                "                    \"value\": \"6Lf3tbYUAAAAAEm78fAOFRKb-n1M67FDtmpczIBK\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        ReCaptchaCallback reCaptchaCallback = new ReCaptchaCallback(raw, 0);

        assertEquals("6Lf3tbYUAAAAAEm78fAOFRKb-n1M67FDtmpczIBK", reCaptchaCallback.getReCaptchaSiteKey());

    }
}