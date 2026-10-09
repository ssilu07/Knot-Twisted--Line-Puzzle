# 🚀 Google Play Store Automated Upload Guide (CI/CD Pipeline)

Is guide me step-by-step bataya gaya hai ki **Tangled Line** (`com.tangledline.game`) ko **Google Play Store** par automatically upload karne ke liye kya-kya setup karna hoga.

---

## 📌 Overall Process (Yeh Kaise Kaam Karta Hai)
1. **GitHub Actions Workflow** (`.github/workflows/deploy-playstore.yml`) ready hai.
2. Aap jab bhi GitHub par **"Run workflow"** button dabayenge ya version tag push karenge:
   - GitHub runner auto-setup hoga (JDK 17).
   - Keystore decode hoke signed `.aab` (Android App Bundle) build hoga.
   - Google Play Developer API ke through bundle direct Play Store ke **Internal Testing** (ya Closed Testing / Production) track par upload ho jayega.

---

## 🛠️ Step 1: Google Play Console & Google Cloud Setup (One-Time Setup)

> ⚠️ **Important Note:** Google Play Store ka rule hai ki app ka **sabse pehla build (.aab)** hamesha manually Play Console par upload karna padta hai. Uske baad se saare releases API ke through automated upload ho sakte hain.

### 1.1 Google Cloud me Service Account banana
1. [Google Play Console](https://play.google.com/console/) me login karein.
2. Left sidebar me **Setup** → **API access** par click karein.
3. Agar Cloud project linked nahi hai, toh **Link to a Google Cloud Project** ya **Create new project** par click karein.
4. **Service accounts** section me **Create service account** button par click karein.
5. Pop-up me link aayega jo aapko [Google Cloud Console](https://console.cloud.google.com/) par le jayega:
   - **Create Service Account** par click karein.
   - Name dalein: `play-store-uploader`
   - Role select karein: `Service Account User`
   - **Done** par click karein.
6. Usi Service Account par click karein → **Keys** tab me jayein:
   - **Add Key** → **Create new key** → **JSON** select karein → **Create**.
   - Ek `.json` file download hogi (e.g. `play-store-key.json`). **Is file ko sambhal kar rakhein.**

### 1.2 Google Play Android Developer API Enable karna
- Google Cloud Console ke search bar me search karein: **Google Play Android Developer API**
- Agar enable nahi hai toh **Enable** par click karein.

### 1.3 Play Console me Service Account ko Permissions dena
1. Wapas [Google Play Console](https://play.google.com/console/) me jayein.
2. **Users and permissions** (Left sidebar) par jayein.
3. **Invite new users** par click karein.
4. Email address me **Service Account ka email** dalein (ye email aapki `.json` file me `"client_email"` me likha hota hai, jaise: `play-store-uploader@project-id.iam.gserviceaccount.com`).
5. **App permissions** tab me:
   - **Add app** click karein aur `Tangled Line` select karein.
6. **Account permissions** tab me ye permissions check karein:
   - `View app information and download bulk reports`
   - `Create, edit, and roll out releases to testing tracks`
   - `Release apps to production, exclude devices, and use Play App Signing`
7. **Invite user** / **Send invitation** par click karein.

---

## 🔑 Step 2: Keystore & Signing Secrets Setup

### 2.1 Keystore ko Base64 me convert karna
GitHub Actions me direct `.keystore` / `.jks` binary file store nahi hoti, isliye use Base64 string me convert karte hain.

Aapne project me helper script create kiya hai:
```powershell
.\scripts\export-keystore-base64.ps1 -KeystorePath "D:\path\to\your-release.keystore"
```
Ye script automatically Base64 string copy kar degi aapke clipboard par.

---

## 🔐 Step 3: GitHub Repository Secrets Add karna

1. Apne GitHub Repository par jayein:
   `https://github.com/ssilu07/Knot-Twisted--Line-Puzzle`
2. **Settings** → **Secrets and variables** → **Actions** par click karein.
3. **New repository secret** par click karke ye 5 secrets add karein:

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | Keystore ka base64 encoded text jo script ne copy kiya tha |
| `KEYSTORE_PASSWORD` | Keystore file ka password |
| `KEY_ALIAS` | Key ka Alias name |
| `KEY_PASSWORD` | Key ka password |
| `PLAY_STORE_JSON_KEY` | Google Cloud se download ki gayi `.json` file ka pura text (notepad me open karke copy-paste karein) |

---

## 🚀 Step 4: Automated Upload Kaise Karein (Usage)

Jab upar ke saare secrets add ho jayein, tab aap 2 tarike se automate kar sakte hain:

### Tarika A: GitHub Website se 1-Click Run (Recommended)
1. GitHub repo me **Actions** tab par jayein.
2. Left side me **Build & Deploy to Google Play Store** workflow select karein.
3. **Run workflow** dropdown par click karein:
   - **Google Play Track**: `internal` (testing ke liye) ya `production` select karein.
   - **Version Code**: (Optional) e.g., `8` (Google Play hamesha pichle version code se bada number mangta hai).
   - **Version Name**: (Optional) e.g., `1.7`.
4. Green color ke **Run workflow** button par click karein.
5. GitHub runner build banayega aur direct Google Play par upload kar dega!

### Tarika B: Git Tag push karke (Release Automation)
Jab bhi aap terminal se tag push karenge:
```bash
git tag v1.7
git push origin v1.7
```
Workflow automatically trigger ho jayega aur deploy kar dega!

---

## 💡 Pro-Tips
1. **Pehle Internal Track use karein:** Hamesha pehle `internal` track par upload karein. Internal releases bina Google review ke 1 minute me testers ke phone par aa jaate hain. Wahan check karne ke baad Play Console se 1-click me production me promote kar sakte hain.
2. **Version Code:** Play Store har naye upload ke liye `versionCode` increment mangta hai. Isliye har naye release se pehle `app/build.gradle.kts` me `versionCode` ko +1 karein (e.g. 7 se 8).
