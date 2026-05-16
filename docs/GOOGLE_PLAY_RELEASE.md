# Google Play Release Setup

This repo is configured to publish GroveTimer to Google Play with Fastlane plus small direct Android Publisher API scripts.

## Recommended Path

Use Fastlane for building the signed Android App Bundle, then use the direct Android Publisher API scripts for uploading. This keeps Workload Identity Federation working without a long-lived service account JSON key.

The default workflow is conservative:

- Builds `app/build/outputs/bundle/release/app-release.aab`.
- Uploads to the `alpha` closed testing track by default.
- Uses `draft` release status while the app itself is still a draft in Play Console.
- Uploads localized store listing metadata, feature graphics, icons, and phone screenshots from `fastlane/metadata/android`.
- Uploads localized release notes from `fastlane/metadata/android/*/changelogs`.

## Automatic Main Releases

Every push to `main` runs `.github/workflows/google-play.yml`.

For normal merged changes, the workflow:

- Builds a signed release AAB.
- Uploads it to the Google Play `alpha` closed testing track.
- Uploads the current Play Store listing assets and screenshots.
- Uploads localized release notes for Catalan, English, and Spanish.
- Uses `draft` release status until Play Console allows non-draft releases for the app.
- Leaves production untouched.

This means the intended flow is:

1. Merge to `main`.
2. GitHub Actions publishes the build to closed testing automatically.
3. Test the closed testing build from Google Play.
4. Promote or manually upload to production only when ready.

## Other Options Considered

- Gradle Play Publisher: good Gradle-native alternative. It adds `publishBundle` tasks and defaults to the internal track, but it couples publishing to the Gradle build more tightly.
- Direct Google Play Developer API: most flexible, but more custom code and more maintenance.
- Third-party GitHub Actions: convenient, but they add another release-critical dependency. The current workflow keeps the release logic in this repo through Fastlane.

## One-Time Google Play Setup

Google Cloud is configured to use Workload Identity Federation instead of a long-lived JSON key.

Current Google Cloud values:

- Project ID: `thecodegrove`
- Project number: `187732555498`
- Service account: `grovetimer-play-publisher@thecodegrove.iam.gserviceaccount.com`
- Workload Identity Provider: `projects/187732555498/locations/global/workloadIdentityPools/github-actions/providers/github`

Manual Play Console steps that still need to happen:

1. In Google Play Console, invite `grovetimer-play-publisher@thecodegrove.iam.gserviceaccount.com` under Users and permissions.
2. Grant only the app permissions needed to upload releases for `com.thecodegrove.grovetimer`.
3. Upload at least one build manually in Play Console if this is the app's first ever Play upload.

Do not create or commit a service account JSON key unless Workload Identity Federation is unavailable.

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
export GROVETIMER_RELEASE_STORE_FILE="$(pwd)/app/release.keystore"
export GROVETIMER_RELEASE_STORE_PASSWORD='...'
export GROVETIMER_RELEASE_KEY_ALIAS='grovetimer'
export GROVETIMER_RELEASE_KEY_PASSWORD='...'
export GROVETIMER_VERSION_CODE=2
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/application-default-credentials.json
```

Then run:

```bash
bundle install
bundle exec fastlane android build_release
PLAY_VALIDATE_ONLY=true bundle exec ruby tools/upload_play_store_listing.rb
PLAY_VALIDATE_ONLY=true PLAY_TRACK=alpha bundle exec ruby tools/upload_play_store_release.rb
```

The GitHub Actions workflow uses direct Android Publisher API scripts with
Workload Identity/ADC because Fastlane `supply` expects service-account JSON
credentials for upload actions.

To change track/status:

```bash
PLAY_TRACK=beta PLAY_RELEASE_STATUS=draft bundle exec ruby tools/upload_play_store_release.rb
```

## Play Store Listing Assets

Localized metadata is stored in:

- `fastlane/metadata/android/es-ES`
- `fastlane/metadata/android/en-US`
- `fastlane/metadata/android/ca`

Each locale includes `title.txt`, `short_description.txt`, `full_description.txt`, `images/icon.png`, `images/featureGraphic.png`, and five stylized `images/phoneScreenshots/*.png`.

Regenerate listing assets after taking fresh emulator screenshots:

```bash
python3 tools/generate_play_store_assets.py
```

Upload only the listing, without uploading a build:

```bash
bundle exec ruby tools/upload_play_store_listing.rb
```

## Release Notes

Google Play release notes are uploaded with each release from:

- `fastlane/metadata/android/ca/changelogs`
- `fastlane/metadata/android/en-US/changelogs`
- `fastlane/metadata/android/es-ES/changelogs`

The upload script looks first for a file matching the uploaded Play version code, for example:

```text
fastlane/metadata/android/en-US/changelogs/42.txt
```

If there is no version-specific file, it falls back to:

```text
fastlane/metadata/android/en-US/changelogs/default.txt
```

Use `default.txt` for generic closed-testing notes, and add `<versionCode>.txt` files only when a release needs specific wording.

Release notes should be prepared during PR review, not after merging to `main`.
For every PR that changes user-visible app behavior, UI, permissions, Play Store
metadata, or release behavior, generate or propose short Play Store notes in all
supported listing languages:

- `ca`
- `en-US`
- `es-ES`

The notes should summarize what changed since the previous published version in
user-facing language. Avoid raw commit logs, implementation details, branch names,
or internal ticket references. If a PR has no user-visible release note, state
that explicitly in the PR description.

Automation should eventually generate a draft set of localized notes for each PR
from the PR title, body, labels, and changed files. Maintainers can then edit the
draft before merge. The release upload should keep using the committed changelog
files as the final source of truth.

## GitHub Actions Setup

The workflow lives at `.github/workflows/google-play.yml`.

It has two entry points:

- `push` to `main`: automatic closed testing upload with `PLAY_TRACK=alpha` and `PLAY_RELEASE_STATUS=draft` while the app is still a draft.
- `workflow_dispatch`: manual validation or manual upload to `internal`, `alpha`, `beta`, or `production`.

Add these repository secrets:

- `ANDROID_RELEASE_KEYSTORE_BASE64`: base64 content of the release keystore.
- `GROVETIMER_RELEASE_STORE_PASSWORD`
- `GROVETIMER_RELEASE_KEY_ALIAS`
- `GROVETIMER_RELEASE_KEY_PASSWORD`
- `GCP_WORKLOAD_IDENTITY_PROVIDER`: `projects/187732555498/locations/global/workloadIdentityPools/github-actions/providers/github`
- `GCP_SERVICE_ACCOUNT`: `grovetimer-play-publisher@thecodegrove.iam.gserviceaccount.com`

Generate the keystore secret:

```bash
base64 -i app/release.keystore | pbcopy
```

For first setup, run the workflow manually with `validate_only=true`. When validation passes, future merges to `main` can publish closed testing builds automatically.

For production, use the manual workflow and choose:

- `track=production`
- `release_status=draft` for a Play Console review step, or `completed` when you want it released directly.

## Sources

- Fastlane `upload_to_play_store`: https://docs.fastlane.tools/actions/upload_to_play_store/
- Google GitHub Actions auth: https://github.com/google-github-actions/auth
- Google Play Developer API setup: https://developers.google.com/android-publisher/getting_started
- Gradle Play Publisher: https://github.com/Triple-T/gradle-play-publisher
