# Learning Dashboard – Senior Mobile App Technical Assignment

A production-grade Android application developed with **Kotlin**, **Jetpack Compose**, **Room Database (Offline-First)**, **Navigation Compose**, and **Coroutines / Flow**, following modern **Clean Architecture + MVVM** principles.

---

## Demo Video & Walkthrough
The video walkthrough demonstrates all 5 assignment requirements:
- **Video File:** [Demo.mp4](./Demo.mp4) (4.5 MB, MP4)

| Flow Demonstrated | Details |
| :--- | :--- |
| **1. Login Flow** | Authentication with validation, loading indicator, and mock API token |
| **2. Course Dashboard** | List of courses showing instructors, dynamic progress percentages, and lesson counts |
| **3. Course Details** | Syllabus view with completed (`✓ Completed`) and pending (`○ Pending`) lesson badges |
| **4. Lesson Completion** | Interactive completion toggle triggering instantaneous progress recalculation across screens |
| **5. Offline Behavior** | Disconnecting connectivity demonstrates seamless cache retrieval from Room & offline banner |

---

## Technical Answers & Engineering Thinking

### 1. Architecture: Why did you choose your architecture?
We implemented **Clean Architecture with MVVM and Unidirectional Data Flow (UDF)**:
```
UI (Jetpack Compose) 
       ↓ Events
ViewModel (StateFlow & Immutable UiState)
       ↓ Invocations
Repository (Single Source of Truth)
       ↓ 
Room Local Database (SQLite) + Remote Mock API (Network)
```

**Key Rationale:**
- **Single Source of Truth (SSOT):** The UI observes reactive Room database `Flow`s rather than temporary network responses. This guarantees that local mutations (e.g., marking a lesson completed) immediately update the UI across all screens (Details and Dashboard) without requiring manual refresh or cache invalidation hacks.
- **Predictable State (UDF):** Screens expose strongly typed sealed UI states (`Loading`, `Success`, `Error`, `Empty`) that drive the UI deterministically and eliminate race conditions or inconsistent UI states.
- **Testability & Decoupling:** Business logic (e.g., `ProgressCalculator`, Repository offline decision branching, ViewModel validation) is completely decoupled from the Android framework, enabling fast, pure JVM JUnit tests without needing mocks or Robolectric.

---

### 2. Offline Support: How are you storing and loading offline data?
- **Local Persistence Engine:** Android Jetpack **Room** backed by SQLite.
- **Relational Schema:**
  - `CourseEntity`: Stores course metadata (`id`, `title`, `instructor`).
  - `LessonEntity`: Foreign-keyed to `CourseEntity` (`id`, `courseId`, `title`, `isCompleted`) with cascade deletion and indices on `courseId`.
  - `CourseWithLessons`: Uses Room `@Relation` to model the 1-to-many relationship.
- **Offline Workflow:**
  1. On first launch, if Room is empty, data is downloaded from the API and inserted in a single atomic database `@Transaction` (`replaceAll`).
  2. If the user loses network connectivity or is in Airplane mode, the `NetworkMonitor` detects the offline state and the repository directly serves the local Room cache.
  3. When the user marks a lesson completed offline, Room executes an immediate UPDATE query (`markLessonCompleted`). Room re-emits the updated `CourseWithLessons` snapshot through the active `Flow`, automatically recalculating the progress percentage and updating both the current screen and the dashboard.

---

### 3. Security: Where would you store authentication tokens in a production application?
In a production Android enterprise application:
1. **Hardware-Backed Storage:** Store tokens in **`EncryptedSharedPreferences`** from the `androidx.security.crypto` library. Under the hood, this uses the **Android Keystore system** to store a Master Key in a hardware security module (Secure Element / StrongBox Keymaster) with AES-256 GCM encryption.
2. **Biometric Protection:** For sensitive banking or learning compliance operations, keys are generated with `setUserAuthenticationRequired(true)`, requiring OS-level `BiometricPrompt` authentication before decryption.
3. **Transport Security:**
   - Strict HTTPS with **SSL/TLS Certificate Pinning** (via OkHttp `CertificatePinner` and Android `network_security_config.xml`).
   - OAuth 2.0 with **PKCE (Proof Key for Code Exchange)**.
   - Ephemeral short-lived access tokens (JWT) paired with rotatable refresh tokens; tokens are purged immediately from memory upon logout (`clear()`).
   - Prevent tokens from leaking into logcat or heap dumps by marking sensitive fields with `@Transient` and stripping debug logs in ProGuard/R8 release builds.

---

### 4. Scale: 1 Million Users + Hundreds of Courses (Improvements)
If scaling to 1M+ active users and hundreds of courses:
1. **Paging 3 & RemoteMediator:**
   - Replace bulk list loading with Android Jetpack `Paging 3`. Use `RemoteMediator` to coordinate chunked REST pagination (e.g., 20 courses per page) directly into Room with incremental cache eviction.
