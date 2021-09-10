/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.fido.common.Transport;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions;
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType;

import org.forgerock.android.auth.BaseTest;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.MockEncryptor;
import org.forgerock.android.auth.DummyTask;
import org.forgerock.android.auth.WebAuthnDataRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RunWith(RobolectricTestRunner.class)
public class WebAuthnAuthenticationTest extends BaseTest {

    private WebAuthnAuthentication webAuthnAuthentication;

    @Captor
    ArgumentCaptor<PublicKeyCredentialRequestOptions> optionsArgumentCaptor;

    @Captor
    ArgumentCaptor<List<PublicKeyCredentialDescriptor>> allowCredentialsArgumentCaptor;

    @After
    public void cleanupAccount() throws Exception {

        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("org.forgerock");
        for (Account acc : accounts) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccountExplicitly(acc);
            } else {
                AccountManagerFuture<Boolean> future = accountManager.removeAccount(acc, null, null);
                future.getResult();
            }
        }
    }

    @Test
    public void testParsingParameterWithUsernameLess71() throws Exception {
        testParsingParameterWithUsernameLess(get71Callback());
    }

    private void testParsingParameterWithUsernameLess(JSONObject input) throws Exception {
        webAuthnAuthentication = Mockito.spy(new WebAuthnAuthentication(input));
        Mockito.doReturn(new DummyTask()).when(webAuthnAuthentication).getSignPendingIntent(any(), optionsArgumentCaptor.capture());
        webAuthnAuthentication.authenticate(context, null, null, null);

        PublicKeyCredentialRequestOptions options = optionsArgumentCaptor.getValue();
        assertThat(options.getAllowList()).isEmpty();
        assertThat(options.getRpId()).isEqualTo("humorous-cuddly-carrot.glitch.me");//TODO
        assertThat(options.getAuthenticationExtensions()).isNull();
        String challenge = "qnMsxgya8h6mUc6OyRu8jJ6Oq16tHV3cgE7juXGMDbg=";
        assertThat(options.getChallenge()).isEqualTo(Base64.getDecoder().decode(challenge));
        assertThat(options.getTimeoutSeconds()).isEqualTo(60.0D);
        assertThat(options.getTokenBinding()).isNull();
        assertThat(options.getRequestId()).isNull();
    }

    private JSONObject get71Callback() throws JSONException {
        return new JSONObject(getJson("/webAuthn_authentication_71.json"))
                .getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("output")
                .getJSONObject(0)
                .getJSONObject("value");
    }

    private JSONObject get71WithUserCallback() throws JSONException {
        return new JSONObject(getJson("/webAuthn_authentication_with_user_71.json"))
                .getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("output")
                .getJSONObject(0)
                .getJSONObject("value");
    }

    @Test
    public void testParsingParameterWithUsername71() throws Exception {
        testParsingParameterWithUsername(get71WithUserCallback());
    }

    private void testParsingParameterWithUsername(JSONObject value) throws Exception {

        webAuthnAuthentication = Mockito.spy(new WebAuthnAuthentication(value));
        Mockito.doReturn(new DummyTask()).when(webAuthnAuthentication).getSignPendingIntent(any(), optionsArgumentCaptor.capture());
        webAuthnAuthentication.authenticate(context, null, null, null);

        PublicKeyCredentialRequestOptions options = optionsArgumentCaptor.getValue();
        assertThat(options.getAllowList()).hasSize(2);
        assertThat(options.getAllowList().get(0).getType()).isEqualTo(PublicKeyCredentialType.PUBLIC_KEY);
        assertThat(options.getAllowList().get(0).getId()).isNotNull();
        assertThat(options.getAllowList().get(1).getType()).isEqualTo(PublicKeyCredentialType.PUBLIC_KEY);
        assertThat(options.getAllowList().get(1).getId()).isNotNull();
        assertThat(options.getRpId()).isEqualTo("humorous-cuddly-carrot.glitch.me");//TODO
        assertThat(options.getAuthenticationExtensions()).isNull();
        String challenge = "qnMsxgya8h6mUc6OyRu8jJ6Oq16tHV3cgE7juXGMDbg=";
        assertThat(options.getChallenge()).isEqualTo(Base64.getDecoder().decode(challenge));
        assertThat(options.getTimeoutSeconds()).isEqualTo(60.0D);
        assertThat(options.getTokenBinding()).isNull();
        assertThat(options.getRequestId()).isNull();

    }

    @Test
    public void testUsernameLessWith1CredentialSource() throws Exception {

        JSONObject value = new JSONObject(getJson("/webAuthn_authentication_71.json"))
                .getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("output")
                .getJSONObject(0)
                .getJSONObject("value");

        WebAuthnDataRepository repository = WebAuthnDataRepository.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        PublicKeyCredentialSource source = PublicKeyCredentialSource.builder()
                .id("keyHandle".getBytes())
                .otherUI("test")
                .rpid("humorous-cuddly-carrot.glitch.me")
                .userHandle("test".getBytes())
                .type("public-key")
                .build();

        repository.persist(source);

        webAuthnAuthentication = Mockito.spy(new WebAuthnAuthentication(value) {
            @Override
            protected List<PublicKeyCredentialSource> getPublicKeyCredentialSource(Context context) {
                return repository.getPublicKeyCredentialSource(relayingPartyId);
            }
        });
        Mockito.doNothing().when(webAuthnAuthentication).authenticate(any(),
                any(), any(), allowCredentialsArgumentCaptor.capture(), any());


        webAuthnAuthentication.authenticate(context, null, null,
                null);

        List<PublicKeyCredentialDescriptor> descriptors = allowCredentialsArgumentCaptor.getValue();
        assertThat(descriptors).hasSize(1);
        assertThat(descriptors.get(0).getId()).isEqualTo("keyHandle".getBytes());
        assertThat(descriptors.get(0).getTypeAsString()).isEqualTo("public-key");
        assertThat(descriptors.get(0).getTransports().get(0)).isEqualTo(Transport.INTERNAL);

    }

    @Test
    public void testUsernameLessWithMoreThan1CredentialSource() throws Exception {

        JSONObject value = new JSONObject(getJson("/webAuthn_authentication_71.json"))
                .getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("output")
                .getJSONObject(0)
                .getJSONObject("value");


        WebAuthnDataRepository repository = WebAuthnDataRepository.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        PublicKeyCredentialSource source = PublicKeyCredentialSource.builder()
                .id("keyHandle".getBytes())
                .otherUI("test")
                .rpid("humorous-cuddly-carrot.glitch.me")
                .userHandle("test".getBytes())
                .type("public-key")
                .build();
        repository.persist(source);

        PublicKeyCredentialSource source2 = PublicKeyCredentialSource.builder()
                .id("keyHandle2".getBytes())
                .otherUI("test2")
                .rpid("humorous-cuddly-carrot.glitch.me")
                .userHandle("test2".getBytes())
                .type("public-key")
                .build();
        repository.persist(source2);
        webAuthnAuthentication = Mockito.spy(new WebAuthnAuthentication(value) {
            @Override
            protected List<PublicKeyCredentialSource> getPublicKeyCredentialSource(Context context) {
                return repository.getPublicKeyCredentialSource(relayingPartyId);

            }
        });
        Mockito.doNothing().when(webAuthnAuthentication).authenticate(any(),
                any(), any(), allowCredentialsArgumentCaptor.capture(), any());

        webAuthnAuthentication.authenticate(context, null, new WebAuthnKeySelector() {
                    @Override
                    public void select(@NonNull FragmentManager fragmentManager, @NonNull List<PublicKeyCredentialSource> sourceList, @NonNull FRListener<PublicKeyCredentialSource> listener) {
                        //Select the second one form the list
                        listener.onSuccess(sourceList.get(1));
                    }
                },
                null);

        List<PublicKeyCredentialDescriptor> descriptors = allowCredentialsArgumentCaptor.getValue();
        assertThat(descriptors).hasSize(1);
        assertThat(descriptors.get(0).getId()).isEqualTo("keyHandle2".getBytes());
        assertThat(descriptors.get(0).getTypeAsString()).isEqualTo("public-key");
        assertThat(descriptors.get(0).getTransports().get(0)).isEqualTo(Transport.INTERNAL);

    }


}
