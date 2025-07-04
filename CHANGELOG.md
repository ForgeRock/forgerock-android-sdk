## [4.8.1]

#### Fixed
- Encryption and decryption performance has been enhanced through the implementation of caching for the KeyStore, Cipher, and Symmetric Key. Additionally, developers now have the flexibility to enable or disable StrongBox during key generation. [SDKS-4090]

## [4.8.0]
#### Added
- Support for new response payload in WebAuthn authentication and registration [SDKS-3843]
- Ability to update Firebase Cloud Messaging (FCM) device token for existing push mechanisms [SDKS-3684]

#### Fixed
- Improved logging for errors and warning exceptions [SDKS-3990]
- Fixed an issue causing a crash when the app process was killed in the background during the centralized login flow [SDKS-3993]

## [4.7.0]
#### Added
- A fallback mechanism that uses an asymmetric key if symmetric key generation in the Android Keystore fails [SDKS-3467]
- Support for Self-Service [SDKS-3408]
- Support for Sign-out with ID Token in the PingOne Platform [SDKS-3423]

#### Fixed
- Prevent duplicate PUSH notifications in the Authenticator module [SDKS-3533]
- Fixed an issue where, in some cases, a user's session was not invalidated upon re-authentication [SDKS-3772]

## [4.6.0]
#### Added
- Allow developers to customize SDK storage [SDKS-3378]
- Support PingOne Protect Marketplace nodes [SDKS-3297]
- Support reCAPTCHA Enterprise node [SDKS-3325]
- Expose Realm, Success Url with SSOToken [SDKS-3351]
- Support Android 15 [SDKS-3098]
- Support http/https scheme for Centralize Login redirect [SDKS-3433]

#### Fixed
- Skip Type 4 TextOutputCallback [SDKS-3227]
- Potential CustomTabManager ServiceConnection leak [SDKS-3346]
- access_token api call triggered twice on force refresh [SDKS-3254]
- Allow http/https as redirect scheme in centralize login flow [SDKS-3433]

## [4.5.0]
#### Added
- Added SDK support for deleting registered WebAuthn devices from the server. [SDKS-1710]
- Added support for signing off from PingOne to the centralized login flow. [SDKS-3020]
- Added the ability to dynamically configure the SDK by collecting values from the server's OpenID Connect `.well-known` endpoint. [SDKS-3022]

#### Fixed
- Resolved security vulnerability warnings related to the `commons-io-2.6.jar` and `bcprov-jdk15on-1.68.jar` libraries. [SDKS-3072, SDKS-3073]
- Fixed a `NullPointerException` in the centralized login flow. [SDKS-3079]
- Improved multi-threaded performance when caching access tokens. [SDKS-3104]
- Synchronized the encryption and decryption block to avoid keystore crashes. [SDKS-3199]
- Fixed an issue related to handling  `HiddenValueCallback` if `isMinifyEnabled` is set to `true`. [SDKS-3201]
- Fixed an issue where device binding using an application PIN was failing when Arabic language was used. [SDKS-3221]
- Fixed an issue where browser sessions were not properly signed out when a non-default browser was used in centralized login. [SDKS-3276]
- Fixed an unexpected behavior in the authentication flow caused by `AppAuthConfiguration` settings being ignored during centralized login. [SDKS-3277]
- Fixed the `FRUser.revokeAccessToken()` method to not end the user's session during the centralized login flow. [SDKS-3282]

## [4.4.0]
#### Added
- Added `TextInput` callback support [SDKS-545]
- Added a new module for integration with `PingOne Protect` [SDKS-2900]
- Added interface allowing developers to customize the biometric prompt for device binding\signing [SDKS-2991]
- Added immutable HTTP headers on each request `x-requested-with: forgerock-sdk` and `x-requested-platform: android` [SDKS-3033]

#### Fixed
- Addressed `nimbus-jose-jwt:9.25` library security vulnerability (CVE-2023-52428) [SDKS-2988]
- NullPointerException for Centralize Login, Replace deprecated onActivityResult with ActivityResultContract [SDKS-3079]

## [4.3.1]
#### Fixed
- Fixed an issue where the SDK was crashing during device binding on Android 9 devices [SDKS-2948]

## [4.3.0]
#### Added
- Added the ability to customize cookie headers in outgoing requests from the SDK [SDKS-2780]
- Added the ability to insert custom claims when performing device signing verification [SDKS-2787]
- Added client-side support for the `AppIntegrity` callback [SDKS-2631]

