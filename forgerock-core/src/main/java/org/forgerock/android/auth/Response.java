/*
 * This is WestJet source code and is for consideration as a pull request to ForgeRock.
 *
 * This fork was necessary to integrate with the F5® Distributed Cloud Defense Mobile SDK,
 * which protects API endpoints from automation attacks by collecting telemetry and adding
 * custom HTTP headers to requests. The response handling capability was built into the
 * ForgeRock SDK to ensure that the F5 Distributed Cloud Bot Defense Mobile SDK can inspect
 * and process response headers for its internal functionality.
 *
 * Dated: 2024
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import kotlin.Pair;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represent an HTTP response. Instances of this class are immutable.
 */
public class Response {

    @Getter(AccessLevel.PACKAGE)
    private okhttp3.Response internalRes;

    Response(@NonNull okhttp3.Response response) {
        this.internalRes = response;
    }

    public URL url() {
        return internalRes.request().url().url();
    }

    public int code() {
        return internalRes.code();
    }

    public String message() {
        return internalRes.message();
    }

    public Iterator<Pair<String, String>> headers() {
        return internalRes.headers().iterator();
    }

    public String header(String name) {
        return internalRes.header(name);
    }

    public List<String> headers(String name) {
        return internalRes.headers(name);
    }

    public okhttp3.ResponseBody body() {
        return internalRes.body();
    }
}