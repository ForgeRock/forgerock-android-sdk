# Version 2.2.0

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
- `MetadataCallback` is now supported. For AM 6.5.2, when `MetadataCallback` is returned with stage value, SDK automatically parses `MetadataCallback` into Node's stage property. Please refer [this blog post](https://forum.forgerock.com/2020/02/using-an-authentication-tree-stage-to-build-a-custom-ui-with-the-forgerock-javascript-sdk/) for more details. [SDKS-305]
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
