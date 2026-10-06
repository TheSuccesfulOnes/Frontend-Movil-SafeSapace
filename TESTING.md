# Mobile validation tests

Run from this repository in PowerShell:

```powershell
./tests/run-validation.ps1
```

The runner uses the Android Studio JDK at `C:\Program Files\Android\Android Studio\jbr` and creates/reuses `M:` for this repository. It refuses to overwrite another mapping or occupied drive. Use `-Drive N:` for an unused alternative. Mappings are retained for review, and no other mappings are removed. The short ASCII path avoids Kotlin/Gradle worker classpath failures caused by the original path's spaces/accent. `.kotlin/` is already ignored.

Equivalent build/test/check command after confirming the mapping:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
M:\gradlew.bat -p M:\ :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:validationDependencyInventory --console=plain
```

`tests/validation-owners.json` is the exhaustive source classification manifest. The runner parses JUnit XML from `app/build/test-results/testDebugUnitTest/`, writes `tests/validation-inventory.json` (every executed individual case, unit/integration type, outcome, source-to-suite mapping and actual counts), and refreshes the table below. It fails for a missing suite, failed/error/skipped case, fewer than 20 cases per owner, or a new/unclassified Kotlin source file. `tests/update-inventory.ps1` can regenerate documentation after an independent complete run; pass its command and exit code. Do not use it after a filtered suite run: the complete set is required, and Gradle results from previous runs are not evidence of newly executed filtered tests.

Tests live in the native `app/src/test/java` tree. Each parameterized row is one executed JUnit case. The existing password-policy suite contains three methods with multiple assertions/iterations: it counts as exactly three cases, supplemented by 17 separate boundary cases. The existing status-color test is retained, counted in total execution, and excluded from validation owner totals. No instrumentation/code coverage percentage is claimed.

Validation owners are discovered by inspecting required/blank/range checks, trimming, truncation, option membership, busy guards, role checks, stored claims and safe backend-error mappings. Simple color/icon/label/empty-list rendering and factory class casts are excluded. Some viewmodels delegate final input/status/permission validation to the server; their suites verify request contracts and local lifecycle without inventing additional frontend rejection rules. AuthScreens/LoginScreen delegates required-field checks to AuthViewModel; registration delegates password acceptance to RegistrationPasswordPolicy. It does not add an independent input-validation owner merely by displaying validation messages.

`unit` denotes an isolated policy/guard or error-mapping case. `integration` means local component plus in-memory repository/API/persistence contract, or real Retrofit/Gson/OkHttp against MockWebServer on localhost. Form integration cases call the same production functions used by the form, then invoke production viewmodels/APIs. They are not Compose interaction tests. Navigation integrations test the role policy with persisted claims; they do not mount AppNavHost or execute its LaunchedEffect.

Every case gets new preferences, APIs, dispatcher and state. Scripted services reject unprepared/exhausted calls, preserve the original exception through the suspend continuation, and assert that held operations were released. No application container or production URL is constructed. HTTP fixtures allow only their own loopback host and port, set a three-second request bound, and close responses, connections, executors and servers. Dispatcher work is drained and Main is reset after each case. Server dates are fixed; day rollover is simulated by a reload returning no mood. Optimistic AI IDs/timestamps are not compared to wall-clock values. No long sleeps or real service calls are used. Maven/Gradle downloads and the separate OSV metadata audit can require internet access; cached test execution does not contact application providers.

Android email-pattern cases use Robolectric SDK 35 with a plain Android Application (not WellbeingApplication). Session/appearance/photo stores use injected in-memory SharedPreferences. The fake typed getters throw on wrong stored types like Android; current corrupt-value cases concern missing/blank strings or unsupported preference values, not recovery from wrong stored types. Android Keystore/encryption, actual preference disk durability/concurrency, system photo picker/URI permissions/bitmap decoding, saved-instance-state handling, Compose rendering/taps, navigation effects, accessibility and emulator/device E2E are unverified. Backend enforcement, real authentication, Firebase/Gemini/Render and paid services are unverified. UI-only splash/feedback timers are excluded; there is no mobile 20-second provider polling timer in the inspected source. Release assembly is not part of this command.

Production seams preserve the original checks: small internal functions remain in their owning screen files and are called by production; constructors accept preferences internally while public Context constructors retain encrypted/session or plain non-sensitive storage; AccessTokenInterceptor visibility is internal for contract tests. No feature behavior or production dependency version was intentionally changed.

Dependency audit is separate and opt-in for reruns:

```powershell
./tests/run-validation.ps1 -RefreshAudit
# Or after resolving :app:validationDependencyInventory:
./tests/audit-dependencies.ps1
```

Only MockWebServer 5.1.0 and Robolectric 4.16.1 were added, both test dependencies. `tests/dependency-audit.json` records every resolved Maven coordinate queried against [OSV's version-specific batch API](https://google.github.io/osv.dev/post-v1-querybatch/), separating the production debug runtime from test-only artifacts. This is a known-advisory lookup, not a security guarantee. Build plugins/toolchain/Android SDK, dynamically downloaded Robolectric Android image and release-only dependencies are outside this audit. Lint version-update suggestions are not vulnerability findings. Dependency versions in production remain unchanged. See the audit result below for actual findings and incomplete-query errors, if any.

Follow-up remediation: Robolectric originally resolved `org.bouncycastle:bcprov-jdk18on:1.81`, with four OSV advisories. The `testImplementation` constraint now strictly selects **1.85.2**, an available maintenance release of the patched 1.85 line. It is a constraint on an existing transitive test dependency, not a production dependency. Authoritative [Maven Central metadata](https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk18on/maven-metadata.xml) showed latest/release **1.86** when investigated; the [1.85.2 POM](https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk18on/1.85.2/bcprov-jdk18on-1.85.2.pom) confirms the same Java 8+ artifact family, and the [publisher's 1.85.2 tag](https://github.com/bcgit/bc-java/releases/tag/r1rv85v2) identifies this maintenance release. Version 1.85.2 was chosen to retain the fixed 1.85 branch and verified through the full Robolectric/JVM suite. Production coordinates are compared against the pre-remediation graph.

| Original OSV advisory | Affected ranges / fixed releases for bcprov-jdk18on from the authoritative OSV JSON | Selected release |
| --- | --- | --- |
| [GHSA-574f-3g2m-x479](https://osv.dev/vulnerability/GHSA-574f-3g2m-x479), GOST CTR keystream reuse | 1.59..<1.80.2, 1.81.0..<1.81.1 and 1.82..<1.84; fixes 1.80.2 / 1.81.1 / 1.84 | 1.85.2 |
| [GHSA-9pwp-9qqc-pr26](https://osv.dev/vulnerability/GHSA-9pwp-9qqc-pr26), certificate name-constraints bypass | <1.85; fixed 1.85 | 1.85.2 |
| [GHSA-c3fc-8qff-9hwx](https://osv.dev/vulnerability/GHSA-c3fc-8qff-9hwx), LDAP injection | 1.74..<1.84; fixed 1.84 | 1.85.2 |
| [GHSA-qp49-qgx5-5m26](https://osv.dev/vulnerability/GHSA-qp49-qgx5-5m26), ASN.1 nesting-depth guard | <1.85; fixed 1.85 | 1.85.2 |

None of these four advisories is waived as inapplicable: the available patched release replaces the affected test artifact. The current audit below records any remaining version-specific advisories; a clean lookup is not proof of universal security. `MainDispatcherRule.finished` drains scheduled work in `try` and resets Main in `finally`, including when asynchronous cleanup throws.

Remediation verification: `./tests/run-validation.ps1 -RefreshAudit` passed the complete 508-case suite (507 validation cases across 25 owners), debug assembly and lint (0 errors, 35 warnings). An independent verification forced the test task to execute again with `:app:testDebugUnitTest --rerun`, retained debug assembly/lint checks and regenerated the inventory successfully. Gradle dependencyInsight confirms Robolectric's `bcprov-jdk18on:1.81` resolves to `1.85.2` by the strict test constraint. All **91 production coordinates were identical** before and after the change; only the test provider coordinate changed. The refreshed OSV lookup queried 129 coordinates (91 production, 38 test-only), with zero flagged coordinates and zero query errors. The runner also reads existing drive mappings through the Unicode Windows mapping API so the accented repository path is correctly recognized when launched from its original directory; no drive mapping was replaced or removed.

Execution history: the initial production compile caught a refactor reference to optionCount and was corrected. The first executable run had 282 cases and three failures: checked exceptions were wrapped by the test proxy (two cases) and the base test class was discovered as a suite (one case). The proxy now resumes suspend failures with their original cause; the base is abstract. A subsequent complete then-implemented run passed 401 cases and assembly/lint. These historical failures are not silently omitted; the generated inventory below describes the final full run.

<!-- dependency-audit:start -->
OSV lookup: 91 production coordinates and 38 test-only coordinates; queried 129. Flagged production coordinates: 0; flagged test-only coordinates: 0; query errors: 0. Exact coordinates and advisory IDs are in [dependency-audit.json](tests/dependency-audit.json).
<!-- dependency-audit:end -->

<!-- validation-results:start -->

Executed: 508 cases; validation cases: 507; owners: 25; failures: 0; errors: 0; skipped: 0. Command exit code: 0.

| Source owner | Executed cases | Unit | Local integration | Suite(s) |
| --- | ---: | ---: | ---: | --- |
| authentication/presentation/AuthViewModel.kt | 20 | 12 | 8 | AuthViewModelValidationTest |
| authentication/presentation/PasswordRecoveryViewModel.kt | 20 | 8 | 12 | RecoveryValidationTest |
| authentication/domain/RegistrationPasswordPolicy.kt | 20 | 20 | 0 | PasswordBoundaryTest, RegistrationPasswordPolicyTest |
| authentication/domain/AuthRepository.kt | 20 | 0 | 20 | AuthRepositoryContractTest |
| authentication/presentation/AuthErrorMapper.kt | 20 | 19 | 1 | AuthErrorValidationTest |
| profile/presentation/ProfileSettingsScreen.kt | 20 | 16 | 4 | ProfileFormValidationTest |
| profile/presentation/ProfileViewModel.kt | 20 | 0 | 20 | ProfileViewModelValidationTest |
| profile/data/ProfilePhotoStore.kt | 20 | 0 | 20 | PhotoStoreValidationTest |
| humanresources/presentation/HrScreens.kt | 27 | 23 | 4 | HrFormValidationTest |
| humanresources/presentation/HrViewModels.kt | 20 | 4 | 16 | HrViewModelsValidationTest |
| survey/presentation/SurveyViewModel.kt | 20 | 4 | 16 | SurveyViewModelValidationTest |
| survey/presentation/SurveyScreen.kt | 20 | 15 | 5 | SurveyFormValidationTest |
| activity/presentation/ActivityViewModel.kt | 20 | 7 | 13 | ActivityViewModelValidationTest |
| activity/presentation/ActivityScreen.kt | 20 | 8 | 12 | ActivityFormValidationTest |
| report/presentation/ReportScreen.kt | 20 | 17 | 3 | ReportFormValidationTest |
| report/presentation/ReportViewModel.kt | 20 | 1 | 19 | ReportViewModelValidationTest |
| ai/presentation/AiViewModel.kt | 20 | 3 | 17 | AiViewModelValidationTest |
| ai/presentation/AiScreen.kt | 20 | 16 | 4 | AiFormValidationTest |
| mood/presentation/HomeViewModel.kt | 20 | 0 | 20 | HomeViewModelValidationTest |
| mood/presentation/HomeScreen.kt | 20 | 7 | 13 | MoodFormValidationTest |
| shared/data/SessionStore.kt | 20 | 11 | 9 | SessionStoreValidationTest |
| shared/data/AppearanceStore.kt | 20 | 12 | 8 | AppearanceStoreValidationTest |
| shared/navigation/AppNavHost.kt | 20 | 16 | 4 | MobileRoleValidationTest |
| shared/data/ApiClient.kt | 20 | 0 | 20 | AccessTokenValidationTest |
| shared/presentation/UiComponents.kt | 20 | 19 | 1 | UserErrorValidationTest |

All individual executed case names and their outcomes/types are saved in [validation-inventory.json](tests/validation-inventory.json). Counts are JUnit testcase nodes, never assertions or iterations within a test method.

Excluded scan candidates:

| Candidate | Reason |
| --- | --- |
| authentication/presentation/AuthScreens.kt | LoginScreen delegates to AuthViewModel; RegisterScreen delegates to the same viewmodel/password policy. Checklist, mismatch text, visibility, loading and registration navigation are presentation/delegation, not independent input validators. Password saved-state behavior and Compose callbacks need device tests. |
| activity/data/ActivityApi.kt | DTOs, enum-free API contracts; service annotations own no validation. Exercised by activity HTTP contracts. |
| ai/data/AiApi.kt | DTOs and Retrofit contracts, no validation. Exercised by AI HTTP contract. |
| authentication/data/AuthApi.kt | DTOs and Retrofit contracts, no validation. Exercised by repository HTTP suite. |
| comment/data/CommentApi.kt | DTOs and Retrofit annotations, no independent input validation; answer/comment/reply service flows exercised in memory. |
| mood/data/MoodApi.kt | Typed enum/DTOs and HTTP annotations, no runtime validator; all moods exercised. |
| profile/data/ProfileApi.kt | DTOs/Retrofit declarations, no independent validation; HTTP and in-memory account contracts exercised. |
| report/data/ReportApi.kt | DTOs/Retrofit declarations, no independent validation; report serialization exercised. |
| survey/data/SurveyApi.kt | Typed survey enum and Retrofit declarations, no independent runtime validator. |
| profile/presentation/ProfileScreen.kt | Delegates account validation to settings/viewmodel, storage to PhotoStore. Display fallbacks, feedback and platform photo URI/permission/bitmap plumbing are not independent user validators; picker/device lifecycle unverified. |
| shared/data/LegalLinks.kt | Static privacy-policy URL, no validation. |
| shared/presentation/AppLocalization.kt | Translation/display labels/fallbacks; no input, authorization or business validation. |
| shared/presentation/SafeSpaceTopBar.kt | Visual composition and callback plumbing. |
| shared/presentation/DesignComponents.kt | Visual components and name/status formatting; no independent validation. |
| shared/designsystem/WellbeingTheme.kt | Theme rendering selection and color definitions, no accepted-input policy; normalization belongs to AppearanceStore. |
| shared/designsystem/StatusColors.kt | Visual status-color mapping. Existing one test retained outside validation counts. |
| MainActivity.kt | Application composition, system-bar and splash animation delays; no independent input validation. Actual splash timers/device lifecycle unverified. |
| WellbeingApplication.kt | Application container construction, no input validation. |
| activity/presentation/ActivityViewModelFactory.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |
| ai/presentation/AiViewModelFactory.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |
| authentication/presentation/AuthViewModelFactory.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |
| humanresources/presentation/HrViewModelFactories.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |
| mood/presentation/HomeViewModelFactory.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |
| profile/presentation/ProfileViewModelFactory.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |
| report/presentation/ReportViewModelFactory.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |
| survey/presentation/SurveyViewModelFactory.kt | ViewModelProvider model-class require/type casts are boilerplate factories, not user input/security/business validation. |

Unclassified source files: 0. Orphaned manifest paths: 0.

<!-- validation-results:end -->
