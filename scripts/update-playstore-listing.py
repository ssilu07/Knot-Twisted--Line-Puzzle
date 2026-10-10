import os
import sys
import json
import argparse
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

DEFAULT_PACKAGE_NAME = "com.tangledline.game"
DEFAULT_LOCALE = "all"

def get_credentials(key_input):
    scopes = ["https://www.googleapis.com/auth/androidpublisher"]
    
    if os.path.isfile(key_input):
        return service_account.Credentials.from_service_account_file(key_input, scopes=scopes)
    
    try:
        data = json.loads(key_input)
        return service_account.Credentials.from_service_account_info(data, scopes=scopes)
    except Exception as e:
        raise ValueError(f"Could not load service account from provided file or JSON string: {e}")

def read_listing_files(base_dir, locale):
    locale_dir = os.path.join(base_dir, "playstore", "listing", locale)
    if not os.path.exists(locale_dir):
        locale_dir = os.path.join(base_dir, "playstore", "listing", "en-US")
    if not os.path.exists(locale_dir):
        locale_dir = os.path.join(base_dir, "playstore", "listing", "en-GB")

    title_path = os.path.join(locale_dir, "title.txt")
    short_desc_path = os.path.join(locale_dir, "short_description.txt")
    full_desc_path = os.path.join(locale_dir, "full_description.txt")

    if not os.path.exists(title_path) or not os.path.exists(short_desc_path) or not os.path.exists(full_desc_path):
        raise FileNotFoundError(f"Missing listing files for {locale} in {locale_dir}")

    with open(title_path, "r", encoding="utf-8") as f:
        title = f.read().strip()
    with open(short_desc_path, "r", encoding="utf-8") as f:
        short_desc = f.read().strip()
    with open(full_desc_path, "r", encoding="utf-8") as f:
        full_desc = f.read().strip()

    return title, short_desc, full_desc

def main():
    parser = argparse.ArgumentParser(description="Update Google Play Store listing metadata and screenshots automatically")
    parser.add_argument("--key", help="Path to Google Cloud Service Account JSON key or JSON string")
    parser.add_argument("--package", default=DEFAULT_PACKAGE_NAME, help="Android package name")
    parser.add_argument("--locale", default=DEFAULT_LOCALE, help="Listing locale (default: all)")
    parser.add_argument("--upload-screenshots", action="store_true", help="Upload screenshots from playstore/screenshots/")
    args = parser.parse_args()

    key_input = args.key or os.environ.get("SERVICE_ACCOUNT_JSON") or os.environ.get("PLAY_STORE_JSON_KEY")
    if not key_input:
        print("ERROR: Service account key not provided. Use --key argument or set SERVICE_ACCOUNT_JSON environment variable.")
        sys.exit(1)

    package_name = args.package or os.environ.get("PACKAGE_NAME", DEFAULT_PACKAGE_NAME)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, ".."))

    print(f"Authenticating with Google Play Developer API for package: {package_name}...")
    credentials = get_credentials(key_input)
    service = build("androidpublisher", "v3", credentials=credentials)

    print("Creating new edit session on Google Play Console...")
    edit_request = service.edits().insert(body={}, packageName=package_name)
    edit = edit_request.execute()
    edit_id = edit["id"]
    print(f"Created edit ID: {edit_id}")

    # Determine target locales
    if args.locale == "all":
        try:
            listings_res = service.edits().listings().list(packageName=package_name, editId=edit_id).execute()
            target_locales = [l.get("language") for l in listings_res.get("listings", [])]
        except Exception:
            target_locales = ["en-GB", "en-US"]
        if not target_locales:
            target_locales = ["en-GB", "en-US"]
    else:
        target_locales = [args.locale]

    print(f"Target locales to update: {target_locales}")

    screenshots_dir = os.path.join(project_root, "playstore", "screenshots")
    screenshot_files = []
    if args.upload_screenshots and os.path.exists(screenshots_dir):
        screenshot_files = sorted([
            os.path.join(screenshots_dir, f) for f in os.listdir(screenshots_dir)
            if f.lower().endswith((".png", ".jpg", ".jpeg"))
        ])
        print(f"Found {len(screenshot_files)} screenshots ready for upload in {screenshots_dir}")

    for loc in target_locales:
        print(f"\n--- Processing Locale: [{loc}] ---")
        try:
            title, short_desc, full_desc = read_listing_files(project_root, loc)
            print(f"Title ({len(title)} chars): {title}")
            print(f"Short Description ({len(short_desc)} chars): {short_desc}")
            print(f"Full Description ({len(full_desc)} chars)")

            listing_body = {
                "title": title,
                "shortDescription": short_desc,
                "fullDescription": full_desc
            }
            service.edits().listings().update(
                packageName=package_name,
                editId=edit_id,
                language=loc,
                body=listing_body
            ).execute()
            print(f"[{loc}] Listing metadata successfully staged.")
        except Exception as e:
            print(f"[{loc}] Error updating metadata: {e}")

        if args.upload_screenshots and screenshot_files:
            print(f"[{loc}] Clearing old screenshots...")
            try:
                service.edits().images().deleteall(
                    packageName=package_name, editId=edit_id, language=loc, imageType="phoneScreenshots"
                ).execute()
            except Exception as e:
                print(f"[{loc}] Note on clearing screenshots: {e}")

            for sf in screenshot_files:
                fname = os.path.basename(sf)
                mimetype = "image/png" if sf.lower().endswith(".png") else "image/jpeg"
                media = MediaFileUpload(sf, mimetype=mimetype)
                service.edits().images().upload(
                    packageName=package_name,
                    editId=edit_id,
                    language=loc,
                    imageType="phoneScreenshots",
                    media_body=media
                ).execute()
                print(f"[{loc}] Uploaded: {fname}")

    print(f"\nCommitting edit ID {edit_id} to Google Play Store...")
    commit_response = service.edits().commit(packageName=package_name, editId=edit_id).execute()
    print("COMMIT SUCCESSFUL! All listing metadata and screenshots have been updated on Google Play Store.")

if __name__ == "__main__":
    main()
