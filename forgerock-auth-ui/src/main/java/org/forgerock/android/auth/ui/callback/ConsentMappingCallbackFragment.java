/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.callback.ConsentMappingCallback;
import org.forgerock.android.auth.ui.R;

import java.io.InputStream;
import java.net.URL;

/**
 * UI representation for {@link ConsentMappingCallback}
 */
public class ConsentMappingCallbackFragment extends CallbackFragment<ConsentMappingCallback> {

    private ImageView icon;
    private static final String TAG = ConsentMappingCallbackFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_consent_mapping_callback, container, false);
        TextView displayName = view.findViewById(R.id.displayName);
        displayName.setText(callback.getDisplayName());

        this.icon = view.findViewById(R.id.icon);
        loadIcon();

        TextView privacy = view.findViewById(R.id.privacy);
        privacy.setText(getResources().getString(R.string.privacy, callback.getDisplayName(), callback.getAccessLevel()));


        LinearLayout fields = view.findViewById(R.id.fields);

        if (callback.getFields() != null) {
            for (int i = 0; i < callback.getFields().length; i++) {
                TextView textView = new TextView(getContext());
                textView.setText(callback.getFields()[i]);
                fields.addView(textView, i);
            }
        }

        CheckBox accept = view.findViewById(R.id.acceptConsent);

        accept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            callback.setAccept(isChecked);
            onDataCollected();
        });
        return view;
    }

    private void loadIcon() {
        if (callback.getIcon() != null && callback.getIcon().length() > 0) {
            new Thread(() -> {
                try {
                    final Drawable drawable = Drawable.createFromStream((InputStream) new URL(callback.getIcon()).getContent(), "src");
                    icon.post(() -> {
                        icon.setImageDrawable(drawable);
                    });
                } catch (Exception e) {
                    Logger.warn(TAG, e, "Failed to load image." );
                }
            }).start();
        }
    }

}
