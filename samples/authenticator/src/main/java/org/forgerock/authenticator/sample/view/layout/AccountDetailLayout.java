/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.forgerock.android.auth.Account;
import org.forgerock.android.auth.OathMechanism;
import org.forgerock.android.auth.OathTokenCode;
import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.forgerock.authenticator.sample.R;

/**
 * Handles the display of a Token and it's elements. Receives UI elements as parameter on the
 * constructor and update those continuously
 */
public class AccountDetailLayout extends FrameLayout {

    private ProgressBar progressOuter;
    private TextView codeDisplay;
    private ImageButton refreshButton;
    private RelativeLayout accountDetailContent;
    private LinearLayout accountDetailLocked;

    private OathTokenCode tokenCode;
    private String code;

    private static final int HOTP_COOLDOWN = 5000;
    private static final int TOTP_TICK = 100;

    public AccountDetailLayout(@NonNull Context context) {
        super(context);
    }

    public AccountDetailLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AccountDetailLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AccountDetailLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        progressOuter = findViewById(R.id.progressOuter);
        codeDisplay = findViewById(R.id.code);
        refreshButton = findViewById(R.id.refresh);
        accountDetailContent = findViewById(R.id.account_detail_content);
        accountDetailLocked = findViewById(R.id.account_detail_locked);
    }

    public void bind(final OathMechanism oath, final Account account) {
        if (account.isLocked()) {
            accountDetailLocked.setVisibility(View.VISIBLE);
            accountDetailContent.setVisibility(View.GONE);
            TextView lockReason = ((TextView) findViewById(R.id.lock_reason));
            lockReason.setText(String.format(
                    getContext().getString(R.string.account_locked),
                    account.getLockingPolicy()
            ));
        } else {
            accountDetailLocked.setVisibility(View.GONE);
            accountDetailContent.setVisibility(View.VISIBLE);
            tokenCode = null;
            progressOuter.clearAnimation();

            // Cancel all active animations.
            setEnabled(true);
            progressOuter.clearAnimation();

            switch (oath.getOathType()) {
                case HOTP:
                    setupHOTP(oath);
                    break;
                case TOTP:
                    setupTOTP(oath);
                    break;
            }
        }
    }

    private void setupTOTP(final OathMechanism oath)  {
        progressOuter.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.GONE);
        try {
            tokenCode = oath.getOathTokenCode();
        } catch (OathMechanismException | AccountLockException e) {
            e.printStackTrace();
        }

        Runnable totpRunnable = new Runnable() {
            @Override
            public void run() {
                if(tokenCode != null) {
                    if (!tokenCode.isValid()) {
                        try {
                            tokenCode = oath.getOathTokenCode();
                        } catch (OathMechanismException | AccountLockException e) {
                        }
                    }

                    code = tokenCode.getCurrentCode();

                    setDisplayCode(code);

                    progressOuter.setProgress(tokenCode.getProgress());
                    postDelayed(this, TOTP_TICK);
                }
            }
        };
        if(tokenCode != null)
            post(totpRunnable);
    }

    private void setupHOTP(final OathMechanism oath)  {

        progressOuter.setVisibility(View.GONE);
        refreshButton.setVisibility(View.VISIBLE);

        StringBuilder placeholderBuilder = new StringBuilder();
        for (int i = 0; i < oath.getDigits(); i++) {
            placeholderBuilder.append('-');
            if (i == oath.getDigits() / 2 - 1) {
                placeholderBuilder.append(' ');
            }
        }
        String placeholder = new String(placeholderBuilder);

        codeDisplay.setText(placeholder);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update the code.
                try {
                    code = oath.getOathTokenCode().getCurrentCode();
                } catch (OathMechanismException | AccountLockException e) {
                }
                setDisplayCode(code);
                refreshButton.setEnabled(false);

                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshButton.setEnabled(true);
                    }
                }, HOTP_COOLDOWN);
            }
        });
    }

    private void setDisplayCode(String code) {
        String formattedCode = code.substring(0, code.length() / 2) + " " +
                code.substring(code.length() / 2);

        codeDisplay.setText(formattedCode);
    }

}
