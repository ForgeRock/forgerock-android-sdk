package org.forgerock.auth;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.Action;
import org.forgerock.android.auth.CookieInterceptor;
import org.forgerock.android.auth.FRRequestInterceptor;
import org.forgerock.android.auth.Request;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;

public class CustomCookieInterceptor implements FRRequestInterceptor<Action>, CookieInterceptor {
    @NonNull
    @Override
    public Request intercept(@NonNull Request request) {
       return request;
    }

    @NonNull
    @Override
    public Request intercept(@NonNull Request request, Action tag) {
        return request;
    }

    @NonNull
    @Override
    public List<Cookie> intercept(@NonNull List<Cookie> cookies) {
        List<Cookie> newCookies = new ArrayList<>();
        newCookies.addAll(cookies);
        newCookies.add(new Cookie.Builder().domain("localhost").name("test").value("testValue").httpOnly().secure().build());

        return newCookies;
    }
}
