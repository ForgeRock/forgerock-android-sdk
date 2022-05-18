/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.view.activity;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.forgerock.android.auth.FRAListener;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Mechanism;
import org.forgerock.android.auth.PushNotification;
import org.forgerock.android.auth.PushType;
import org.forgerock.authenticator.sample.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PushNotificationActivity extends BaseNotificationActivity {

    private static final String TAG = PushNotificationActivity.class.getSimpleName();

    /** Json attribute name for Device location. */
    private static final String LOCATION_ATTRIBUTE_NAME = "location";

    private View acceptButtons;
    private View challengeButtons;
    private View rejectChallengeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_notification);

        final PushNotification notification = getNotification();
        Mechanism mechanism = getMechanism();
        if (notification == null || !notification.isPending()) {
            finish();
            return;
        }

        final Activity thisActivity = this;

        // set notification header
        ((TextView) findViewById(R.id.question))
                .setText(String.format(getString(R.string.pushauth_login_title), mechanism.getIssuer()));
        ((TextView) findViewById(R.id.issuer)).setText(mechanism.getIssuer());
        ((TextView) findViewById(R.id.accountName)).setText(mechanism.getAccountName());
        ((TextView) findViewById(R.id.message)).setText(notification.getMessage());

        // set location, if available
        setLocation(notification);

        // set actions buttons based on the push type
        acceptButtons = findViewById(R.id.acceptButtons);
        challengeButtons = findViewById(R.id.challengeButtons);
        rejectChallengeButton = findViewById(R.id.rejectChallengeButton);
        if(notification.getPushType() == PushType.CHALLENGE) {
            setupPushChallengeActions(thisActivity, notification);
        } else if (notification.getPushType() == PushType.BIOMETRIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setupPushBiometricActions(thisActivity, notification);
        } else {
            setupPushDefaultsActions(thisActivity, notification);
        }

    }

    private void setupPushChallengeActions(Activity thisActivity, PushNotification notification) {
        acceptButtons.setVisibility(View.GONE);
        challengeButtons.setVisibility(View.VISIBLE);
        rejectChallengeButton.setVisibility(View.VISIBLE);
        int[] choices = notification.getNumbersChallenge();

        Button choice1Button = findViewById(R.id.choice1);
        choice1Button.setText(String.valueOf(choices[0]));
        choice1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.accept(String.valueOf(choices[0]), new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "approved"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });

        Button choice2Button = findViewById(R.id.choice2);
        choice2Button.setText(String.valueOf(choices[1]));
        choice2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.accept(String.valueOf(choices[1]), new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "approved"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });

        Button choice3Button = findViewById(R.id.choice3);
        choice3Button.setText(String.valueOf(choices[2]));
        choice3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.accept(String.valueOf(choices[2]), new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "approved"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });

        Button denyButton = findViewById(R.id.pushChallengeDeny);
        denyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.deny(new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "denied"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });
    }

    private void setupPushBiometricActions(Activity thisActivity, PushNotification notification) {
        acceptButtons.setVisibility(View.VISIBLE);
        challengeButtons.setVisibility(View.GONE);
        rejectChallengeButton.setVisibility(View.GONE);

        final AppCompatActivity activity = this;
        Button approveButton = findViewById(R.id.pushApprove);
        approveButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                notification.accept(null, null, true, activity, new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "approved"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });

        Button denyButton = findViewById(R.id.pushDeny);
        denyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.deny(new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "denied"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });
    }

    private void setupPushDefaultsActions(Activity thisActivity, PushNotification notification) {
        acceptButtons.setVisibility(View.VISIBLE);
        challengeButtons.setVisibility(View.GONE);
        rejectChallengeButton.setVisibility(View.GONE);

        Button approveButton = findViewById(R.id.pushApprove);
        approveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.accept(new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "approved"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });

        Button denyButton = findViewById(R.id.pushDeny);
        denyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.deny(new FRAListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, String.format(getString(R.string.push_notification_approval), "denied"), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        PushNotificationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                });
            }
        });
    }

    private void setLocation(PushNotification notification) {
        String location = getAttributeFromContextInfo(notification, LOCATION_ATTRIBUTE_NAME);
        if(location != null) {
            try {
                String geoLocation = "Unknown";

                JSONObject jsonObject = new JSONObject(location);
                double latitude = Double.parseDouble(jsonObject.getString("latitude"));
                double longitude = Double.parseDouble(jsonObject.getString("longitude"));

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(latitude, longitude, 1);
                if (list != null && list.size() > 0) {
                    Address address = list.get(0);
                    geoLocation = address.getLocality() + ", " + address.getCountryName();
                }
                ((TextView) findViewById(R.id.geoLocation)).setText(geoLocation);
            } catch (JSONException | IOException e) {
                Logger.warn(TAG, e, "Error parsing location: %s", location);
            }
        }
    }

    private String getAttributeFromContextInfo(PushNotification notification, String attributeName) {
        String value = null;
        String contextInfo = notification.getContextInfo();
        if(contextInfo != null && !contextInfo.isEmpty()) {
            try {
                JSONObject payload = new JSONObject(contextInfo);
                if(payload.has(attributeName)) {
                    value = payload.getString(attributeName);
                }
            } catch (JSONException e) {
                Logger.warn(TAG, e, "Error parsing contextInfo: %s", contextInfo);
            }
        }
        return value;
    }

}
