/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import android.content.Context;

import org.forgerock.android.auth.DefaultNetworkClient;
import org.forgerock.android.auth.DefaultStorageClient;
import org.forgerock.android.authenticator.network.NetworkClient;

/**
 * The top level FRAClient object represents the Authenticator module of the ForgeRock
 * Mobile SDK. It is the front facing class where the configuration settings for the SDK can be
 * found and utilized.
 */
public class FRAClient {

    /**
     * The network client.
     */
    private NetworkClient networkClient;

    /**
     * The storage client.
     */
    private StorageClient storageClient;

    /**
     * The Context.
     */
    private Context context;

    private FRAClient(Context context, StorageClient storageClient, NetworkClient networkClient) {
        this.context = context;
        this.storageClient = storageClient;
        this.networkClient = networkClient;

    }

    static FRAClientBuilder builder() {
        return new FRAClientBuilder();
    }

    /**
     * The asynchronous Authenticator client builder
     */
    public static class FRAClientBuilder {
        private StorageClient storageClient;
        private NetworkClient networkClient;
        private Context context;

        /**
         * Sets the context.
         *
         * @param context the context
         * @return this builder
         */
        public FRAClientBuilder withContext(Context context) {
            context = context;
            return this;
        }

        /**
         * Set a storage client implementation to use. You can define your own storage implementing
         * {@link StorageClient} or use the default implementation {@link DefaultStorageClient}
         *
         * @param storage the storage implementation
         * @return this builder
         */
        public FRAClientBuilder withStorage(StorageClient storage) {
            this.storageClient = storage;
            return this;
        }

        /**
         * Starts FRAClient.
         *
         * @return the authenticator client {@link FRAClient}
         */
        public FRAClient build() {

            return createFRAuthenticator();
        }

        /**
         * Create the FRAClient client.
         *
         * @return the FRAClient
         */
        protected FRAClient createFRAuthenticator() {

            if(storageClient == null) {
                storageClient = new DefaultStorageClient(context);
            }

            if(networkClient == null) {
                networkClient = new DefaultNetworkClient(context);
            }

            return new FRAClient(context, storageClient, networkClient);
        }
    }
}
