# Krypta

**Krypta** is an offline-first Android password manager and encrypted document vault built with **Kotlin and Jetpack Compose**. The idea behind Krypta is pretty simple: **sensitive data should stay on your device and remain under your control.** Krypta lets you store passwords and documents locally, protect them with a master password, generate strong passwords, and reduce common risks such as accidentally leaving sensitive information in the clipboard. There is no cloud account or server required for its core functionality.

> **Project Status:** Active development. Krypta is currently being developed and security-audited. It is **not yet intended to be used as a production-grade password manager.**

---

## Features

### 🔐 Password Vault

The Password Vault is the main part of Krypta and lets you manage your saved credentials locally.

* Store website and application credentials
* Add, edit, and delete password entries
* Search saved credentials
* Check password strength
* Generate secure passwords
* Detect duplicate passwords
* Temporarily reveal hidden passwords
* Automatically clear sensitive clipboard contents

### 📁 Encrypted Document Vault

Krypta also includes a separate vault for protecting personal documents.

* Import documents using Android's storage picker
* Store imported files inside the app's private storage
* Use UUID-based internal file identifiers instead of relying on original file paths
* Require separate authentication for the Document Vault
* Store document metadata using Room
* Share files securely through Android's `FileProvider`
* Handle document imports atomically to avoid leaving incomplete files behind

### 🛡️ Security

Security is treated as a core part of Krypta rather than something added on later.

* Master-password authentication
* PBKDF2-HMAC-SHA256 password-based key derivation
* 210,000 PBKDF2 iterations
* Per-installation cryptographic salt
* AES-GCM authenticated encryption
* Two-slot cryptographic key-bundle design
* Generation-based key recovery
* Authentication failure cooldown
* Automatic clipboard clearing
* Private application storage
* No cloud synchronization by default

### 🔎 Additional Protection

Krypta also includes several defensive measures around authentication and cryptographic state:

* Failed-authentication tracking
* Temporary session protection
* Lifecycle-aware security handling
* Defensive handling of malformed key-storage states
* Automated cryptographic migration tests
* Android instrumentation testing

---

## Tech Stack

| Technology                           | Purpose                       |
| ------------------------------------ | ----------------------------- |
| **Kotlin**                           | Primary programming language  |
| **Jetpack Compose**                  | UI                            |
| **Material 3**                       | Design system                 |
| **Room**                             | Local database                |
| **Android Keystore / Security APIs** | Cryptographic key handling    |
| **PBKDF2-HMAC-SHA256**               | Password-based key derivation |
| **AES-GCM**                          | Authenticated encryption      |
| **FileProvider**                     | Secure file sharing           |
| **Gradle**                           | Build system                  |
| **Android Studio**                   | Development                   |

---

## Architecture

Krypta uses a layered architecture so that the UI, application logic, data storage, and security-related code can remain reasonably separated.

```text
┌─────────────────────────────┐
│        Jetpack Compose      │
│             UI              │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│          ViewModels         │
│       State & UI Logic      │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│        Repositories         │
│      Application Logic      │
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
        │      Layer       │
        └──────────────────┘
```

The security layer is kept separate from the normal application flow where possible, making security-sensitive code easier to test and review.

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

## Security Model

Krypta is designed around an **offline-first** approach. The core idea is that sensitive information should remain on the user's device instead of being automatically uploaded to a remote service.

The authentication system derives cryptographic material from the user's master password using **PBKDF2-HMAC-SHA256** with a per-installation salt.

Krypta also uses a **two-slot key-bundle design**. The goal is to make cryptographic key state more resilient during migrations or updates by keeping recoverable key generations rather than relying on a single piece of stored state.

For encryption, Krypta uses **AES-GCM**, which provides authenticated encryption so that encrypted data can be protected against both unauthorized reading and undetected modification.

### Important

Krypta is currently a **personal/educational project undergoing active development and security review**.

**Do not use Krypta as the sole storage location for critical production credentials or irreplaceable documents. Security-sensitive software needs extensive testing, threat modeling, independent review, and long-term maintenance. Krypta is still working toward that level of maturity.**

---

## ⚠️ Disclaimer

Krypta is an independently developed project and is still under active development.

Security has been a major consideration throughout the project, but **no software can be guaranteed to be completely secure**. Krypta has **not undergone a formal independent security audit**.

For now, Krypta is best treated as a learning, experimentation, and development project rather than a replacement for an established production password manager.

---

## Author

**Venkat Bhoogarbh**

Computer Science student and Android developer.

GitHub: **[@venkatbhoogarbh](https://github.com/venkatbhoogarbh)**

---

## License

License information will be added to the repository as Krypta approaches its public-release stage.
