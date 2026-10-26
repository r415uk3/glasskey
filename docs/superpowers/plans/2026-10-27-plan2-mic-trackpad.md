# Plan 2 — space trackpad and microphone

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (chosen by the user for plan 1, kept). Steps use checkbox (`- [ ]`) syntax.

**Goal:** the two iOS behaviours left from the spec: long-press on space turns the keyboard into a cursor trackpad, and a mic button under the keys dictates text with Google's recognizer.

**Architecture:** reuse HeliBoard's `TouchpadHandler` (2D cursor movement, already reachable via a space swipe) and trigger it from a space long-press in the iOS style; the iOS look is "keys without labels" instead of HeliBoard's half-transparent keyboard. Dictation is a new `IosDictation` controller around `SpeechRecognizer`, explicitly bound to the Google app's `GoogleRecognitionService` (the phone's default recognizer is vivo Copilot), shown through the reserved `ios_mic` button in `IosBottomBar`.

**Spec:** `docs/superpowers/specs/2026-10-26-ios-keyboard-design.md` (goals 3 and 6).

## Global Constraints

- iOS style only; stock styles keep HeliBoard behaviour.
- Recognizer component: `com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService`; mic hidden when it is not installed or not resolvable.
- Language of dictation = current keyboard language (`ru`, `en-US`, `kk`).
- `RECORD_AUDIO` is requested through a small transparent activity (an IME service cannot ask for runtime permissions itself).
- Partial results are shown as composing text; the final result is committed; tapping the mic again stops.

## Review Focus

1. Dictation while the field already has text / a selection — text must be inserted at the cursor, not replace the field.
2. Permission denied, or the Google app missing — no crash, mic hidden or a one-time explanation.
3. Switching language or closing the keyboard during dictation — recognizer released, no leaked listener.
4. Trackpad started on the space bar of the symbols page or in a numeric field.
5. Trackpad release — labels come back even if the finger leaves the keyboard.

---

### Task 1: Space long-press trackpad (iOS)

**Files:** `keyboard/PointerTracker.java` (`onLongPressed`), `keyboard/KeyboardActionListenerImpl.kt` (TOUCHPAD_ON/OFF visuals), `keyboard/KeyboardView.java` (skip key labels/icons while in trackpad mode), test `IosTrackpadTest.kt`.

- [ ] Failing test: a pure `IosTrackpad.shouldStartOnLongPress(isIos, code, popupKeys)` → true only for iOS + space without popups; and `KeyboardView` hides labels when `IosTrackpad.active` (pure flag object).
- [ ] Implement: in `onLongPressed`, iOS + space → `TouchpadHandler.setTouchpadModeActive(true)`, start the touchpad at the current pointer, haptic; TOUCHPAD_ON/OFF set `IosTrackpad.active` and invalidate instead of alpha 0.5 in the iOS style.
- [ ] Device check: long-press space in a text with several lines, move in 2D, release → labels back.
- [ ] Commit `feat: iOS space long-press trackpad`.

### Task 2: Dictation controller and permission

**Files:** create `latin/ios/IosDictation.kt`, `latin/ios/IosPermissionActivity.kt` (+ manifest entry, `<queries>` for `android.speech.RecognitionService`, `RECORD_AUDIO` permission); test `IosDictationTest.kt` (pure state machine: idle → listening → (partial)* → final → idle; error → idle; stop).

- [ ] Failing tests for the state machine with a fake recognizer: partial text becomes composing text, final text is committed with a leading space when needed, stop ends listening, errors end listening without committing.
- [ ] Implement with `SpeechRecognizer.createSpeechRecognizer(context, googleComponent)`, `RecognizerIntent.EXTRA_LANGUAGE` = subtype locale tag, `EXTRA_PARTIAL_RESULTS`.
- [ ] Commit `feat: iOS dictation controller`.

### Task 3: Mic button

**Files:** `latin/ios/IosBottomBar.kt`, `keyboard/KeyboardSwitcher.java`, `latin/LatinIME.java` (release on hide / input finish), drawables for the active mic (blue, pulsing).

- [ ] Show `ios_mic` when the Google recognizer is available; tap toggles dictation; while listening the icon is blue and pulses; permission missing → open `IosPermissionActivity`.
- [ ] Release the recognizer on `onFinishInputView` and on language switch (Review Focus 3).
- [ ] Device check: dictate ru / en / kk into a notes-like field with existing text (Review Focus 1); deny permission once (Review Focus 2).
- [ ] Commit `feat: iOS mic button with Google dictation`.
