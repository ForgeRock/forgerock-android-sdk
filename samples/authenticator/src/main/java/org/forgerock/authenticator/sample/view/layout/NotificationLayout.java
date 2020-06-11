/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.layout;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.forgerock.android.auth.PushMechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.view.activity.BaseNotificationActivity;
import org.forgerock.authenticator.sample.view.activity.PushNotificationActivity;

/**
 * Individual entry which displays information about a given Notification.
 */
public class NotificationLayout extends FrameLayout {

    private PushNotification notification;
    private java.text.DateFormat dateFormat;
    private java.text.DateFormat timeFormat;

    /**
     * Create a cell that a Notification is displayed in.
     * @param context The context that the Notification is called from.
     */
    public NotificationLayout(Context context) {
        super(context);
        setup(context);
    }

    /**
     * Create a cell that a Notification is displayed in.
     * @param context The context that the Notification is called from.
     * @param attrs The list of attributes.
     */
    public NotificationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    /**
     * Create a cell that a Notification is displayed in.
     * @param context The context that the Notification is called from.
     * @param attrs The list of attributes.
     * @param defStyleAttr The resource containing default style attributes.
     */
    public NotificationLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    private void setup(Context context) {
        dateFormat = DateFormat.getLongDateFormat(context);
        timeFormat = DateFormat.getTimeFormat(context);
    }

    /**
     * Set the Notification that this Layout displays.
     * @param notification The Notification to display.
     */
    public void bind(final PushNotification notification, final PushMechanism mechanism) {
        this.notification = notification;
        final Context context = getContext();

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseNotificationActivity.start(context, PushNotificationActivity.class,
                        notification, mechanism);
            }
        });

        setClickable(notification.isPending() && !notification.isExpired());

        refresh();
    }

    /**
     * Update the current time and status, based on a millisecond value passed in.
     */
    public void refresh() {
        ImageView statusImage = findViewById(R.id.image);
        TextView statusText = findViewById(R.id.status);
        if (notification.isApproved()) {
            statusImage.setImageDrawable(getResources().getDrawable(R.drawable.icon_approved));
            statusText.setText(R.string.notification_status_approved);
        } else if (notification.isExpired() && notification.isPending()){
            statusImage.setImageDrawable(getResources().getDrawable(R.drawable.icon_expired));
            statusText.setText(R.string.notification_status_expired);
        } else if (notification.isPending()) {
            statusImage.setImageDrawable(getResources().getDrawable(R.drawable.icon_pending));
            statusText.setText(R.string.notification_status_pending);
        } else {
            statusImage.setImageDrawable(getResources().getDrawable(R.drawable.icon_denied));
            statusText.setText(R.string.notification_status_rejected);
        }

        TextView timeView = findViewById(R.id.time);
        timeView.setText(
                String.format("%s\n%s",
                dateFormat.format(notification.getTimeAdded().getTimeInMillis()),
                timeFormat.format(notification.getTimeAdded().getTimeInMillis()))
        );
    }

}
