/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.controller;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.view.activity.BaseNotificationActivity;
import org.forgerock.authenticator.sample.view.activity.PushNotificationActivity;

/**
 * FCM Service responds to downstream messages from the Firebase Messaging (FCM) framework.
 *
 * Responsible for triggering a Permissive Intent which will invoke the notification screen in
 * this App. The body of the FCM message is included in the Intent.
 */
public class FcmService extends FirebaseMessagingService {

    private static int messageCount = 1;
    private final Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    private static final String TAG = FcmService.class.getSimpleName();

    /**
     * Default instance of FcmService expected to be instantiated by Android framework.
     */
    public FcmService() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message){
        try {
            // Send remote message received to be processed by the Authenticator SDK
            PushNotification pushNotification =  AuthenticatorModel
                    .getInstance(getApplicationContext())
                    .handleRemoteMessage(message);

            // If it's a valid Push message from AM and not expired, create a system notification
            if(pushNotification != null && !pushNotification.isExpired()) {
                createSystemNotification(pushNotification);
            }
        } catch (InvalidNotificationException e) {
            Log.e(TAG,"Error handling remote message: ", e);
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);

        // This FCM method is called if InstanceID token is updated. This may occur if the security
        // of the previous token had been compromised.
        // Currently OpenAM does not provide an API to receives updates for those tokens. So, there
        // is no method available to handle it FRAClient. The current workaround is removed the Push
        // mechanism and add it again by scanning a new QRCode.
    }

    /**
     * Create system notification to display to user the Push request received
     * @param pushNotification the PushNotification object
     */
    private void createSystemNotification(PushNotification pushNotification) {
        int id = messageCount++;

        Mechanism mechanism = AuthenticatorModel.getInstance(getApplicationContext()).getMechanism(pushNotification);
        Intent intent = BaseNotificationActivity.setupIntent(this, PushNotificationActivity.class,
                pushNotification, mechanism);

        String title = String.format(getString(R.string.system_notification_title),
                mechanism.getAccountName(), mechanism.getIssuer());
        String body = getString(R.string.system_notification_body);

        Notification notification = generatePending(this, id, title, body, intent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(id, notification);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private Notification generatePending(Context context, int requestCode, String title, String message, Intent intent) {
        createNotificationChannel(context);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        }else {
            pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return new NotificationCompat.Builder(context, context.getString(R.string.channel_id))
                .setSmallIcon(R.drawable.forgerock_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = context.getString(R.string.channel_id);
            String channelName = context.getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

}
