/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator.network;

import androidx.annotation.NonNull;

import org.forgerock.android.authenticator.FRAListener;

import java.net.URL;

/**
 * The network client interface used to establish network connections via HTTP protocol.
 */
public interface NetworkClient {

    /**
     * Connect to the url provided within the connection parameters.
     *
     * @param url   URL for the connection.
     * @param connectionProperties parameters for the connection.
     * @return a ConnectionResponse.
     * @throws Exception the exception indicating failure case.
     */
    ConnectionResponse connect(@NonNull URL url, @NonNull ConnectionProperties connectionProperties) throws Exception;

    /**
     * Connect asynchronously to the url provided within the connection parameters.
     *
     * @param url   URL for the connection.
     * @param connectionProperties parameters for the connection.
     * @param listener registered listener to receive results {@link FRAListener}
     * @return a InputStream.
     * @throws Exception the exception indicating failure case.
     */
    void connect(@NonNull URL url, @NonNull ConnectionProperties connectionProperties, @NonNull FRAListener<ConnectionResponse> listener);

    /**
     * Close the network connection and clean up any resources in NetworkClient.
     */
    void terminate();

    /**
     * Cancel a connection attempt.
     */
    void cancel();

}
