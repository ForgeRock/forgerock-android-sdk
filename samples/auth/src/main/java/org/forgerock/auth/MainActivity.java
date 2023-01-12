/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.google.android.material.snackbar.Snackbar.LENGTH_LONG;
import static org.forgerock.android.auth.ui.SimpleLoginActivity.ERROR_EXTRA;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;
import com.nimbusds.jwt.JWTParser;

import org.forgerock.android.auth.AccessToken;
import org.forgerock.android.auth.Config;
import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRDevice;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.PolicyAdvice;
import org.forgerock.android.auth.SecureCookieJar;
import org.forgerock.android.auth.UserInfo;
import org.forgerock.android.auth.interceptor.AccessTokenInterceptor;
import org.forgerock.android.auth.interceptor.AdviceHandler;
import org.forgerock.android.auth.interceptor.IdentityGatewayAdviceInterceptor;
import org.forgerock.android.auth.ui.AdviceDialogHandler;
import org.forgerock.android.auth.ui.LoginFragment;
import org.forgerock.android.auth.ui.SimpleLoginActivity;
import org.forgerock.android.auth.ui.SimpleRegisterActivity;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {

    public static final int AUTH_REQUEST_CODE = 100;
    public static final int REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView success;
    private TextView content;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /*
        RequestInterceptorRegistry.getInstance().register(
                new ForceAuthRequestInterceptor(),
                new NoSessionRequestInterceptor()
        );
         */

        //CallbackFactory.getInstance().register(MyCustomDeviceProfile.class);
        FRAuth.start(this);
        Logger.set(Logger.Level.DEBUG);
        super.onCreate(savedInstanceState);

        setContentView(org.forgerock.auth.R.layout.activity_main);
        success = findViewById(org.forgerock.auth.R.id.success);
        content = findViewById(org.forgerock.auth.R.id.content);
        progressBar = findViewById(org.forgerock.auth.R.id.progressBar);
        progressBar.setVisibility(INVISIBLE);

        if (getIntent() != null) {
            if (getIntent().getData() != null) {
                Intent resume = new Intent(this, SimpleLoginActivity.class);
                resume.setData(getIntent().getData());
                startActivityForResult(resume, AUTH_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTH_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                if (data!= null && data.getStringExtra(ERROR_EXTRA) != null) {
                    Snackbar.make(findViewById(org.forgerock.auth.R.id.success), "Login Failed:" +
                            data.getStringExtra(ERROR_EXTRA) , LENGTH_LONG).show();
                } else {
                    success.setVisibility(VISIBLE);
                    userinfo();
                }
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
                TreeDialogFragment.newInstance().show(getSupportFragmentManager(), "TREE");
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
                TreeDialogFragment.newInstance().show(getSupportFragmentManager(), "TREE");

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
                userinfo();
                return true;
            case org.forgerock.auth.R.id.invoke:

                OkHttpClient.Builder builder = new OkHttpClient.Builder()
                        .followRedirects(false);

                if (Logger.isDebugEnabled()) {
                    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                    interceptor.level(HttpLoggingInterceptor.Level.BODY);
                    builder.addInterceptor(interceptor);
                }

                builder.addInterceptor(new IdentityGatewayAdviceInterceptor<Void>() {
                    @Override
                    public AdviceHandler<Void> getAdviceHandler(PolicyAdvice advice) {
                        return new AdviceDialogHandler();
                    }
                });
                builder.addInterceptor(new AccessTokenInterceptor());
                builder.cookieJar(SecureCookieJar.builder()
                        .context(this.getApplicationContext())
                        .build());

                OkHttpClient client = builder.build();
                Request request = new Request.Builder().url("http://openig.example.com:9090/products").build();
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

            case R.id.token:
                JSONObject output = new JSONObject();
                if (FRUser.getCurrentUser() != null) {
                    FRUser.getCurrentUser().getAccessToken(new FRListener<AccessToken>() {
                        @Override
                        public void onSuccess(AccessToken result) {
                            try {
                                put(output, "ACCESS_TOKEN_RAW", new JSONObject(result.toJson()));
                            } catch (JSONException e) {
                                //ignore
                            }
                            try {
                                put(output, "ACCESS_TOKEN", new JSONObject(JWTParser.parse(result.getValue()).getJWTClaimsSet().toString()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                put(output, "REFRESH_TOKEN", new JSONObject(JWTParser.parse(result.getRefreshToken()).getJWTClaimsSet().toString()));
                            } catch (Exception e) {
                            }
                            try {
                                put(output, "ID_TOKEN", new JSONObject(JWTParser.parse(result.getIdToken()).getJWTClaimsSet().toString()));
                            } catch (Exception e) {
                                //ignore
                            }

                            runOnUiThread(() -> {
                                try {
                                    success.setVisibility(View.GONE);
                                    content.setText(output.toString(2));
                                } catch (JSONException e) {
                                    //ignore
                                }
                            });
                        }

                        @Override
                        public void onException(Exception e) {
                            put(output, "ERROR", e.getMessage());
                        }
                    });

                    if (FRSession.getCurrentSession() != null) {
                        if (FRSession.getCurrentSession().getSessionToken() != null) {
                            put(output, "SESSION", FRSession.getCurrentSession().getSessionToken().getValue());
                        }
                    }
                }
                return true;

            case R.id.revokeToken:
                progressBar.setVisibility(VISIBLE);
                revokeAccessToken();
                return true;
            case R.id.trustAllCert:

                try {
                    final TrustManager trustManager = new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[] {};
                        }
                    };
                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, new TrustManager[] { trustManager }, new java.security.SecureRandom());
                    Config.getInstance().reset();
                    Config.getInstance().init(this, null);
                    Config.getInstance().setBuildSteps(Collections.singletonList(builder1 -> {
                        builder1.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManager);
                        builder1.hostnameVerifier((s, sslSession) -> true);
                    }));

                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    runOnUiThread(() -> content.setText(e.getMessage()));
                }

            case R.id.webAuthn:
                success.setVisibility(INVISIBLE);
                content.setText("");
                listWebAuthnCredentials();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            //ignore
        }
    }

    public void userinfo() {
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
    }

    private void revokeAccessToken() {
        if (FRUser.getCurrentUser() != null) {
            FRUser.getCurrentUser().revokeAccessToken(new FRListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(INVISIBLE);
                        content.setText("Access token revoked");
                    });
                }

                @Override
                public void onException(Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(INVISIBLE);
                        content.setText("Access token revoked locally only!\n");
                        content.append("Error message: " + e.getMessage());
                    });
                }
            });
        } else {
            progressBar.setVisibility(INVISIBLE);
            content.setText("No User Session");
        }
    }

    private void listWebAuthnCredentials() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("List WebAuthn Credentials");
        builder.setMessage("List all credentials by RpId");
        final EditText rpIdInput = new EditText(this);
        rpIdInput.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(rpIdInput);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String rpId = rpIdInput.getText().toString();
                dialog.dismiss();

                progressBar.setVisibility(INVISIBLE);
                content.setText("");

                if (rpId != null) {

                    Intent webAuthnList = new Intent(MainActivity.this, WebAuthnKeysListActivity.class);
                    webAuthnList.putExtra("RPID", rpId);
                    MainActivity.this.startActivity(webAuthnList);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressBar.setVisibility(INVISIBLE);
                content.setText("");
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.GRAY);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.DKGRAY);
            }
        });

        dialog.show();
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


    public void launchTree(String result) {
        Intent loginIntent = new Intent(this, SimpleLoginActivity.class);
        loginIntent.putExtra(LoginFragment.TREE_NAME, result);
        startActivityForResult(loginIntent, AUTH_REQUEST_CODE);
    }

    public void launchBrowser() {

        FRUser.browser().appAuthConfigurer()
                .authorizationRequest(r -> {
                    Map<String, String> additionalParameters = new HashMap<>();
                    additionalParameters.put("service", "Simple");
                    additionalParameters.put("KEY2", "VALUE2");
                    //r.setAdditionalParameters(additionalParameters);
                    //r.setLoginHint("login");
                    //r.setPrompt("login");
                })
                .customTabsIntent(t -> {
                    t.setShowTitle(false);
                    t.setToolbarColor(getResources().getColor(R.color.colorAccent));
                }).done()
                .login(this, new FRListener<FRUser>() {
                    @Override
                    public void onSuccess(FRUser result) {
                        userinfo();
                    }

                    @Override
                    public void onException(Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(INVISIBLE);
                            content.setText(e.getMessage());
                        });

                    }
                });
    }
}
