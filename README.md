# Employee Wellbeing Mobile

Native Android client for the Employee Wellbeing platform.

## Stack

- Kotlin with Java 21 bytecode.
- Android Gradle Plugin 9.3.1.
- Jetpack Compose and Material 3.
- Retrofit and OkHttp for the backend API.
- Android Keystore-backed encrypted preferences for the session.
- DDD-oriented feature boundaries.

## Local setup

1. Open this directory in Android Studio.
2. Confirm that Android SDK 36 and Java 21 are available.
3. Start the backend with the local Spring profile.
4. Use an Android emulator. The default debug API URL is `http://10.0.2.2:8080/`.
5. For a physical device, replace `API_BASE_URL` in `app/build.gradle.kts` with the host machine's LAN address.

Cleartext HTTP is enabled only for the debug build so local development can reach the backend. Release builds reject cleartext traffic and must use HTTPS.

## Architecture

The application is organized by business context. Authentication is the first connected context; the employee shell includes the four Stitch navigation destinations:

```text
Inicio | Encuestas | Chat IA | Perfil
```

The `SYSTEM_ADMIN` role is intentionally excluded from mobile navigation. System administration belongs to the web admin panel.

## Implemented scope

The mobile client currently includes:

- Registration with display name, username, email, password, and confirmation.
- Username/password login with encrypted session persistence.
- Employee password recovery with a generic request response, expiring single-use token, and new password confirmation.
- Automatic logout when a protected request returns HTTP 401.
- Role-based navigation for `EMPLOYEE` and `HR_MEMBER`; `SYSTEM_ADMIN` remains in the web admin panel.
- Employee tabs: Home, Surveys, AI Chat, and Profile.
- Daily mood submission and HR mood summary.
- Published surveys, answers, anonymous comments, replies, and likes.
- Weekly activities with vote changes and live percentages.
- Anonymous or identified reports; HR report status management.
- AI conversations for employees, including loading feedback and backend provider responses.
- Profile preferences for language and light/dark theme.
- Theme and language preferences are scoped by the immutable backend user ID and restored after each login.
- Release-safe Gson DTO handling and backup exclusion for encrypted session data.

Password recovery requests are delivered by the backend. In local development, the backend logs the temporary recovery link; copy its token into the recovery screen. Production must connect an email adapter and use HTTPS.

## Audit commands

Run the following commands before distributing a build:

```powershell
.\gradlew.bat clean app:build
.\gradlew.bat app:lintDebug app:testDebugUnitTest app:connectedAndroidTest
```

The current local audit expects the Spring Boot backend at `http://localhost:8080` and a running Android emulator for instrumented tests.
