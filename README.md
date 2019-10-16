# ForgeRock Android SDK 
ForgeRock Android SDK is a toolkit that allows developers communicate efficiently with ForgeRock Platform and ForgeRock Identity Cloud.

## Release Status
SDK is **currently still in development** and scheduling for Beta release in October, 2019. Please note that SDK's interfaces, functionalities, and designs may change at any time prior to the official release.

## Requirements
* Android 5.0 (API Level 21) and above

## Quick Start

### Add Forgerock Android SDK Dependency
```gradle
dependencies {
    ...
    implementation 'org.forgerock:forgerock-auth:<version>'
}
```
#### Optional Dependency

| Feature        | Dependency | 
| -------------  |:-------------:| 
| UI Template    | implementation 'org.forgerock:forgerock-auth-ui:0.9.0| 
| ReCAPTCHA     | implementation 'com.google.android.gms:play-services-safetynet:17.0.0'      |
| Location for Device Profile | implementation 'com.google.android.gms:play-services-safetynet:17.0.0' |

### Add Compile Option

```gradle
android {
    ...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

### Configure Forgerock Android SDK

#### Using res/values/strings.xml

| Attribute        | Description           | Default Value  |
| -----------------|---------------------| -----:|
| forgerock_oauth_client_id     | OAuth2 client's `client_id` registered in OpenAM| N/A |
| forgerock_oauth_redirect_uri     | OAuth2 client's `redirect_uri` registered in OpenAM      |   N/A |
| forgerock_oauth_scope |   OAuth2 client's `scope` registered in OpenAM   | N/A
| forgerock_oauth_threshold | Threshold in seconds to refresh OAuth2 token set before `access_token` expires through FRAuth SDK's token management feature. | 30 |
| forgerock_url | Base URL of OpenAM | N/A |
| forgerock_realm | `realm` in OpenAM | root |
| forgerock_timeout | Timeout in seconds of each request that FRAuth SDK communicates to OpenAM. | 30 |
| forgerock | SSO Account Label | N/A |
| forgerock_account_name | SSO Account Name | N/A |
| forgerock_auth_service | Authentication Tree name registered in OpenAM for user authentication. | N/A |
| forgerock_registration_service | Authentication Tree name registered in OpenAM for user registration. | N/A |

See below for sample `strings.xml`:

```xml
...
<!-- OAuth -->
<string name="forgerock_oauth_client_id" translatable="false">place holder</string>
<string name="forgerock_oauth_redirect_uri" translatable="false">place holder</string>
<string name="forgerock_oauth_scope" translatable="false">place holder</string>
<integer name="forgerock_oauth_threshold" translatable="false">30</integer> <!-- in second -->
 
<!-- Server -->
<string name="forgerock_url" translatable="false">place holder</string>
<string name="forgerock_realm" translatable="false">place holder</string>
<integer name="forgerock_timeout" translatable="false">30</integer>
 
<!-- SSO -->
<string name="forgerock">place holder</string>
<string name="forgerock_account_name" translatable="false">place holder</string>
 
<!-- Service -->
<string name="forgerock_auth_service" translatable="false">place holder</string>
<string name="forgerock_registration_service" translatable="false">place holder</string>

```

#### SSO Configuration

Add `android:sharedUserId` to `AndroidManifest.xml` for the SSO Group

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          android:sharedUserId="org.forgerock.singleSignOn"
          package="org.forgerock.auth">
```

Add `AuthenticatorService` to `AndroidManifest.xml` and reference to authenticator resource

```xml
<service android:name="org.forgerock.android.auth.authenticator.AuthenticatorService">
    <intent-filter>
        <action android:name="android.accounts.AccountAuthenticator"/>
    </intent-filter>
    <meta-data
        android:name="android.accounts.AccountAuthenticator"
        android:resource="@xml/forgerock_authenticator" />
</service>
```

Create `forgerock_authenticator.xml` under `res/xml` folder

```xml
<account-authenticator
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:accountType="org.forgerock"
        android:icon="@mipmap/ic_logo"
        android:smallIcon="@mipmap/ic_logo"
        android:label="@string/forgerock"/>

```

### Add Authentication with Forgerock Android SDK 

#### Start the SDK

```java
FRAuth.start();
```

#### Embedded Login
`forgerock-authui` provides simple authentication template in your application.

Include `LoginFragment` in your Activity or Fragment

```xml
<fragment
    android:id="@+id/loginFragment"
    android:name="org.forgerock.android.auth.ui.LoginFragment"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" /
```

Here is an example of embedded the `forgerock-authui` to `fragment`:

```xml
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        tools:context=".ui.main.HomeFragment">

            <androidx.cardview.widget.CardView
                    android:id="@+id/signIn"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:layout_margin="8dp"
                    android:translationZ="2dp"
                    app:cardCornerRadius="2dp">

                    <fragment
                            android:id="@+id/loginFragment"
                            android:name="org.forgerock.android.auth.ui.LoginFragment"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content" />
            </androidx.cardview.widget.CardView>

</androidx.constraintlayout.widget.ConstraintLayout>

```

Implement `FRListener<Void>` in your Activity or Fragment to receive Authentication result.

```java
public class HomeFragment extends Fragment implements FRListener<Void> {
    ...

    @Override
    public void onSuccess(Void result) {
        //Retrieve the authenticated User
        FRUser.getCurrentUser();
    }

    @Override
    public void onException(Exception e) {
        //Handle Exception
    }

}
```