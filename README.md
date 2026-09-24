# Krypta

**Krypta** is an offline-first Android password manager and encrypted document vault built with **Kotlin and Jetpack Compose**. It's designed around a simple principle: **your sensitive data should stay on your device and remain under your control.** Krypta provides separate vaults for passwords and documents, local encryption, secure authentication, password-generation tools, clipboard protection, and other security-focused features — without requiring a cloud account.

> **Project Status:** Active development. Krypta is currently being developed and security-audited. It should not yet be considered a production-grade password manager.

---

## Features
### Password Vault
* Can store website/app credentials locally
* Add, edit, and delete password entries
* Password strength analysis
* Secure password generator
* Duplicate-password detection
* Temporary password visibility
* Automatic clipboard clearing
* Search and manage saved credentials

### Encrypted Document Vault
* Import documents from Android storage
* Store imported files inside private app storage
* UUID-based internal file identifiers
* Separate document-vault authentication
* Room-backed document metadata
* Private file access through Android's `FileProvider`
* Atomic document-import handling

### Security
* Master-password authentication
* PBKDF2-HMAC-SHA256 key derivation
* 210,000 PBKDF2 iterations
* Per-installation cryptographic salt
* AES-GCM based encryption architecture
* Two-slot cryptographic key-bundle design
* Key-generation based recovery
* Failed-authentication cooldown
* Clipboard clearing
* Private application storage
* No cloud synchronization by default

### Additional Protection
* Authentication failure tracking
* Temporary session protection
* Security-focused lifecycle handling
* Defensive handling of malformed key-storage states
* Automated cryptographic migration tests
* Instrumentation testing on Android Emulator

---

## Tech Stack

| Technology                                   | Usage                         |
| -------------------------------------------- | ----------------------------- |
| **Kotlin**                                   | Primary programming language  |
| **Jetpack Compose**                          | UI                            |
| **Material 3**                               | Design system                 |
| **Room**                                     | Local database                |
| **Android Keystore / Android security APIs** | Cryptographic key handling    |
| **PBKDF2-HMAC-SHA256**                       | Password-based key derivation |
| **AES-GCM**                                  | Authenticated encryption      |
| **FileProvider**                             | Secure file sharing           |
| **Gradle**                                   | Build system                  |
| **Android Studio**                           | Development                   |

---

## Architecture
Krypta follows a layered Android architecture designed to keep UI, business logic, persistence, and security responsibilities separated.

```text
┌─────────────────────────────┐
│        Jetpack Compose      │
│             UI              │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│        ViewModels           │
│     State & UI Logic        │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│        Repositories         │
│     Application Logic       │
└──────────────┬──────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
┌─────────────┐  ┌──────────────┐
│    Room     │  │   Storage    │
│  Database   │  │  Private App │
└─────────────┘  │    Files     │
                 └──────────────┘

        ┌──────────────────┐
        │ Security / Crypto│
        │     Layer        │
        └──────────────────┘
```

---

## Project Structure

```text
Krypta/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/venkatbhoogarbh/krypta/
│       │   │       ├── data/
│       │   │       ├── security/
│       │   │       ├── storage/
│       │   │       ├── ui/
│       │   │       └── ...
│       │   └── res/
│       └── androidTest/
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Getting Started

### Requirements
* Android Studio
* JDK compatible with the project's Gradle/Android Gradle Plugin configuration
* Android SDK
* Android device or emulator running **Android 8.0 (API 26) or higher**

### Clone the repository
```bash
git clone https://github.com/venkatbhoogarbh/Krypta.git
cd Krypta
```

Open the project in **Android Studio**, allow Gradle to synchronize, and run the `app` configuration on an emulator or physical Android device.

---

## Testing

Krypta includes Android instrumentation tests for security-critical functionality.

Run the connected Android tests with:

```bash
./gradlew connectedDebugAndroidTest
```

On Windows PowerShell:

```powershell
.\gradlew connectedDebugAndroidTest
```

Security-related tests currently cover areas such as cryptographic key-bundle recovery and migration behavior.

---

## Security Model

Krypta is designed as an **offline-first** application. Sensitive data is intended to remain on the user's device rather than being automatically uploaded to a remote service. The authentication flow derives cryptographic material from the user's master password using **PBKDF2-HMAC-SHA256** with a per-installation salt. Krypta also uses a two-slot key-bundle design to improve resilience when cryptographic key state is being migrated or updated. Encryption operations use authenticated encryption through **AES-GCM**, helping provide both confidentiality and integrity.

### Important
Krypta is a personal/educational project undergoing active security review.

**Do not use Krypta as the sole storage location for critical production credentials or irreplaceable documents.**

Security-sensitive software benefits from independent review, extensive testing, threat modeling, and long-term maintenance.

---

## Design

Krypta uses a **dark-only Material 3 interface** designed around a secure command-center style. The application currently contains three primary areas:

```text
Passwords
    │
    ├── Saved credentials
    ├── Password generator
    └── Password security tools

Documents
    │
    ├── Imported documents
    └── Protected document storage

Settings
    │
    └── Application & security configuration
```

---

## Screenshots

> Screenshots will be added as the UI continues to be polished.


---

## Goals

Krypta is being developed with the following goals:
* **Offline-first** — no cloud dependency for core functionality
* **Privacy-focused** — sensitive information stays local
* **Security-conscious** — cryptographic operations are treated as first-class functionality
* **Simple UX** — security should not make everyday use unnecessarily complicated
* **Maintainable architecture** — security and application logic should remain testable and separated

---

## ⚠️ Disclaimer

Krypta is an independently developed project and is currently under active development. While security has been a major design consideration, no software can be guaranteed to be completely secure. The project has **not undergone a formal independent security audit**. Use it for experimentation, learning, and development purposes at your own risk.

---

## Author

**Venkat Bhoogarbh**

Computer Science student and Android developer.

GitHub: **[@venkatbhoogarbh](https://github.com/venkatbhoogarbh)**

---

## License

License information will be added to the repository as the project approaches its public-release stage.
