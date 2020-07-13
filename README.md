[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/ForgeRock/forgerock-android-sdk?color=%23f46200&label=Version&style=flat-square)](CHANGELOG.md)

<p align="center">
  <a href="https://github.com/ForgeRock">
    <img src="https://www.forgerock.com/themes/custom/forgerock/images/fr-logo-horz-color.svg" alt="Logo">
  </a>
  <h2 align="center">ForgeRock SDK for Android</h2>
  <p align="center">
    <a href="CHANGELOG.md">Change Log</a>
    ·
    <a href="#support">Support</a>
    ·
    <a href="#documentation">Docs</a>
  </p>
  <hr/>
</p>

The ForgeRock Android SDK enables you to quickly integrate the [ForgeRock Identity Platform](https://www.forgerock.com/digital-identity-and-access-management-platform) into your Android apps.

Use the SDKs to leverage _[Intelligent Authentication](https://www.forgerock.com/platform/access-management/intelligent-authentication)_ in [ForgeRock's Access Management (AM)](https://www.forgerock.com/platform/access-management) product, to easily step through each stage of an authentication tree by using callbacks.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- REQUIREMENTS - Supported AM versions, API versions, any other requirements. -->

## Requirements

* ForgeRock Identity Platform
    * Access Management (AM) 6.5.2+

* Android API level 21+
    * Android 5.0 (Lollipop), 6.0 (Marshmallow), 7.0 (Nougat), 8.0 (Oreo), 9.0 (Pie), 10.0

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

1. Setup an Access Management (AM) instance, as described in the **[Documentation](https://sdks.forgerock.com/android/01_prepare-am/)**.
2. Clone this repo:

    ```
    git clone https://github.com/ForgeRock/forgerock-android-sdk.git
    ```
3. Open the Android SDK project in [Android Studio](https://developer.android.com/studio).
4. Open `/app/src/main/res/values/strings.xml` and edit the values to match your AM instance.
5. On the **Run** menu, click **Run 'app'**.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- DOCS - Link off to the AM-centric documentation at sdks.forgerock.com. -->

## Documentation

Documentation for the SDKs is provided at **<https://sdks.forgerock.com>**, and includes topics such as:

* Introducting the SDK Features
* Preparing AM for use with the SDKS
* API Reference documentation

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- SUPPORT -->

## Support

If you encounter any issues, be sure to check our **[Troubleshooting](https://backstage.forgerock.com/knowledge/kb/article/a68547609)** pages.

Support tickets can be raised whenever you need our assistance; here are some examples of when it is appropriate to open a ticket (but not limited to):

* Suspected bugs or problems with ForgeRock software.
* Requests for assistance - please look at the **[Documentation](https://sdks.forgerock.com)** and **[Knowledge Base](https://backstage.forgerock.com/knowledge/kb/home/g32324668)** first.

You can raise a ticket using **[BackStage](https://backstage.forgerock.com/support/tickets)**, our customer support portal that provides one stop access to ForgeRock services. 

BackStage shows all currently open support tickets and allows you to raise a new one by clicking **New Ticket**.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- COLLABORATION -->

## Contributing

If you would like to contribute to this project you can fork the repository, clone it to your machine and get started.

<!-- Note: Found elsewhere, but is Java-only //-->
Be sure to check out our [Coding Style and Guidelines](https://wikis.forgerock.org/confluence/display/devcom/Coding+Style+and+Guidelines) page.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- LEGAL -->

## Disclaimer

> This code is provided on an “as is” basis, without warranty of any kind, to the fullest extent permitted by law.
>
> ForgeRock does not warrant or guarantee the individual success developers may have in implementing the code on their development platforms or in production configurations.
>
> ForgeRock does not warrant, guarantee or make any representations regarding the use, results of use, accuracy, timeliness or completeness of any data or information relating to this code.
>
> ForgeRock disclaims all warranties, expressed or implied, and in particular, disclaims all warranties of merchantability, and warranties related to the code, or any service or software related thereto.
>
> ForgeRock shall not be liable for any direct, indirect or consequential damages or costs of any type arising out of any action taken by you or others related to the code.

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- LICENSE - Links to the MIT LICENSE file in each repo. -->

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

&copy; Copyright 2020 ForgeRock AS. All Rights Reserved

[forgerock-logo]: https://www.forgerock.com/themes/custom/forgerock/images/fr-logo-horz-color.svg "ForgeRock Logo"
