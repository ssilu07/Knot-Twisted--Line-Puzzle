import os
import sys
import json
import argparse
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

DEFAULT_PACKAGE_NAME = "com.tangledline.game"
DEFAULT_LOCALE = "en-US"

def get_credentials(key_input):
    scopes = ["https://www.googleapis.com/auth/androidpublisher"]
    
    # Check if input is a JSON file path
    if os.path.isfile(key_input):
        return service_account.Credentials.from_service_account_file(key_input, scopes=scopes)
    
    # Try parsing as JSON string directly
    try:
        data = json.loads(key_input)
        return service_account.Credentials.from_service_account_info(data, scopes=scopes)
    except Exception as e:
        raise ValueError(f"Could not load service account from provided file or JSON string: {e}")

def read_listing_files(base_dir, locale):
    locale_dir = os.path.join(base_dir, "playstore", "listing", locale)
    if not os.path.exists(locale_dir):
        # Fallback to en-US if locale directory does not exist
        locale_dir = os.path.join(base_dir, "playstore", "listing", "en-US")

    title_path = os.path.join(locale_dir, "title.txt")
    short_desc_path = os.path.join(locale_dir, "short_description.txt")
    full_desc_path = os.path.join(locale_dir, "full_description.txt")

    if not os.path.exists(title_path) or not os.path.exists(short_desc_path) or not os.path.exists(full_desc_path):
        raise FileNotFoundError(f"Missing listing files in {locale_dir}")

    with open(title_path, "r", encoding="utf-8") as f:
        title = f.read().strip()
    with open(short_desc_path, "r", encoding="utf-8") as f:
        short_desc = f.read().strip()
    with open(full_desc_path, "r", encoding="utf-8") as f:
        full_desc = f.read().strip()

    return title, short_desc, full_desc

def main():
    parser = argparse.ArgumentParser(description="Update Google Play Store listing metadata automatically")
    parser.add_argument("--key", help="Path to Google Cloud Service Account JSON key or JSON string")
    parser.add_argument("--package", default=DEFAULT_PACKAGE_NAME, help="Android package name")
    parser.add_argument("--locale", default=DEFAULT_LOCALE, help="Listing locale (default: en-US)")
    parser.add_argument("--upload-screenshots", action="store_true", help="Upload screenshots from playstore/screenshots/")
    args = parser.parse_args()

    key_input = args.key or os.environ.get("SERVICE_ACCOUNT_JSON") or os.environ.get("PLAY_STORE_JSON_KEY")
    if not key_input:
        print("ERROR: Service account key not provided. Use --key argument or set SERVICE_ACCOUNT_JSON environment variable.")
        sys.exit(1)

    package_name = args.package or os.environ.get("PACKAGE_NAME", DEFAULT_PACKAGE_NAME)
    locale = args.locale

    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, ".."))

    print(f"Loading listing files for locale [{locale}] from project root: {project_root}")
    title, short_desc, full_desc = read_listing_files(project_root, locale)

    print(f"Title ({len(title)} chars): {title}")
    print(f"Short Description ({len(short_desc)} chars): {short_desc}")
    print(f"Full Description ({len(full_desc)} chars)")

    print(f"Authenticating with Google Play Developer API for package: {package_name}...")
    credentials = get_credentials(key_input)
    service = build("androidpublisher", "v3", credentials=credentials)

    print("Creating new edit session on Google Play Console...")
    edit_request = service.edits().insert(body={}, packageName=package_name)
    edit = edit_request.execute()
    edit_id = edit["id"]
    print(f"Created edit ID: {edit_id}")

    print(f"Updating store listing for language [{locale}]...")
    listing_body = {
        "title": title,
        "shortDescription": short_desc,
        "fullDescription": full_desc
    }

    service.edits().listings().update(
        packageName=package_name,
        editId=edit_id,
        language=locale,
        body=listing_body
    ).execute()
    print("Store listing successfully staged.")

    if args.upload_screenshots:
        screenshots_dir = os.path.join(project_root, "playstore", "screenshots")
        if os.path.exists(screenshots_dir):
            screenshot_files = sorted([
                os.path.join(screenshots_dir, f) for f in os.listdir(screenshots_dir)
                if f.lower().endswith((".png", ".jpg", ".jpeg"))
            ])
            if screenshot_files:
                print(f"Uploading {len(screenshot_files)} screenshots for [{locale}]...")
                service.edits().images().deleteall(
                    packageName=package_name, editId=edit_id, language=locale, imageType="phoneScreenshots"
                ).execute()
                for sf in screenshot_files:
                    mimetype = "image/png" if sf.lower().endswith(".png") else "image/jpeg"
                    media = MediaFileUpload(sf, mimetype=mimetype)
                    service.edits().images().upload(
                        packageName=package_name,
                        editId=edit_id,
                        language=locale,
                        imageType="phoneScreenshots",
                        media_body=media
                    ).execute()
                    print(f"Uploaded screenshot: {os.path.basename(sf)}")

    print(f"Committing edit ID {edit_id} to Google Play Store...")
    commit_response = service.edits().commit(packageName=package_name, editId=edit_id).execute()
    print("COMMIT SUCCESSFUL! Google Play Store listing has been updated.")

if __name__ == "__main__":
    main()
