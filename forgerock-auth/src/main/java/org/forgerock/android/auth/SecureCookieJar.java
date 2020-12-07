/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Builder;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class SecureCookieJar implements CookieJar {

    private SingleSignOnManager singleSignOnManager;
    private AtomicReference<Set<Cookie>> cacheRef = new AtomicReference<>();
    private final long cacheIntervalMillis;
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();
    private final CookieMarshaller cookieMarshaller = new CookieMarshaller();

    @Builder
    public SecureCookieJar(Context context, SingleSignOnManager singleSignOnManager, Long cacheIntervalMillis) {
        this.singleSignOnManager = singleSignOnManager == null ?
                Config.getInstance().getSingleSignOnManager() : singleSignOnManager;
        this.cacheIntervalMillis = cacheIntervalMillis == null ?
                context.getResources().getInteger(R.integer.forgerock_cookie_cache) * 1000 : cacheIntervalMillis;
    }

    @NotNull
    @Override
    public synchronized List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {

        Set<Cookie> cookies = cacheRef.get();
        if (cookies == null) {
            cookies = new HashSet<>();
            Collection<String> storedCookies = singleSignOnManager.getCookies();
            if (!storedCookies.isEmpty()) {
                Set<String> updatedCookies = new HashSet<>(storedCookies);
                Iterator<String> iterator = updatedCookies.iterator();
                while (iterator.hasNext()) {
                    Cookie cookie = cookieMarshaller.unmarshal(iterator.next());
                    if (cookie != null) {
                        if (!isExpired(cookie)) {
                            cookies.add(cookie);
                        } else {
                            //Remove expired cookies
                            iterator.remove();
                        }
                    } else {
                        //Failed to parse it
                        iterator.remove();
                    }
                }

                // Some cookies are expired, or failed to parse, remove it
                if (storedCookies.size() != updatedCookies.size()) {
                    cache(cookies);
                    singleSignOnManager.persist(updatedCookies);
                }
            }
        }

        return filter(httpUrl, cookies);
    }

    @Override
    public synchronized void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {

        Set<Cookie> cookies = new HashSet<>();
        for (String c : singleSignOnManager.getCookies()) {
            Cookie cookie = cookieMarshaller.unmarshal(c);
            //Remove the same stored cookies
            if (cookie != null && !contains(cookie, list)) {
                cookies.add(cookie);
            }
        }

        for (Cookie cookie : list) {
            if (!isExpired(cookie)) {
                cookies.add(cookie);
            }
        }

        cache(cookies);

        Set<String> updatedCookies = new HashSet<>();
        for (Cookie cookie : cookies) {
            updatedCookies.add(cookieMarshaller.marshal(cookie));
        }

        singleSignOnManager.persist(updatedCookies);
    }

    private boolean contains(Cookie input, Collection<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            if (cookie.name().equals(input.name()) &&
                    cookie.domain().equals(input.domain()) &&
                    cookie.path().equals(input.path())) {
                return true;
            }
        }
        return false;
    }

    private List<Cookie> filter(HttpUrl httpUrl, Set<Cookie> cookies) {
        List<Cookie> result = new ArrayList<>();
        for (Cookie cookie : cookies) {
            if (!isExpired(cookie) &&
                    cookie.matches(httpUrl)) {
                result.add(cookie);
            }
        }
        return result;
    }

    private boolean isExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    private void cache(Set<Cookie> cookies) {
        if (cacheIntervalMillis > 0) {
            cacheRef.set(cookies);
            worker.schedule(() -> cacheRef.set(null), cacheIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

}
