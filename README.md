# Keeb ⌨️💸

**An Android keyboard that lets you send USDC on Base directly from any chat app.**

Keeb is a custom Input Method Editor (IME) for Android that detects Ethereum wallet addresses from your clipboard and presents a one-tap payment chip to send USDC on Base Sepolia — all without leaving the conversation.

## Features

- 🔤 **Full QWERTY keyboard** — Material3 dark theme with haptic feedback
- 📋 **Clipboard monitoring** — Automatically detects `0x...` Ethereum addresses
- 💳 **Payment chip** — Slide-up chip to set amount and send USDC
- ⛓️ **Base Sepolia** — ERC-20 transfers via web3j on L2
- ✅ **Chat confirmation** — Inserts `✅ Sent X USDC on Base` into the active chat

## Architecture

```
ClipboardMonitor → KeebInputMethodService → PaymentChip UI → TransactionService
       ↑                                                            ↓
  System Clipboard                                          Base Sepolia RPC
```

| Module | Purpose |
|--------|---------|
| `keyboard/` | IME service, keyboard layout, clipboard monitoring, address detection |
| `wallet/` | Web3j wallet management, ERC-20 transfer, Base Sepolia config |
| `utils/` | App-wide constants |

## Tech Stack

- **Language:** Kotlin
- **UI:** Material3 + Custom IME View
- **Web3:** web3j 4.10.3
- **Network:** Base Sepolia (Chain ID 84532)
- **Contract:** USDC (`0x036CbD53842c5426634e7929541eC2318f3dCF7e`)

## Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- A funded Base Sepolia wallet (for demo)

### Build

```bash
# Clone
git clone https://github.com/elfrtz/keeb.git
cd keeb

# Open in Android Studio and sync Gradle
# OR build from CLI:
./gradlew assembleDebug
```

### Configure Demo Wallet

Edit `app/src/main/java/com/elfrtz/keeb/wallet/BaseConfig.kt`:

```kotlin
const val DEMO_PRIVATE_KEY = "your_base_sepolia_private_key_here"
```

> ⚠️ **DEMO ONLY** — Never use a real wallet private key. Fund a test wallet with Base Sepolia ETH and USDC from the [Base Sepolia Faucet](https://www.alchemy.com/faucets/base-sepolia).

### Install & Enable

1. Build and install the APK on your device/emulator
2. Go to **Settings → System → Languages & Input → On-screen keyboard**
3. Enable **Keeb**
4. Open any chat app and switch to Keeb keyboard
5. Copy an Ethereum address to clipboard — the payment chip appears!

## Demo Flow

1. Copy a wallet address: `0x742d35Cc6634C0532925a3b844Bc9e7595f2bD18`
2. The payment chip slides up above the keyboard
3. Set the USDC amount (default: 5)
4. Tap **Send**
5. Transaction is signed and broadcast to Base Sepolia
6. Confirmation message is typed into the chat: `✅ Sent 5 USDC on Base`

## Project Structure

```
keeb/
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle config
├── app/
│   ├── build.gradle.kts          # App dependencies (web3j, Material3)
│   └── src/main/
│       ├── AndroidManifest.xml   # IME service declaration
│       ├── java/com/elfrtz/keeb/
│       │   ├── keyboard/
│       │   │   ├── KeebInputMethodService.kt  # Core IME service
│       │   │   ├── KeyboardView.kt            # QWERTY key generation
│       │   │   ├── ClipboardMonitor.kt        # Clipboard polling
│       │   │   └── AddressDetector.kt         # Ethereum address regex
│       │   ├── wallet/
│       │   │   ├── WalletManager.kt           # web3j wallet init
│       │   │   ├── TransactionService.kt      # ERC-20 transfer
│       │   │   └── BaseConfig.kt              # Network constants
│       │   └── utils/
│       │       └── Constants.kt               # App constants
│       └── res/
│           ├── layout/
│           │   ├── view_keyboard.xml          # Main keyboard layout
│           │   └── view_payment_chip.xml      # Payment chip UI
│           ├── drawable/
│           │   └── chip_background.xml        # Chip rounded bg
│           ├── anim/
│           │   ├── slide_up.xml               # Chip entrance
│           │   └── fade_out.xml               # Chip dismissal
│           ├── values/
│           │   ├── colors.xml                 # Dark theme palette
│           │   ├── strings.xml                # App strings
│           │   └── themes.xml                 # Material3 theme
│           └── xml/
│               └── method.xml                 # IME subtype config
```

## License

MIT — Built for hackathon demo purposes.
