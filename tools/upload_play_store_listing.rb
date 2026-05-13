#!/usr/bin/env ruby
# Uploads localized Google Play listing metadata and images using ADC credentials.

require "google/apis/androidpublisher_v3"
require "googleauth"

PACKAGE_NAME = "com.thecodegrove.grovetimer"
METADATA_PATH = File.expand_path("../fastlane/metadata/android", __dir__)
SCOPE = "https://www.googleapis.com/auth/androidpublisher"

IMAGE_TYPES = {
  "icon" => ["images/icon.png"],
  "featureGraphic" => ["images/featureGraphic.png"],
  "phoneScreenshots" => ["images/phoneScreenshots/*.png"]
}.freeze

def read_text(path)
  File.read(path, encoding: "UTF-8").strip
end

def locales
  Dir.children(METADATA_PATH)
     .select { |entry| File.directory?(File.join(METADATA_PATH, entry)) }
     .sort
end

service = Google::Apis::AndroidpublisherV3::AndroidPublisherService.new
service.authorization = Google::Auth.get_application_default([SCOPE])

edit = service.insert_edit(PACKAGE_NAME)
edit_id = edit.id
puts "Created Google Play edit #{edit_id}"

begin
  locales.each do |language|
    base = File.join(METADATA_PATH, language)
    listing = Google::Apis::AndroidpublisherV3::Listing.new(
      language: language,
      title: read_text(File.join(base, "title.txt")),
      short_description: read_text(File.join(base, "short_description.txt")),
      full_description: read_text(File.join(base, "full_description.txt"))
    )

    service.update_edit_listing(PACKAGE_NAME, edit_id, language, listing)
    puts "Updated listing text for #{language}"

    IMAGE_TYPES.each do |image_type, patterns|
      service.deleteall_edit_image(PACKAGE_NAME, edit_id, language, image_type)
      files = patterns.flat_map { |pattern| Dir[File.join(base, pattern)] }.sort
      files.each do |file|
        service.upload_edit_image(
          PACKAGE_NAME,
          edit_id,
          language,
          image_type,
          upload_source: file,
          content_type: "image/png"
        )
      end
      puts "Uploaded #{files.length} #{image_type} image(s) for #{language}"
    end
  end

  if ENV["PLAY_VALIDATE_ONLY"] == "true"
    service.validate_edit(PACKAGE_NAME, edit_id)
    puts "Validated Google Play edit #{edit_id}"
  else
    service.commit_edit(PACKAGE_NAME, edit_id)
    puts "Committed Google Play edit #{edit_id}"
  end
rescue StandardError
  warn "Google Play upload failed: #{$!.class}: #{$!.message}"
  warn $!.body if $!.respond_to?(:body) && $!.body
  service.delete_edit(PACKAGE_NAME, edit_id) if edit_id
  raise
end
