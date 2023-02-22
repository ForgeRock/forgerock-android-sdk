[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/ForgeRock/forgerock-android-sdk?color=%23f46200&label=Version&style=flat-square)](CHANGELOG.md)
[![Build Status](https://github.com/ForgeRock/forgerock-android-sdk/actions/workflows/ci.yaml/badge.svg)](https://github.com/ForgeRock/forgerock-android-sdk/actions/workflows/ci.yaml)

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

* Android API level 23+
    * Android 6.0 (Marshmallow), 7.0 (Nougat), 8.0 (Oreo), 9.0 (Pie), 10.0, 11.0, 12.0, 13.0

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

* Introducing the SDK Features
* Preparing AM for use with the SDKS

### Build API Reference Documentation

You can build the API reference documentation, which uses Dokka to generate either Javadoc or HTML output.

HTML
: `./gradlew clean dokkaHtmlMultiModule` 
: View the output at [`build/api-reference/html/`](build/api-reference/html/index.html).

JavaDoc
: `./gradlew clean dokkaJavadocCollector`
: View the output at [`build/api-reference/javadoc/`](build/api-reference/javadoc/index.html).

> **TIP**: Use the following command to build both HTML and JavaDoc: 
> 
>`./gradlew clean dokkaHtmlMultiModule dokkaJavadocCollector`

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

> **This code is provided by ForgeRock on an “as is” basis, without warranty of any kind, to the fullest extent permitted by law. 
>ForgeRock does not represent or warrant or make any guarantee regarding the use of this code or the accuracy, 
>timeliness or completeness of any data or information relating to this code, and ForgeRock hereby disclaims all warranties whether express, 
>or implied or statutory, including without limitation the implied warranties of merchantability, fitness for a particular purpose, 
>and any warranty of non-infringement. ForgeRock shall not have any liability arising out of or related to any use, 
>implementation or configuration of this code, including but not limited to use for any commercial purpose. 
>Any action or suit relating to the use of the code may be brought only in the courts of a jurisdiction wherein 
>ForgeRock resides or in which ForgeRock conducts its primary business, and under the laws of that jurisdiction excluding its conflict-of-law provisions.**

<!------------------------------------------------------------------------------------------------------------------------------------>
<!-- LICENSE - Links to the MIT LICENSE file in each repo. -->

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

---

&copy; Copyright 2020 ForgeRock AS. All Rights Reserved

[forgerock-logo]: https://www.forgerock.com/themes/custom/forgerock/images/fr-logo-horz-color.svg "ForgeRock Logo"
