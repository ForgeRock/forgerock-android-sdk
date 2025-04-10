# Contribution Guidelines for Ping SDK for Android

Welcome! We are excited that you are interested in contributing to the **Ping SDK for Android**. 
This document will guide you through the steps required to contribute to the project.

We appreciate your help in making the Ping SDK for Android better for everyone.

## 1. Setting Up Your Development Environment

Before you can contribute to the Ping SDK for Android, you'll need to set up your development 
environment. This section describes the prerequisites and steps needed to start using the project 
in your local machine.

### Prerequisites

1. GitHub account.
2. Git installed.
3. Latest version of [Android Studio](https://developer.android.com/studio).
4. Android API level 23+
5. Setup an Advanced Identity Cloud tenant or PingAM instance, as described in the **[Documentation](https://docs.pingidentity.com/sdks/latest/sdks/tutorials/android/00_before-you-begin.html#server_configuration)**.

### Fork and Clone the Repository

1. Fork the repository to your own GitHub account by clicking the "Fork" button at the top-right of the repository page.
 
2. Clone your forked repository to your local machine:
   ```bash
   git clone https://github.com/your-username/forgerock-android-sdk.git
   ```

3. Navigate into the project folder:
   ```bash
    cd forgerock-android-sdk
   ```
   
4. Build the project to make sure it works:

   ```bash
    ./gradlew clean build
   ```

### Understanding the Project Structure

The Ping SDK for Android is organized in a modular way. This section is designed to help you 
understand the layout of the project. We will break down each of the folders and what modules you 
will find there. Getting familiar with the project structure will make contributing easier and 
more efficient.

    forgerock-android-sdk
     |── e2e                               # Contains the sample app for end-to-end tests
     |── forgerock-auth                    # Provides the OIDC client and integrates with Journey
     |── forgerock-auth-ui*                # Contains UI components for rapid prototype apps with Journey
     |── forgerock-authenticator           # Allows to add Push and OATH mechanisms to the app
     |── forgerock-core                    # Provide common functions for all modules
     |── forgerock-integration-tests       # Includes tests for the SDK
     |── ping-protect                      # Provide access to the Ping Protect API
     ├── ...
     └── ...

***Note***: * Module deprecated

* **e2e** : This folder houses the sample application used for end-to-end (E2E) testing of the SDK. E2E tests 
verify that the entire system works as expected from the user's perspective, covering interactions 
across multiple SDK components. This sample application provides a practical example of how to 
integrate and utilize the SDK's various functionalities in a real-world scenario.

* **forgerock-auth**: The forgerock-auth module is at the core of the SDK's authentication capabilities. It provides a 
robust OpenID Connect (OIDC) client implementation, enabling seamless user authentication and 
authorization. This module also integrates with ForgeRock's Journey framework, allowing developers
to implement complex authentication flows and user journeys. Some features such as WebAuthn, 
Social Login and many others are also included in this module.

* **forgerock-auth-ui**: The forgerock-auth-ui module provides a collection of pre-built UI components designed to expedite 
the development of prototype applications. These UI elements integrate directly with the 
forgerock-auth module and are specifically designed to facilitate rapid prototyping with the 
Journey framework. This module is deprecated.

* **forgerock-authenticator**: This module empowers you to add powerful multi-factor authentication (MFA) capabilities to an 
application. It includes support for Push notifications and Time-based One-Time Passwords (TOTP) 
mechanisms (OATH), enabling you to enhance security and protect users' accounts.

* **forgerock-core**: The forgerock-core module acts as the foundational layer for all other modules within the SDK. It 
provides a set of common functions and utilities that are shared across the SDK, ensuring 
consistency and reducing code duplication.

* **forgerock-integration-tests**: This module is dedicated to the SDK's integration tests. Integration tests verify that different 
modules within the SDK work correctly together and that they function as expected when interacting 
with external systems or APIs.

### Running Tests

Unit testing is essential for software development. It ensures individual code components work 
correctly, catches bugs early, and improves code reliability. This section explains how to run unit 
tests for each SDK module.

#### Execute unit tests for forgerock-core

   ```bash
    ./gradlew :forgerock-core:testDebugUnitTestCoverage --stacktrace --no-daemon
   ```

#### Execute unit tests for forgerock-auth

   ```bash
    ./gradlew :forgerock-auth:testDebugUnitTestCoverage --stacktrace --no-daemon
   ```

#### Execute unit tests for forgerock-authenticator

   ```bash
    ./gradlew :forgerock-authenticator:testDebugUnitTestCoverage --stacktrace --no-daemon
   ```

#### Execute unit tests for ping-protect

   ```bash
    ./gradlew :ping-protect:testDebugUnitTestCoverage --stacktrace --no-daemon
   ```

### Build API Reference Documentation

Comprehensive and accurate API reference documentation is essential for developers working with the
Ping SDK for Android. It serves as the definitive guide to the SDK's classes, methods, and
functions, enabling you to quickly understand how to utilize its capabilities effectively.
This section outlines the process for generating API reference documentation directly from the
source code.

You can build the API reference documentation, which uses Dokka to generate either JavaDoc or HTML 
output, using the following commands:

HTML
: `./gradlew clean dokkaHtmlMultiModule`
: View the output at [`build/api-reference/html/`](build/api-reference/html/index.html).

JavaDoc
: `./gradlew clean dokkaJavadocCollector`
: View the output at [`build/api-reference/javadoc/`](build/api-reference/javadoc/index.html).

> **TIP**: Use the following command to build both HTML and JavaDoc:
>
>`./gradlew clean dokkaHtmlMultiModule dokkaJavadocCollector`

## 2. Standards of Practice

We ask that all contributors to this project adhere to our engineering Standard for team culture, practices and code of conduct. We expect everyone to be respectful, inclusive, and collaborative. Any violations will be handled according to the project's guidelines.

For more details on our Standards of Practice, please refer to the [SDK Standards of Practice](https://github.com/ForgeRock/sdk-standards-of-practice) documentation.

## 3. Creating a Pull Request (PR)

This section covers how to create your changes, and submit them for review by Ping Identity engineers 
by using a Pull Request. A PR is a formal request to merge your changes from your forked repository into 
the main project. The following steps will guide you on creating a well-structured PR that 
facilitates efficient review and integration of your contributions.

### 1. Create a New Branch
   Always create a new branch to work on your changes. Avoid making changes directly on the `develop` or `master` branch.

   ```bash
   git checkout -b feature/my-new-feature
   ```
   
### 2. Make Your Changes
Implement the required changes or new features. Make sure to write clean, well-commented, and readable code. If applicable, include tests and documentation for the new functionality.

### 3. Commit Your Changes
Once you’ve made your changes, commit them with a clear and descriptive message. Note that our 
repository requires all commits to be signed. For more information on signing commits, please refer to
the [GitHub Docs](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits)

The commit message should follow this structure:

```
[TYPE] Short description of the changes
```
Types:

* `feat`: A new feature
* `fix`: A bug fix
* `docs`: Documentation changes
* `refactor`: Code refactoring (no feature change)
* `test`: Adding or modifying tests

Example:

   ```bash
   git commit -S -m "feat: add login functionality"
   ```

### 4. Push Your Changes
After committing your changes, push them to your fork:

   ```bash
    git push origin feature/my-new-feature
   ```

### 5. Create a Pull Request

1. Go to your fork on GitHub.

2. Click on the "New Pull Request" button.

3. Select the base repository and base branch (usually `develop`), then select the branch you just pushed.

4. Fill out the PR Template

   Make sure to fill out the PR template provided. The template helps us better understand your change. Typically, a PR will require the following information:

   * Add a title and description for the PR. The description should include:
     * What was changed and why.
     * Any related issues.
     * Any additional context if necessary, for example relevant screenshots or breaking changes. 
   
   Once everything looks good, submit the PR for review.

### 6. PR Review and Feedback

Once the PR is submitted, the team will review it. Be prepared to:

* Address any feedback or requested changes.
* Keep your branch up to date with the base branch by rebasing or merging.

## 4. Additional Notes

* **Testing:** Please ensure that your code is well-tested. If your changes introduce new features or bug fixes, add appropriate tests to verify the behavior.

* **Documentation**: Update relevant API documentation to reflect any new features or changes to existing functionality.

* **Style Guide**: Please follow the [coding style guide](https://github.com/ForgeRock/sdk-standards-of-practice/blob/main/code-style/android-coding-standard.md) for the language you are working with.

Thank you for contributing to Ping SDK for Android! Your contributions help make the project better for everyone.

If you have any questions, feel free to reach out via the Issues or Discussions section of the repository.

&copy; Copyright 2025 Ping Identity Corporation. All Rights Reserved