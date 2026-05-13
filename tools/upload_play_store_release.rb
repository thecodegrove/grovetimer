#!/usr/bin/env ruby
# Uploads a signed Android App Bundle to a Google Play track using ADC credentials.

require "google/apis/androidpublisher_v3"
require "googleauth"

PACKAGE_NAME = "com.thecodegrove.grovetimer"
AAB_PATH = ENV.fetch("PLAY_AAB_PATH", "app/build/outputs/bundle/release/app-release.aab")
TRACK = ENV.fetch("PLAY_TRACK", "alpha")
RELEASE_STATUS = ENV.fetch("PLAY_RELEASE_STATUS", "completed")
SCOPE = "https://www.googleapis.com/auth/androidpublisher"

unless File.exist?(AAB_PATH)
  raise "AAB not found at #{AAB_PATH}"
end

service = Google::Apis::AndroidpublisherV3::AndroidPublisherService.new
service.authorization = Google::Auth.get_application_default([SCOPE])

edit = service.insert_edit(PACKAGE_NAME)
edit_id = edit.id
puts "Created Google Play edit #{edit_id}"

begin
  bundle = service.upload_edit_bundle(
    PACKAGE_NAME,
    edit_id,
    ack_bundle_installation_warning: true,
    upload_source: AAB_PATH,
    content_type: "application/octet-stream"
  )
  version_code = bundle.version_code
  puts "Uploaded AAB version code #{version_code}"

  release = Google::Apis::AndroidpublisherV3::TrackRelease.new(
    name: "GroveTimer #{ENV.fetch("GROVETIMER_VERSION_CODE", version_code)}",
    status: RELEASE_STATUS,
    version_codes: [version_code]
  )
  track = Google::Apis::AndroidpublisherV3::Track.new(
    track: TRACK,
    releases: [release]
  )

  service.update_edit_track(PACKAGE_NAME, edit_id, TRACK, track)
  puts "Assigned version code #{version_code} to #{TRACK} with status #{RELEASE_STATUS}"

  if ENV["PLAY_VALIDATE_ONLY"] == "true"
    service.validate_edit(PACKAGE_NAME, edit_id)
    puts "Validated Google Play release edit #{edit_id}"
  else
    service.commit_edit(PACKAGE_NAME, edit_id)
    puts "Committed Google Play release edit #{edit_id}"
  end
rescue StandardError
  warn "Google Play release upload failed: #{$!.class}: #{$!.message}"
  warn $!.body if $!.respond_to?(:body) && $!.body
  service.delete_edit(PACKAGE_NAME, edit_id) if edit_id
  raise
end
