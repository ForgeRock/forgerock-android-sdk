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

/**
 * Registry to manage {@link ResponseInterceptor}
 */
public class ResponseInterceptorRegistry {

    private static final ResponseInterceptorRegistry INSTANCE = new ResponseInterceptorRegistry();

    private ResponseInterceptor[] responseInterceptors;

    private ResponseInterceptorRegistry() {
    }

    /**
     * Returns a cached instance {@link ResponseInterceptorRegistry}
     *
     * @return instance of {@link ResponseInterceptorRegistry}
     */
    public static ResponseInterceptorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register new {@link ResponseInterceptor}(s)
     *
     * @param responseInterceptors A list of response interceptors
     */
    public void register(ResponseInterceptor... responseInterceptors) {
        this.responseInterceptors = responseInterceptors;
    }

    public ResponseInterceptor[] getResponseInterceptors() {
        return this.responseInterceptors;
    }
}