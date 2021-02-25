/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;

import org.forgerock.android.auth.BaseTest;
import org.forgerock.android.auth.DummyTask;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RunWith(RobolectricTestRunner.class)
public class WebAuthnRegistrationTest extends BaseTest {

    private WebAuthnRegistration webAuthnRegistration;

    @Captor
    ArgumentCaptor<PublicKeyCredentialCreationOptions> optionsArgumentCaptor;

    @Test
    public void testParsingParameter71() throws Exception {
        JSONObject value = new JSONObject(getJson("/webAuthn_registration_71.json"))
                .getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("output")
                .getJSONObject(0)
                .getJSONObject("value");
        testParsingParameter(value);
    }

    private void testParsingParameter(JSONObject value) throws Exception {

       webAuthnRegistration = Mockito.spy(new WebAuthnRegistration(value));
        Mockito.doReturn(new DummyTask()).when(webAuthnRegistration).getRegisterPendingIntent(any(), optionsArgumentCaptor.capture());
        webAuthnRegistration.register(context, null, null );

        PublicKeyCredentialCreationOptions options = optionsArgumentCaptor.getValue();
        assertThat(options.getAttestationConveyancePreference().name()).isEqualTo("NONE");
        assertThat(options.getAuthenticationExtensions()).isNull();
        //Google not allow to set the residentKey
        assertThat(options.getAuthenticatorSelection().getRequireResidentKey()).isNull();
        assertThat(webAuthnRegistration.isRequireResidentKey(value)).isTrue();
        assertThat(options.getAuthenticatorSelection().getAttachment().toString()).isEqualTo("platform");
        String challenge = "X5OsmgG2We2Xgir575Grt19hwXoC9m7Jth6UxWOrEYE=";
        assertThat(options.getChallenge()).isEqualTo(Base64.getDecoder().decode(challenge));
        assertThat(options.getTimeoutSeconds()).isEqualTo(60.0D);
        assertThat(options.getExcludeList()).hasSize(3);
        assertThat(options.getParameters()).hasSize(2);
        assertThat(options.getParameters().get(0).getType()).isEqualTo(PublicKeyCredentialType.PUBLIC_KEY);
        assertThat(options.getParameters().get(0).getAlgorithmIdAsInteger()).isEqualTo(-7);
        assertThat(options.getParameters().get(1).getType()).isEqualTo(PublicKeyCredentialType.PUBLIC_KEY);
        assertThat(options.getParameters().get(1).getAlgorithmIdAsInteger()).isEqualTo(-257);
        assertThat(options.getRp().getName()).isEqualTo("ForgeRock");
        assertThat(options.getRp().getIcon()).isNull();
        assertThat(options.getRp().getId()).isEqualTo("humorous-cuddly-carrot.glitch.me");
        assertThat(options.getUser().getDisplayName()).isEqualTo("e24f0d7c-a9d5-4a3f-a002-6f808210a8a3");
        assertThat(options.getUser().getName()).isEqualTo("e24f0d7c-a9d5-4a3f-a002-6f808210a8a3");
        assertThat(options.getUser().getIcon()).isNull();
        String id = "WlRJMFpqQmtOMk10WVRsa05TMDBZVE5tTFdFd01ESXRObVk0TURneU1UQmhPR0V6";
        assertThat(options.getUser().getId()).isEqualTo(Base64.getDecoder().decode(id));
        assertThat(options.getTokenBinding()).isNull();
        assertThat(options.getRequestId()).isNull();

    }

}
