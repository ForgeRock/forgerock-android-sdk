/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputLayout;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PollingWaitCallback;
import org.forgerock.android.auth.ui.R;

import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.INVISIBLE;

/**
 * UI representation for {@link PollingWaitCallback}
 */
public class PollingWaitCallbackFragment extends CallbackFragment<PollingWaitCallback> {

    private static final String REMAIN = "REMAIN";
    private static final String PROGRESS_SO_FAR = "PROGRESS_SO_FAR";
    private long waitTime;
    private Timer timer;
    private int progressSoFar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_polling_wait_callback, container, false);
        TextView messageText = view.findViewById(R.id.message);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        messageText.setText(callback.getMessage());

        if (savedInstanceState != null)  {
            waitTime = savedInstanceState.getLong(REMAIN, Long.parseLong(callback.getWaitTime()));
            progressSoFar = savedInstanceState.getInt(PROGRESS_SO_FAR, 0);
        } else {
            waitTime = Long.parseLong(callback.getWaitTime());
        }

        long split = waitTime / 100;

        timer = new Timer();
        Handler handler = new Handler();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                waitTime = waitTime  - split;
                if (waitTime < 0) {
                    timer.cancel();
                    handler.post(() -> {
                        progressBar.setVisibility(INVISIBLE);
                        next();
                    });
                } else {
                    handler.post(() -> progressBar.setProgress(++progressSoFar));
                }
           }
        }, split, split);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        timer.cancel();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(REMAIN, waitTime);
        outState.putInt(PROGRESS_SO_FAR, progressSoFar);
    }
}