2. **WorkManager Offline Synchronization Queue:**
   - Implement an event outbox pattern. Offline lesson completions and user watch time are queued in a local `pending_sync` Room table. A constrained `CoroutineWorker` (`NetworkType.CONNECTED`) uploads events to the backend with exponential backoff and idempotency keys.
3. **CDN Media Asset Caching & Streaming:**
   - Offload course artwork, audio, and video content to an Edge CDN (Cloudflare/CloudFront). Use `Coil` for image memory/disk LRU caching and `ExoPlayer` with `CacheDataSource` for byte-range progressive video streaming.
4. **App Modularization:**
   - Split the codebase into feature modules (`:feature:auth`, `:feature:dashboard`, `:feature:course-details`) and core libraries (`:core:database`, `:core:network`, `:core:designsystem`, `:core:model`) to enable parallel Gradle builds, isolated test scopes, and Dynamic Delivery feature loading.
5. **Observability & Performance Monitoring:**
   - Integrate Datadog / OpenTelemetry / Firebase Crashlytics to monitor cold start times (optimized via Baseline Profiles), ANR rates, memory footprint, and network latency percentiles (p95/p99).

---

### 5. Second Platform: Implementing the same application on Apple iOS/macOS
If developing this architecture for **iOS/macOS**:
- **UI Framework:** **SwiftUI** with declarative state-driven views and native design tokens conforming to Apple Human Interface Guidelines.
- **Architecture:** **Clean Architecture + MVVM** using Swift 6's `@Observable` macro (or Combine's `ObservableObject` with `@Published` for earlier iOS targets) producing immutable view state structs.
- **Local Database & Offline Cache:** **SwiftData** (iOS 17+) or **Core Data** with SQLite backing. Define models with `@Model` (`CourseModel` and `@Relationship(deleteRule: .cascade) var lessons: [LessonModel]`). Queries use `@Query` macros for reactive UI updates upon database mutation.
- **Concurrency & Networking:** Native Swift Concurrency (`async`/`await`, `Task`, `AsyncStream` / Combine for reactive flows). Networking with `URLSession` and `Codable` models. Network reachability monitored with `NWPathMonitor` from Apple's `Network.framework`.
- **Navigation:** Type-safe `NavigationStack` with `navigationDestination(for:)` routing to `CourseDetailsView`.
- **Security:** Token persistence in the **Apple Keychain** (`SecItemAdd`, `SecItemCopyMatching`) using `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` or `kSecAccessControlBiometryAny` for Secure Enclave biometric validation.

---

## Project Structure & Deliverables

```
LearningDashboard/
├── Demo.mp4                                    <-- Recorded Demo Video (Login, Dashboard, Details, Offline)
├── app/
│   ├── build/outputs/apk/debug/app-debug.apk   <-- Generated Android APK
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/courses.json            <-- Mock course API payload
│   │   │   └── java/com/learning/dashboard/
│   │   │       ├── data/
│   │   │       │   ├── local/                 <-- Room Database, Entities, DAO, Mappers
│   │   │       │   ├── network/               <-- Network connectivity monitor
│   │   │       │   ├── remote/                <-- Mock Course & Auth APIs, DTOs
│   │   │       │   ├── repository/            <-- CourseRepository, AuthRepository
│   │   │       │   └── session/               <-- SessionStore (token storage)
│   │   │       ├── di/                        <-- AppContainer (Dependency Injection)
│   │   │       ├── domain/                    <-- Course & Lesson models, ProgressCalculator
│   │   │       └── ui/
│   │   │           ├── login/                 <-- Screen 1: LoginScreen, ViewModel, UiState
│   │   │           ├── dashboard/             <-- Screen 2: DashboardScreen, ViewModel, UiState
│   │   │           ├── details/               <-- Screen 3: CourseDetailsScreen, ViewModel, UiState
│   │   │           ├── components/            <-- Loading, Empty, Error state views
│   │   │           └── theme/                 <-- Material3 typography, color palette
│   │   └── test/java/com/learning/dashboard/  <-- Comprehensive Unit Test Suite
│   │       ├── domain/ProgressCalculatorTest.kt
│   │       ├── ui/login/LoginViewModelTest.kt
│   │       ├── ui/dashboard/DashboardViewModelTest.kt
│   │       ├── ui/details/CourseDetailsViewModelTest.kt
│   │       └── data/repository/CourseRepositoryTest.kt
```

---

## Build & Test Instructions

### 1. Run All Unit Tests
```bash
./gradlew testDebugUnitTest
```
*Output: All 18 unit tests across Domain, ViewModels, and Repositories pass successfully.*

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
*Generated binary:* `app/build/outputs/apk/debug/app-debug.apk`

### 3. Demo Credentials
- **Email:** `student@learn.com`
- **Password:** `password123`
