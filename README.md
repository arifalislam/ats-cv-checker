# CV Check — ATS score for Android

Upload a CV as **PDF, DOCX or an image (JPG/PNG)** and get:

- an **ATS score out of 100** with a seven-part breakdown
- a prioritised list of **suggestions** (High / Medium / Low)
- optional **job-description matching**: paste a job ad to see which keywords you cover and which you miss
- a shareable plain-text report

Everything runs on the device. The app declares no internet permission, and the OCR model ships inside the APK.

## Build

**Android Studio (easiest):** File → Open → select this folder → let Gradle sync → Run ▶.
Requires Android Studio Ladybug or newer (JDK 17). If it offers to upgrade AGP/Kotlin versions, accept.

**No Android Studio:** push this folder to a GitHub repo. The included workflow
(`.github/workflows/build-apk.yml`) runs the unit tests and builds `app-debug.apk`; download it from the
run's *Artifacts*, then sideload it on your phone.

## How it works

| Input | How text is read |
|---|---|
| PDF | PDFBox-Android text extraction. If almost no text is found (scanned PDF), the first 3 pages are rendered and OCR'd |
| DOCX | Reads `word/document.xml` directly; also detects tables, text boxes and images, which hurt ATS parsing |
| Image | EXIF-corrected, downscaled, then ML Kit on-device OCR |

Scoring (`ats/AtsAnalyzer.kt`, plain Kotlin, unit-tested):

| Category | Points |
|---|---|
| Contact details | 10 |
| Section structure | 20 |
| Keywords (or job-description match) | 20 |
| Impact & achievements (numbers, action verbs) | 20 |
| Formatting & length | 15 |
| Content quality (vague phrases, pronouns, dates) | 10 |
| Machine readability | 5 |

This is a rules-based estimate of common ATS behaviour, not a replica of any vendor's parser.

## Tuning

- Skills, action verbs and weak phrases live in `ats/SkillBank.kt`.
- Weights and thresholds are in `ats/AtsAnalyzer.kt`.
- Colours and theme: `ui/Theme.kt` (follows system light/dark).

## Known limits

- Old `.doc` files are not supported (save as .docx or PDF).
- Password-protected PDFs are rejected with a clear message.
- OCR reads the first 3 pages of a scanned PDF.
- Keyword vocabulary is general-purpose; add terms for your field in `SkillBank.skills`.
