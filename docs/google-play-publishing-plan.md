# Google Play publishing plan

Last reviewed: 2026-09-20

## Current state

- Package: `se.johannalynn.nexttram`
- Version: `1.0` (`versionCode` 1)
- SDK: minimum 35, target 37
- Permissions: internet only
- Release signing: not configured
- Release shrinking/obfuscation: disabled
- Store listing, privacy information, and release assets: not prepared

The target SDK meets Google Play's current requirement of API 36 or higher for new mobile apps. The minimum SDK limits the app to Android 15 and newer; confirm that this small device audience is intentional before launch.

## 1. Resolve release blockers

- [ ] Remove the Västtrafik client secret from `BuildConfig`. Values compiled into an Android app can be extracted by users.
- [ ] Put the client-credentials exchange behind a small backend, or switch to a Västtrafik-supported flow that does not require a secret in the app.
- [ ] Rotate the current Västtrafik secret before production if it has been included in any shared build.
- [ ] Confirm that `se.johannalynn.nexttram` is the final application ID. It cannot be changed for updates after publication.
- [ ] Decide whether `minSdk = 35` is intentional; lower it and test older Android versions if wider availability is wanted.
- [ ] Replace or approve the launcher icon and make the Swedish/English UI language consistent.
- [ ] Review the placeholder backup rules and explicitly include or exclude any data the app later persists.

## 2. Prepare a production build

- [ ] Create a long-lived upload keystore outside the repository and back it up securely.
- [ ] Keep the keystore path, alias, and passwords in an ignored local file or CI secrets.
- [ ] Add a release `signingConfig` to `app/build.gradle.kts`.
- [ ] Enroll in Play App Signing when the first bundle is uploaded; use the local key only as the upload key.
- [ ] Define the versioning rule: increase `versionCode` for every upload and use a user-facing `versionName`.
- [ ] Decide whether to enable R8 with `isMinifyEnabled = true`; if enabled, test the optimized release and retain the generated mapping file.
- [ ] Build the upload artifact with `./gradlew :app:bundleRelease` and confirm it is created at `app/build/outputs/bundle/release/app-release.aab`.

## 3. Verify release quality

- [ ] Replace the generated example tests with coverage for departure parsing, API failures, empty results, and platform selection.
- [ ] Run `./gradlew test lint :app:bundleRelease` with production-equivalent configuration.
- [ ] Install the release through Play's internal-testing track and test startup, refresh, settings, dark mode, rotation, offline/error behavior, and real Västtrafik responses.
- [ ] Test the supported phone and tablet layouts, accessibility labels, contrast, text scaling, and Swedish locale.
- [ ] Review the Play pre-launch report and resolve crashes, ANRs, compatibility, performance, and accessibility findings.
- [ ] Confirm the production build contains no secret, test endpoint, debug logging, or unintended user data.

## 4. Prepare the Play Console listing

- [ ] Create and verify the correct personal or organization developer account.
- [ ] Create the app in Play Console with package `se.johannalynn.nexttram`, default language, app name, app/game type, and free/paid status.
- [ ] Write the short description, full description, support email, and optional website.
- [ ] Prepare a 512 x 512 Play Store icon, a 1024 x 500 feature graphic, and representative phone screenshots; add tablet screenshots if tablets remain supported.
- [ ] Select the category, countries/regions, and distribution availability.
- [ ] Host a stable privacy-policy page that identifies the app, developer, data handling, Västtrafik service use, retention, sharing, security, and contact method.

## 5. Complete policy declarations

- [ ] Audit the final app and every dependency before answering Data safety. The current source appears to send only timetable requests and has no analytics or ads, but declarations must match the released bundle and backend behavior.
- [ ] Complete Data safety even if no user data is collected.
- [ ] Declare whether the app contains ads.
- [ ] Complete target audience, content rating, app access, and any other App content forms shown by Play Console.
- [ ] Ensure the listing and privacy policy accurately describe network requests and any server-side logs introduced by the credential proxy.
- [ ] Recheck the target API and policy requirements immediately before submission because Google updates them regularly.

## 6. Test and publish

- [ ] Upload the signed AAB to internal testing and add trusted testers.
- [ ] If this is a personal developer account created after 2023-11-13, run a closed test with at least 12 opted-in testers continuously for 14 days, then apply for production access.
- [ ] Fix tester and pre-launch-report findings, increment `versionCode`, rebuild, and upload the final candidate.
- [ ] Add release notes and submit the production release for review.
- [ ] Use managed publishing if the public launch time must be controlled after approval.
- [ ] After launch, monitor Android vitals, crashes, ANRs, reviews, backend health, and Västtrafik API usage.

## Release gate

Publish only when all of these are true:

- [ ] No secret is shipped in the AAB.
- [ ] The upload key is backed up and Play App Signing is enabled.
- [ ] The signed release bundle passes tests and works through a Play testing track.
- [ ] Store assets, privacy policy, Data safety, content rating, and audience declarations are complete and consistent.
- [ ] Required closed testing and production-access approval are complete.
- [ ] `versionCode` is unique and the production release notes are ready.

## Official references

- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [App signing and upload keys](https://developer.android.com/studio/publish/app-signing)
- [Prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348)
- [Prepare an app for review](https://support.google.com/googleplay/android-developer/answer/9859455)
- [Testing requirements for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Pre-launch reports](https://support.google.com/googleplay/android-developer/answer/9844487)
