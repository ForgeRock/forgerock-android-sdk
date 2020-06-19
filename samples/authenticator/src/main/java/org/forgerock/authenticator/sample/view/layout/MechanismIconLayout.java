/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.PushMechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.view.activity.BaseNotificationActivity;
import org.forgerock.authenticator.sample.view.activity.NotificationsActivity;
import org.forgerock.authenticator.sample.view.activity.PushNotificationActivity;

/**
 * UI element which is an icon representation of a Mechanism. Contains both the icon for the
 * Mechanism, as well as a badge which displays the number of notifications attached to that
 * Mechanism.
 */
public class MechanismIconLayout extends FrameLayout {

    private Context context;

    /**
     * Create an icon that a will represent a Mechanism.
     * @param context The context that the view is created from.
     */
    public MechanismIconLayout(Context context) {
        super(context);
        init();
    }

    /**
     * Create an icon that a will represent a Mechanism.
     * @param context The context that the view is created from.
     * @param attrs The list of attributes.
     */
    public MechanismIconLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Create an icon that a will represent a Mechanism.
     * @param context The context that the view is created from.
     * @param attrs The list of attributes.
     * @param defStyleAttr The resource containing default style attributes.
     */
    public MechanismIconLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_mechanism_icon, this);
        context = getContext();
    }

    /**
     * Set the Mechanism that this icon represents.
     * @param mechanism The Mechanism that is represented by this icon.
     */
    public void setMechanism(final Mechanism mechanism) {
        ImageView icon = (ImageView) findViewById(R.id.icon_image);
        TextView badge = (TextView) findViewById(R.id.badge);

        int activeNotifications = 0;

        switch (mechanism.getType()) {
            case Mechanism.OATH:
                if(((OathMechanism) mechanism).getOathType().equals(OathMechanism.TokenType.HOTP)) {
                    icon.setImageDrawable(getResources().getDrawable(R.drawable.icon_hotp));
                } else if(((OathMechanism) mechanism).getOathType().equals(OathMechanism.TokenType.TOTP)) {
                    icon.setImageDrawable(getResources().getDrawable(R.drawable.icon_totp));
                }
                badge.setVisibility(GONE);
                break;
            case Mechanism.PUSH:
                icon.setImageDrawable(getResources().getDrawable(R.drawable.icon_push));
                for (PushNotification notification : ((PushMechanism)mechanism).getPendingNotifications()) {
                    if (notification.isPending() && !notification.isExpired()) {
                        activeNotifications++;
                    }
                }
                icon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BaseNotificationActivity.start(context,
                                NotificationsActivity.class, null, mechanism);
                    }
                });
                setNotificationNumber(activeNotifications, badge);
                break;
        }
    }

    private void setNotificationNumber(int notificationNumber, TextView badge) {
        if (notificationNumber == 0) {
            badge.setBackground(ContextCompat.getDrawable(context, R.drawable.notification_background));
        } else {
            badge.setBackground(ContextCompat.getDrawable(context, R.drawable.new_notification_background));
        }
        badge.setVisibility(VISIBLE);
        badge.setText(Integer.toString(notificationNumber));
    }
    
}
