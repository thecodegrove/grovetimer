# Google Play Release Setup

This repo is configured to publish GroveTimer to Google Play with Fastlane.

## Recommended Path

Use Fastlane `upload_to_play_store` as the main release path. It uploads the signed AAB through the Google Play Developer Publishing API and supports tracks such as `internal`, `alpha`, `beta`, and `production`.

The default lane is conservative:

- Builds `app/build/outputs/bundle/release/app-release.aab`.
- Uploads to the `internal` track by default.
- Uses `draft` release status by default.
- Skips metadata, screenshots, images, and changelogs for now.

## Automatic Main Releases

Every push to `main` runs `.github/workflows/google-play.yml`.

For normal merged changes, the workflow:

- Builds a signed release AAB.
- Uploads it to the Google Play `beta` track.
- Uses `completed` release status so testers can receive the build.
- Leaves production untouched.

This means the intended flow is:

1. Merge to `main`.
2. GitHub Actions publishes the build to beta automatically.
3. Test the beta build from Google Play.
4. Promote or manually upload to production only when ready.

## Other Options Considered

- Gradle Play Publisher: good Gradle-native alternative. It adds `publishBundle` tasks and defaults to the internal track, but it couples publishing to the Gradle build more tightly.
- Direct Google Play Developer API: most flexible, but more custom code and more maintenance.
- Third-party GitHub Actions: convenient, but they add another release-critical dependency. The current workflow keeps the release logic in this repo through Fastlane.

## One-Time Google Play Setup

1. Create or choose a Google Cloud project.
2. Enable the Google Play Developer API.
3. Create a service account.
4. In Google Play Console, invite the service account email under Users and permissions.
5. Grant only the app permissions needed to upload releases for `dev.thecodegrove.grovetimer`.
6. Download the service account JSON key and keep it secret.
7. Upload at least one build manually in Play Console if this is the app's first ever Play upload.

## Release Signing

Create a release keystore if you do not already have one:

```bash
keytool -genkeypair \
  -v \
  -keystore app/release.keystore \
  -alias grovetimer \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Never commit the keystore or passwords. They are ignored by `.gitignore`.

For a local release build, export:

```bash
export GROVETIMER_RELEASE_STORE_FILE=app/release.keystore
export GROVETIMER_RELEASE_STORE_PASSWORD='...'
export GROVETIMER_RELEASE_KEY_ALIAS='grovetimer'
export GROVETIMER_RELEASE_KEY_PASSWORD='...'
export GOOGLE_PLAY_JSON_KEY_FILE=fastlane/google-play-service-account.json
```

Then run:

```bash
bundle install
bundle exec fastlane android validate_play
bundle exec fastlane android play_internal
```

To change track/status:

```bash
PLAY_TRACK=beta PLAY_RELEASE_STATUS=draft bundle exec fastlane android play_internal
```

## GitHub Actions Setup

The workflow lives at `.github/workflows/google-play.yml`.

It has two entry points:

- `push` to `main`: automatic beta upload with `PLAY_TRACK=beta` and `PLAY_RELEASE_STATUS=completed`.
- `workflow_dispatch`: manual validation or manual upload to `internal`, `alpha`, `beta`, or `production`.

Add these repository secrets:

- `ANDROID_RELEASE_KEYSTORE_BASE64`: base64 content of the release keystore.
- `GROVETIMER_RELEASE_STORE_PASSWORD`
- `GROVETIMER_RELEASE_KEY_ALIAS`
- `GROVETIMER_RELEASE_KEY_PASSWORD`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`: raw service account JSON.

Generate the keystore secret:

```bash
base64 -i app/release.keystore | pbcopy
```

For first setup, run the workflow manually with `validate_only=true`. When validation passes, future merges to `main` can publish beta builds automatically.

For production, use the manual workflow and choose:

- `track=production`
- `release_status=draft` for a Play Console review step, or `completed` when you want it released directly.

## Sources

- Fastlane `upload_to_play_store`: https://docs.fastlane.tools/actions/upload_to_play_store/
- Google Play Developer API setup: https://developers.google.com/android-publisher/getting_started
- Gradle Play Publisher: https://github.com/Triple-T/gradle-play-publisher
