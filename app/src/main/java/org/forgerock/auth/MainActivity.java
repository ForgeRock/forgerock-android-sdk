/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRDevice;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.PolicyAdvice;
import org.forgerock.android.auth.SecureCookieJar;
import org.forgerock.android.auth.UserInfo;
import org.forgerock.android.auth.interceptor.AccessTokenInterceptor;
import org.forgerock.android.auth.interceptor.AdviceHandler;
import org.forgerock.android.auth.interceptor.IdentityGatewayAdviceInterceptor;
import org.forgerock.android.auth.ui.AdviceDialogHandler;
import org.forgerock.android.auth.ui.SimpleLoginActivity;
import org.forgerock.android.auth.ui.SimpleRegisterActivity;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.google.android.material.snackbar.Snackbar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {

    public static final int AUTH_REQUEST_CODE = 100;
    public static final int REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView success;
    private TextView content;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FRAuth.start(this);
        Logger.set(Logger.Level.DEBUG);
        super.onCreate(savedInstanceState);

        setContentView(org.forgerock.auth.R.layout.activity_main);
        success = findViewById(org.forgerock.auth.R.id.success);
        content = findViewById(org.forgerock.auth.R.id.content);
        progressBar = findViewById(org.forgerock.auth.R.id.progressBar);
        progressBar.setVisibility(INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTH_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                success.setVisibility(VISIBLE);
                FRUser.getCurrentUser().getUserInfo(new FRListener<UserInfo>() {
                    @Override
                    public void onSuccess(final UserInfo result) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(INVISIBLE);
                            try {
                                content.setText(result.getRaw().toString(2));
                            } catch (JSONException e) {
                                onException(e);
                            }
                        });
                    }

                    @Override
                    public void onException(final Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(INVISIBLE);
                            content.setText(e.getMessage());
                        });
                    }
                });
            } else {
                Snackbar.make(findViewById(org.forgerock.auth.R.id.success), "Login Failed", LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(org.forgerock.auth.R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case org.forgerock.auth.R.id.login:
                success.setVisibility(INVISIBLE);
                content.setText("");
                Intent loginIntent = new Intent(this, SimpleLoginActivity.class);
                startActivityForResult(loginIntent, AUTH_REQUEST_CODE);
                return true;

            case org.forgerock.auth.R.id.register:
                success.setVisibility(INVISIBLE);
                content.setText("");
                Intent registerIntent = new Intent(this, SimpleRegisterActivity.class);
                startActivityForResult(registerIntent, AUTH_REQUEST_CODE);
                return true;

            case org.forgerock.auth.R.id.logout:
                success.setVisibility(INVISIBLE);
                content.setText("");
                if (FRUser.getCurrentUser() != null) {
                    FRUser.getCurrentUser().logout();
                }
                Intent relogin = new Intent(this, SimpleLoginActivity.class);
                startActivityForResult(relogin, AUTH_REQUEST_CODE);

                return true;
            case org.forgerock.auth.R.id.profile:
                checkPermission();
                success.setVisibility(View.GONE);
                FRDevice.getInstance().getProfile(new FRListener<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        runOnUiThread(() -> {
                            try {
                                content.setText(result.toString(4));
                            } catch (JSONException e) {
                                Logger.warn(TAG, e, "Failed to convert json to string");
                            }
                        });
                    }

                    @Override
                    public void onException(Exception e) {
                        Logger.warn(TAG, e, "Failed to retrieve device profile");
                    }
                });
                return true;
            case R.id.userinfo:
                if (FRUser.getCurrentUser() != null) {
                    FRUser.getCurrentUser().getUserInfo(new FRListener<UserInfo>() {
                        @Override
                        public void onSuccess(final UserInfo result) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(INVISIBLE);
                                try {
                                    content.setText(result.getRaw().toString(2));
                                } catch (JSONException e) {
                                    onException(e);
                                }
                            });
                        }

                        @Override
                        public void onException(final Exception e) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(INVISIBLE);
                                content.setText(e.getMessage());
                            });
                        }
                    });
                } else {
                    content.setText("No User Session");
                }
                return true;
            case org.forgerock.auth.R.id.invoke:
                OkHttpClient.Builder builder = new OkHttpClient.Builder()
                        .followRedirects(false);

                builder.addInterceptor(new IdentityGatewayAdviceInterceptor() {
                    @Override
                    public AdviceHandler getAdviceHandler(PolicyAdvice advice) {
                        return new AdviceDialogHandler();
                    }
                });
                builder.addInterceptor(new AccessTokenInterceptor());
                builder.cookieJar(SecureCookieJar.builder().build());

                OkHttpClient client = builder.build();
                Request request = new Request.Builder().url("http://openig.example.com:8080/products").build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        runOnUiThread(() -> content.setText(e.getMessage()));
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                try {
                                    content.setText("Response:" + response.body().string());
                                } catch (IOException e) {
                                    content.setText(e.getMessage());
                                }
                            } else {
                                content.setText("Failed:" + response.message());
                            }
                        });
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(this),
                ACCESS_FINE_LOCATION)) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setMessage("We need location to next")
                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, REQUEST_CODE))
                    .create();
            alertDialog.show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, REQUEST_CODE);
        }
    }


}
