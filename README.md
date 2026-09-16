# Bookisham for Android

The native Android app for [Bookisham](https://github.com/Arkhins-0/bookisham),
the private reading room. Kotlin, Jetpack Compose, Material 3. Minimum
Android 8.0 (API 26), targets API 35. No third-party UI libraries: OkHttp for
HTTP, kotlinx.serialization for JSON, Navigation Compose for the screens.

It talks to the same Next.js server the website runs on, at
`https://bookisham.arkhins.com`. Nothing about the books lives on the phone:
pages are fetched one at a time through the session, unwrapped in memory,
drawn, and dropped.

## What the app does

Everything a reader can do on the website, and everything an admin can:

- **Landing** with the banner, the three steps, Contact (WhatsApp or email) and Login.
- **Sign in** with the email or username and password from the admin. One device at a time, as on the web.
- **Library**: the books this reader has been given, with progress bars, pull to refresh.
- **Browse**: every book, locked covers for the ones the reader has not been given.
- **Reader**: one scrolled column of pages, pinch or buttons to zoom, page jump, the current page saved locally at once and on the server a moment later.
- **Account**: change name and password; sign out.
- **Admin** (admin accounts only): the reader list; create a reader and see the generated password once; per reader, reset the password, sign out their device, disable, delete, and tick which books they may open; the book list; upload a book (PDF, EPUB or Word) with an optional cover, edit or replace one, delete one.

### Keeping the text off the clipboard

While a book is open the window carries `FLAG_SECURE`: the OS refuses
screenshots and screen recording and blanks the app in the recents switcher.
The pages are covered whenever the app is not in front, the reader's email is
tiled faintly across every page, and nothing is ever written to disk.

## Build

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).
Android Studio installs both; otherwise point `local.properties` at the SDK:

```
sdk.dir=C:/Users/you/AppData/Local/Android/Sdk
```

Then:

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # onto a connected device or emulator
./gradlew assembleRelease        # signed if a keystore is configured, see below
```

Or open this folder in Android Studio and press Run.

## Configuration

`gradle.properties`:

```
bookisham.baseUrl=https://bookisham.arkhins.com    # no trailing slash
bookisham.whatsapp=917708204956                    # digits only, or blank
bookisham.contactEmail=krishnavijay.gkv@gmail.com  # or blank
```

Any of these can be overridden per machine in `local.properties` (not
checked in) or on the command line with `-Pbookisham.baseUrl=...`. They end
up in `BuildConfig` and are read through `Config.kt`.

Plain HTTP is refused except to `10.0.2.2`, `localhost` and `127.0.0.1`
(`res/xml/network_security_config.xml`), so a dev server on the host machine
works from the emulator with `bookisham.baseUrl=http://10.0.2.2:3000`.

## Signing a release build

An unsigned APK will not install ("package appears to be invalid"), so
`assembleRelease` looks for a keystore. Generate one once and keep it
outside git (it is already gitignored):

```bash
keytool -genkeypair -v -keystore release.keystore.jks -storetype JKS \
  -alias bookisham -keyalg RSA -keysize 2048 -validity 10000
```

Drop it at `release.keystore.jks` in this folder with alias `bookisham` (the
defaults `app/build.gradle.kts` looks for), or point at it via
`local.properties` / `-P` flags: `bookisham.keystore.path`,
`bookisham.keystore.password`, `bookisham.key.alias`,
`bookisham.key.password`. The same four values, as environment variables
(`BOOKISHAM_KEYSTORE_PATH`, `BOOKISHAM_KEYSTORE_PASSWORD`,
`BOOKISHAM_KEY_ALIAS`, `BOOKISHAM_KEY_PASSWORD`), are what the CI workflow
reads from repository secrets.

Every release must be signed with the **same** key, or Android refuses to
install it over the previous version. Keep the keystore backed up.

## Releasing to GitHub

Push a tag and [.github/workflows/release.yml](.github/workflows/release.yml)
builds the app, checks the release APK really is signed, and attaches both
APKs and a `SHA256SUMS.txt` to a GitHub Release:

```bash
git tag v1.0.1
git push origin v1.0.1
```

The workflow signs with these repository secrets (Settings → Secrets and
variables → Actions):

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 release.keystore.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
| `ANDROID_KEY_ALIAS` | the key alias |
| `ANDROID_KEY_PASSWORD` | the key password |

Repository variables `BOOKISHAM_BASE_URL`, `BOOKISHAM_WHATSAPP` and
`BOOKISHAM_CONTACT_EMAIL` override what the built app points at; without
them the workflow falls back to the live server.

A manual run from the Actions tab (workflow_dispatch) builds and verifies
without publishing anything.

## The server side

The app uses these routes on the Bookisham server. The website renders its
pages on the server, so the ones marked *added* did not exist before the app:

| Route | Returns |
|---|---|
| `POST /api/auth/login`, `POST /api/auth/logout` | the session cookie |
| `GET /api/me` *(added)* | the signed-in user, session expiry, contact details |
| `GET /api/library` *(added)* | the reader's shelf |
| `GET /api/browse` *(added)* | every book with an `unlocked` flag |
| `GET /api/books/:id` *(added)* | a book's shape, start page and page key |
| `GET /api/books/:id/cover`, `GET /api/read/:bookId/:page`, `POST /api/progress` | covers, pages, progress |
| `PATCH /api/account` | name and password changes |
| `GET /api/admin/users` *(added)*, `POST`, `GET /:id` *(added)*, `PATCH /:id`, `DELETE /:id`, `PUT /:id/books` | readers |
| `GET /api/admin/books` *(added)*, `POST`, `PATCH /:id`, `DELETE /:id` | books |

## Layout

```
app/src/main/java/com/bookisham/app/
  Config.kt                 server address, contact details, support links
  BookishamApplication.kt   process-wide objects: session, API client, cover cache
  MainActivity.kt           splash, edge-to-edge, the Compose root
  data/
    Models.kt, AdminModels.kt   the JSON shapes the server sends
    SessionStore.kt         the session token and last page per book (SharedPreferences)
    ApiClient.kt            every server call, with the session cookie sent by hand
    PageRepository.kt       pages of one book: fetch, unwrap (XOR with the session key), decode, LRU cache
    CoverStore.kt           covers, decoded once
    ContentUriRequestBody.kt a picked file streamed into a multipart upload
  ui/
    App.kt                  signed-out and signed-in navigation graphs, bottom bar
    ViewModels.kt           AppViewModel (who is signed in), ShelfViewModel
    AdminViewModels.kt      the reader list, one reader, the book list
    theme/Theme.kt          the paper / ink / ember palette and type scale
    components/             pills, fields, cards, the contact menu, BookCard, file pickers
    screens/                Landing, Login, Library, Browse, Account, Admin*
    reader/                 ReaderScreen and ReaderViewModel
```

## The reader

`ReaderScreen` is a `LazyColumn` of page slots, each sized from the page
ratio the first loaded page reported (A4 until then). A slot fetches its
page when it comes near the viewport; the repository keeps decoded bitmaps
in an `LruCache` bounded to a third of the heap. The current page is the
slot crossing the middle of the viewport; it is written to SharedPreferences
at once and posted to `/api/progress` after 600 ms of quiet.

Render scale: phones ask the server for `s=2`, screens 1600 px and wider for
`s=3` (the sharpest the server renders). Any 401 from the server signs the
app out and returns to the front door.