#### Fixed
- The SDK now uses `auth-per-use` keys for Device Binding [SDKS-2797]
- Improved handling of WebAuthn cancellations [SDKS-2819]
- Made `forgerock_url`, `forgerock_realm`, and `forgerock_cookie_name` params mandatory when dynamically configuring the SDK [SDKS-2782]
- Addressed `woodstox-core:6.2.4` library security vulnerability (CVE-2022-40152) [SDKS-2751]

## [4.2.0]
#### Added
- Gradle 8 and JDK 17 support  [SDKS-2451]
- Android 14 support [SDKS-2636]
- Key pair verification with key attestation during device binding enrollment [SDKS-2412]
- Added `iat` and `nbf` claims in the Device Binding and Device Signed JWT [SDKS-2747]

## [4.1.0]
#### Added
- Interceptor support for the Authenticator module [SDKS-2544]
- Interface for access_token refresh [SDKS-2567]
- Ability to process new JSON format of IG policy advice [SDKS-2240]

#### Fixed
- Fixed an issue on parsing `issuer` from combined MFA registration uri [SDKS-2542]
- Added error message about duplicated accounts while performing combined MFA registration [SDKS-2627]
- Fixed an issue related to "lost" WebAuthn credentials upon upgrade from 4.0.0-beta4 to newer version [SDKS-2576]

## [4.0.0]
#### Added
- Upgrade Google Fido Client to support PassKey [SDKS-2243]
- FRWebAuthn interface to remove WebAuthn Reference Keys [SDKS-2272]
- Interface to set Device Name during WebAuthn Registration [SDKS-2296]
- `DeviceBinding` callback support [SDKS-1747]
- `DeviceSigningVerifier` callback support [SDKS-2022]
- Support for combined MFA in the Authenticator SDK [SDKS-2166]
- Support for policy enforcement in the Authenticator SDK [SDKS-2166]

#### Fixed
- Fix for WebAuthn authentication for devices which use full screen biometric prompt [SDKS-2340]
- Fixed functionality for NetworkCollector [SDKS-2445]

#### Changed
- `public void WebAuthnRegistrationCallback.register(Node node,FRListener<Void> listener)` to `suspend fun register(context: Context, node: Node)`
- `public void WebAuthAuthenticationCallback.authenticate(@NonNull Fragment fragment, @NonNull Node node, @Nullable WebAuthnKeySelector selector, FRListener<Void> listener)` to `suspend fun authenticate(context: Context, node: Node, selector: WebAuthnKeySelector = WebAuthnKeySelector.DEFAULT)`
- `FRAClient.updateAccount` now throws `AccountLockException` upon attempt to update a locked account [SDKS-2166]
- `OathMechanism.getOathTokenCode()`, `HOTPMechanism.getOathTokenCode()` and `TOTPMechanism.getOathTokenCode()`  now throws `AccountLockException` upon attempt to get an OATH token for a locked account [SDKS-2166]

#### Deprecated
- Removed support for native single sign-on (SSO) [SDKS-2260], [SDKS-1367]

## [3.4.0]
#### Added
- Dynamic SDK Configuration [SDKS-1759]
- Android 13 support. [SDKS-1944]

#### Fixed
- Changed Activity type used as parameter in `PushNotification.accept`. [SDKS-1968]
- Deserializing an object with whitelist to prevent deserialization of untrusted data. [SDKS-1818]
- Updated the `Authenticator` module and sample app to handle the new `POST_NOTIFICATIONS` permission in Android 13. [SDKS-2033]
- Fixed issue where the `DefaultTokenManager` was not caching the `AccessToken` in memory upon retrieval from Shared Preferences. [SDKS-2066]
- Deprecated the `forgerock_enable_cookie` configuration [SDKS-2069]
- Align `forgerock_logout_endpoint` configuration name with the ForgeRock iOS SDK [SDKS-2085]
- Allow leading slash on custom endpoint path [SDKS-2074]
- Fixed bug where `state` parameter value was not being verified upon calling the `Authorize` endpoint [SDKS-2078]

## [3.3.3]
#### Fixed
- Bumped the version of the com.squareup.okhttp3 library to 4.10.0 [SDKS-1957]

## [3.3.2]
#### Added
- Interface for log management [SDKS-1864]

## [3.3.0]
#### Added
- Support SSL Pinning [SDKS-80]
- Restore SSO Token when it is out of sync with the SSO Token that bound with the Access Token [SDKS-1664]
- SSO Token should be included in the header instead of request parameter for /authorize endpoint [SDKS-1670]
- Support to broadcast logout event to clear application tokens when user logout the app [SDKS-1663]
- Obtain timestamp from new PushNotification payload [SDKS-1666]
- Add new payload attributes to the PushNotification [SDKS-1776]
- Allow processing of Push Notifications without device token [SDKS-1844]

