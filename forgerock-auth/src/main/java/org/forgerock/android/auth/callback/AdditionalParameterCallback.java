/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import java.util.Map;

/**
 * Defines the interface that allows to inject addition parameter to the authenticate request.
 *
 * @see Callback
 */

public interface AdditionalParameterCallback {

    Map<String, String> getAdditionalParameters();

}
