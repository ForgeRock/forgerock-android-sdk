/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import lombok.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Manages Server configuration information
 */
@EqualsAndHashCode
@Getter
public class ServerConfig {

    public static final String API_VERSION_2_1 = "resource=2.1, protocol=1.0";
    public static final String API_VERSION_3_1 = "resource=3.1, protocol=1.0";
    public static final String ACCEPT_API_VERSION = "Accept-API-Version";

    private String url;

    private String realm;

    private Integer timeout;

    private TimeUnit timeUnit;

    private List<String> pins;

    private String host;

    @lombok.Builder
    public ServerConfig(@NonNull Context context,
                        @NonNull String url,
                        String realm,
                        Integer timeout,
                        TimeUnit timeUnit,
                        @Singular List<String> pins) {

        Config config = Config.getInstance(context);

        this.url = url;
        try {
            this.host = new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        this.realm = config.applyDefaultIfNull(realm);
        this.timeout = config.applyDefaultIfNull(timeout);
        this.timeUnit = timeUnit == null ? SECONDS : timeUnit;
        this.pins = config.applyDefaultIfNull(pins);

    }


}
