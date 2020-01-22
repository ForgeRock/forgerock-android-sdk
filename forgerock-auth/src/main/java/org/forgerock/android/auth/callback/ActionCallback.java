/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.content.Context;

import org.forgerock.android.auth.FRListener;

public interface ActionCallback {

    void execute(Context context, FRListener<Void> listener);
}
