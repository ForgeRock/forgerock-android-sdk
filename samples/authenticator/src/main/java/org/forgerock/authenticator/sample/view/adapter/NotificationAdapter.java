/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushMechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.authenticator.sample.R;
import org.forgerock.authenticator.sample.controller.AuthenticatorModel;
import org.forgerock.authenticator.sample.view.layout.NotificationLayout;

import java.util.List;

/**
 * Class for linking the complete list of Notifications with a series of layouts which display each one.
 */
public class NotificationAdapter extends BaseAdapter {
    private PushMechanism mechanism;
    private final LayoutInflater mLayoutInflater;
    private List<PushNotification> notificationList;

    /**
     * Creates the adapter, and finds the data model.
     */
    public NotificationAdapter(Context context, Mechanism mechanism) {
        this.mechanism = (PushMechanism)mechanism;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        AuthenticatorModel authenticatorModel = AuthenticatorModel.getInstance(context);
        notificationList = authenticatorModel.getNotificationsForMechanism(mechanism);
    }

    public int getCount() {
        return notificationList.size();
    }

    @Override
    public PushNotification getItem(int position) {
        return notificationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.layout_notification, parent, false);
        }

        PushNotification notification = getItem(position);
        ((NotificationLayout) convertView).bind(notification, mechanism);

        return convertView;
    }

}


