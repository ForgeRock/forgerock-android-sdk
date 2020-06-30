/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushNotification;

public class BaseNotificationActivity extends AppCompatActivity {

    private static PushNotification notification;
    private static Mechanism mechanism;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Loads an intent with the required information to start an activity that needs a PushNotification.
     * @param context The context that the activity is being started from.
     * @param notificationActivity The class of activity to start.
     * @param pushNotification The notification to pass.
     * @param pushMechanism The mechanism to pass.
     * @return The generated intent.
     */
    public static Intent setupIntent(Context context,
                                     Class<? extends BaseNotificationActivity> notificationActivity,
                                     PushNotification pushNotification, Mechanism pushMechanism) {
        Intent intent = new Intent(context, notificationActivity);
        notification = pushNotification;
        mechanism = pushMechanism;
        return intent;
    }

    /**
     * Method used for starting activities that uses Push Notifications. Handles passing the
     * notification through to the Activity.
     * @param context The context that the activity is being started from.
     * @param notificationActivity The class of activity to start.
     * @param notification The notification to pass.
     * @param pushMechanism The mechanism to pass.
     */
    public static void start(Context context,
                             Class<? extends BaseNotificationActivity> notificationActivity,
                             PushNotification notification, Mechanism pushMechanism) {
        Intent intent = setupIntent(context, notificationActivity, notification, pushMechanism);
        context.startActivity(intent);
    }

    /**
     * Returns the Notification that has been passed into this activity.
     * @return The passed in Notification.
     */
    protected final PushNotification getNotification() {
        return notification;
    }

    /**
     * Returns the Mechanism that has been passed into this activity.
     * @return The passed in PushMechanism.
     */
    protected final Mechanism getMechanism() {
        return mechanism;
    }

}