#### Fixed
- Dispose AuthorizationService when no longer required [SDKS-1636]
- Authenticator sample app crash after scan push mechanism [SDKS-1454]

## [3.2.0]
#### Added
- Google Sign-In Security Enhancement [SDKS-1255]
- WebAuthn Registration & Authentication prompt not shown on second invocation on Single Activity App [SDKS-1297]
  
#### Fixed
- AbstractValidatedCallback is not serializable [SDKS-1486]
- Provide Build-in Binary Protection to avoid Memory Corruption Attack [SDKS-1368]

## [3.1.2]
#### Added
- Disable native SSO if failed to access Android AccountManager [SDKS-1304]

## [3.1.1]
#### Added
- Introduce `FRLifecycle` and exposed interfaces to allow custom Native SSO implementation. [SDKS-1140]
- Unlock device is not required for data decryption. [SDKS-1141]
- Support Android 12. [SDKS-1141]

## [3.0.0]
#### Added
- Social Login support for Google and Facebook
- Biometric Authentication with WebAuthn
- Exposed Revoke access token method [SDKS-980] - 'FRUser.getCurrentUser().revokeAccessToken(Listener)'
- Support Apple SignIn
- Remove deprecated methods (Config.getInstance(Context), FRAuth Builder, FRUserViewModel)

## [2.2.0]
#### Added
- Centralize Login (`AppAuth` Integration) [SDKS-330]

#### Fixed
- Refresh Token is not persisted when refresh_token grant is not issuing new Refresh Token [SDKS-649]
- org.forgerock.android.auth.FRUser.getAccessToken() clean up tokens in the following conditions: [SDKS-701]
-- When Refresh Token Grant Types is used, Server returns invalid_grant (Refresh Token expired), and failed to acquire OAuth2 Tokens with Session Token
-- When Refresh Token Grant Types is not used, and failed to acquire OAuth2 Tokens with Session Token
- Properly cache and reuse OKHttpClient [SDKS-770]
- Fix HostOnly Cookie handling [SDKS-808]

## [2.1.0]

#### Added
- Support NumberAttributeInputCallback [SDKS-495]
- Support BooleanAttributeInputCallback [SDKS-497]
- Access to the Page Node's header and description property [SDKS-518]
- Support Email Suspend Node [SDKS-505]
- Security Enhancement for Android 28+ Device [SDKS-571]

## [2.0.0]

#### Added
- `Set Persistent Cookie Node` is now supported to persist and manage Cookie [SDKS-182]
- `Device Profile Collector Node` is now supported [SDKS-293]
- `MetadataCallback` is now supported. For AM 6.5.2, when `MetadataCallback` is returned with stage value, SDK automatically parses `MetadataCallback` into Node's stage property. [SDKS-305]
- Allow server url paths to be configurable, Custom URL paths can be configured through `String.xml` or `ServerConfig` [SDKS-307]
- Support `Authentication by Server` and `Transaction Authenticate to Tree` in Policy Environment. [SDKS-88]
- Interface alignment with other platforms and introduce FRSession to authenticate against Authentication Tree in AM, persist and manage Session Token [SDKS-177]
- Allow developers to customize SDK outbound request, for example customize url to provide query parameters or adding/removing headers [SDKS-308]
- Allow developers to configure the cookie name [SDKS-364]
- New `forgerock-authenticator` module added to the SDK. This module allows developers to easily incorporate One-Time Password and Push Authentication capabilities in their apps [SDKS-225] 

#### Changed
- `FRUser.login` & `FRUser.register` now throws `AlreadyAuthenticatedException` when there is already authenticated user sessions [SDKS-177] 
- When Session Token is updated through `FRSession.authenticate` or `FRUser.login`, previously granted OAuth2 token set will automatically be revoked. [SDKS-177]
- Rename device browser `agent` attribute to `userAgent` for `FRDevice` [SDKS-371]

#### Fixed
- Fix Instrument Test. [SDKS-162]
- Fix Refresh of Access Token with Threshold not working consistently. [SDKS-476]

#### Deprecated
- `FRAuth.next()` is now deprecated, use `FRSession.authenticate` instead [SDKS-177] 

## [1.0.0]
- General Availability release for SDKs

#### Changed
- Changed OAuth2 authorization request to POST [SDKS-125]
- Store SSO token even SSO is disabled [SDKS-166]

## [0.9.0]

#### Added
- Initial release for forgerock-auth sdk
- Initial release for forgerock-auth-ui sdk
