/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import okhttp3.Cookie;

import static org.assertj.core.api.Assertions.assertThat;

public class SetPersistentCookieTest extends TreeTest {

    @Override
    protected String getTreeName() {
        return "SetPersistentCookieTest";
    }

    @Override
    protected NodeListenerFuture<FRSession> getNodeListenerFuture() {
        return new UsernamePasswordNodeListener(context);
    }

    @Override
    public void testTree() throws ExecutionException, InterruptedException {
        super.testTree();
        Collection<String> cookies = Config.getInstance().getSingleSignOnManager().getCookies();
        //Assert that session-jwt is set
        /* Does not support stream
        assertThat(cookies.stream().anyMatch(s -> {
            Cookie cookie1 = Cookie.parse(HttpUrl.parse(Config.getInstance().getUrl()), s);
            return cookie1.name().equals("session-jwt") && cookie1.httpOnly() && cookie1.secure();
        })).isTrue();
         */

        boolean found = false;
        for (String cookie : cookies) {
            //Cookie cookie1 = Cookie.parse(HttpUrl.parse(Config.getInstance().getUrl()), cookie);
            Cookie cookie1 = new CookieMarshaller().unmarshal(cookie);
            if (cookie1.name().equals("session-jwt") && cookie1.httpOnly() && cookie1.secure()) {
                found = true;
            }
        }
        assertThat(found).isTrue();


        FRSession.getCurrentSession().logout();
        cookies = Config.getInstance().getSingleSignOnManager().getCookies();
        assertThat(cookies).isEmpty();

    }
}
