/*
 * Copyright (c) 2020 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.KbaCreateCallback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.StringAttributeInputCallback;
import org.forgerock.android.auth.callback.TermsAndConditionsCallback;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class AndroidBaseTest {

    @Rule
    public Timeout timeout = new Timeout(10000, TimeUnit.MILLISECONDS);
    protected static Context context = ApplicationProvider.getApplicationContext();
    public static String USERNAME = "sdkuser";
    public static String PASSWORD = "password";
    public static String USER_EMAIL = "sdkuser@example.com";

    protected String TREE = "UsernamePassword";

    @Before
    public void setUpSDK() {
        Logger.set(Logger.Level.DEBUG);
        FRAuth.start(context);
    }

    /**
     * Register a random user
     *
     * @return The username of the registered user
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static String registerRandomUser() throws ExecutionException, InterruptedException {
        String randomUsername = "user" + System.currentTimeMillis();

        NodeListenerFuture<FRSession> nodeListenerFuture = getNodeListenerFuture(randomUsername);

        FRSession.authenticate(context, "TEST_USER_REGISTRATION", nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

        return randomUsername;
    }

    protected static NodeListenerFuture<FRSession> getNodeListenerFuture(String userName) {
        return new UsernamePasswordNodeListener(context) {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(NameCallback.class) != null) {
                    node.getCallback(NameCallback.class).setName(userName);
                    node.next(context, this);
                }
                if (node.getCallback(PasswordCallback.class) != null) {
                    PasswordCallback callback = node.getCallback(PasswordCallback.class);
                    assertThat(callback.getPrompt()).isEqualTo("Password");
                    callback.setPassword(userName.toCharArray());
                    node.next(context, this );
                }
                if (node.getCallback(StringAttributeInputCallback.class) != null) {
                    List<Callback> callbacks = node.getCallbacks();

                    StringAttributeInputCallback givenName = (StringAttributeInputCallback) callbacks.get(0);
                    StringAttributeInputCallback sn = (StringAttributeInputCallback) callbacks.get(1);
                    StringAttributeInputCallback mail = (StringAttributeInputCallback) callbacks.get(2);

                    givenName.setValue(userName);
                    sn.setValue(userName);
                    mail.setValue(userName +  "@example.com");

                    node.next(context, this );
                }
                if (node.getCallback(KbaCreateCallback.class) != null) {
                    List<Callback> callbacks = node.getCallbacks();

                    KbaCreateCallback firstQuestion = (KbaCreateCallback) callbacks.get(0);
                    firstQuestion.setSelectedQuestion(firstQuestion.getPredefinedQuestions().get(0));
                    firstQuestion.setSelectedAnswer("Test");

                    // Uncomment this block if there are more than one KbaCreateCallbacks in the tree
                    /*
                    KbaCreateCallback secondQuestion = (KbaCreateCallback) callbacks.get(1);
                    secondQuestion.setSelectedQuestion(secondQuestion.getPredefinedQuestions().get(1));
                    secondQuestion.setSelectedAnswer("Test");
                    */

                    node.next(context, this );
                }

                if (node.getCallback(TermsAndConditionsCallback.class) != null) {
                    TermsAndConditionsCallback callback = node.getCallback(TermsAndConditionsCallback.class);
                    callback.setAccept(true);

                    node.next(context, this );
                }
            }
        };
    }
}
