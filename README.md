[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/ForgeRock/forgerock-android-sdk?color=%23f46200&label=Version&style=flat-square)](CHANGELOG.md)
[![Build Status](https://github.com/ForgeRock/forgerock-android-sdk/actions/workflows/ci.yaml/badge.svg)](https://github.com/ForgeRock/forgerock-android-sdk/actions/workflows/ci.yaml)
[![Coverage](https://codecov.io/gh/ForgeRock/forgerock-android-sdk/graph/badge.svg?token=PGfmkaLyIC)](https://codecov.io/gh/ForgeRock/forgerock-android-sdk)

<p align="center">
  <a href="https://github.com/ForgeRock">
    <img src="https://cdn-docs.pingidentity.com/navbar/ping-logo-horizontal.svg" alt="Logo">
  </a>
  <h2 align="center">Ping SDK for Android</h2>
  <p align="center">
    <a href="CHANGELOG.md">Change Log</a>
    ·
    <a href="#support">Support</a>
    ·
    <a href="#documentation">Docs</a>
  </p>
  <hr/>
</p>

The Ping SDK for Android enables you to quickly integrate Ping products into your Android apps.

Use the SDKs to leverage _[Intelligent Access](https://www.pingidentity.com/en/platform/capabilities/intelligent-access.html)_ to easily step through each stage of an authentication tree by using callbacks.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- REQUIREMENTS - Supported AM versions, API versions, any other requirements. -->

## Requirements

* ForgeRock Identity Platform
    * Ping Advanced Identity Cloud
    * PingAM 6.5.2+

* Android API level 23+
    * Android 6.0 (Marshmallow), 7.0 (Nougat), 8.0 (Oreo), 9.0 (Pie), 10.0, 11.0, 12.0, 13.0, 14.0, 15.0

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- INSTALLATION - If you want to start quickly with minimal assistance. -->

## Installation

```groovy
dependencies {
    implementation 'org.forgerock:forgerock-auth:<version>'
}
```

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- QUICK START - Get one of the included samples up and running in as few steps as possible. -->

## Getting Started

To try out the ForgeRock Android SDK sample, perform these steps:

1. Setup an Advanced Identity Cloud tenant or PingAM instance, as described in the **[Documentation](https://docs.pingidentity.com/sdks/latest/sdks/tutorials/android/00_before-you-begin.html#server_configuration)**.
2. Clone this repo:

    ```
    git clone https://github.com/ForgeRock/forgerock-android-sdk.git
    ```
3. Open the Android SDK project in [Android Studio](https://developer.android.com/studio).
4. Open `/app/src/main/res/values/strings.xml` and edit the values to match your server.
5. On the **Run** menu, click **Run 'app'**.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- DOCS - Link off to the AM-centric documentation at sdks.forgerock.com. -->

## Documentation

Documentation for the SDKs is provided at **<https://docs.pingidentity.com/sdks>**, and includes topics such as:

* Introducing the SDK Features
* Preparing AM for use with the SDKS

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- SUPPORT -->

## Support

If you encounter any issues, be sure to check our **[Troubleshooting](https://support.pingidentity.com/s/article/How-do-I-troubleshoot-the-ForgeRock-SDK-for-Android)** pages.

Support tickets can be raised whenever you need our assistance; here are some examples of when it is appropriate to open a ticket (but not limited to):

* Suspected bugs or problems with ForgeRock software.
* Requests for assistance - please look at the **[Documentation](https://docs.pingidentity.com/sdks)** and **[Knowledge Base](https://support.pingidentity.com/s/knowledge-base)** first.

You can raise a ticket using the **[Ping Identity Support Portal](https://support.pingidentity.com/s/)** that provides one stop access to support services. 

The support portal shows all currently open support tickets and allows you to raise a new one by clicking **New Ticket**.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- COLLABORATION -->

## Contributing

If you would like to contribute to this project, please see the [contributions guide](./CONTRIBUTION.md).

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- LEGAL -->

## Disclaimer

> **This code is provided by Ping Identity Corporation ("Ping") on an "as is" basis, without warranty of any kind, to the fullest extent permitted by law.
> Ping Identity Corporation does not represent or warrant or make any guarantee regarding the use of this code or the accuracy, timeliness or completeness of any data or information relating to this code, and Ping Identity Corporation hereby disclaims all warranties whether express, or implied or statutory, including without limitation the implied warranties of merchantability, fitness for a particular purpose, and any warranty of non-infringement.
> Ping Identity Corporation shall not have any liability arising out of or related to any use, implementation or configuration of this code, including but not limited to use for any commercial purpose.
> Any action or suit relating to the use of the code may be brought only in the courts of a jurisdiction wherein Ping Identity Corporation resides or in which Ping Identity Corporation conducts its primary business, and under the laws of that jurisdiction excluding its conflict-of-law provisions.**

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- LICENSE - Links to the MIT LICENSE file in each repo. -->

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

&copy; Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved

