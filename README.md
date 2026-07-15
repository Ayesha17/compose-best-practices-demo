# Compose Best Practices Demo

One-screen Android app that demonstrates **Jetpack Compose recomposition control**, **immutable UI state**, and **ViewModel coroutine / dispatcher** best practices.

Open in Android Studio → Sync → Run on an emulator or device.

## What this project teaches

### 1. Break the screen into functions (avoid unnecessary recomposition)

`DemoScreen` composes three siblings:

| Composable | Owns / receives |
|---|---|
| `DemoHeader(state)` | `HeaderUiState` only |
| `MiddleView(...)` | form + table + list slices |
| `DemoFooter(state, …)` | `FooterUiState` + actions |

When the user types in a `TextField`, only form-related nodes should recompose. `Header` skips if `HeaderUiState` equals the previous value (immutable `data class` + stable inputs).

### 2. What params should each function take?

Prefer **the smallest slice** + **grouped actions**:

```kotlin
MiddleView(state: MiddleUiState, actions: MiddleUiActions, modifier)
```

Not 9–20 loose params (`form`, `items`, `onUsernameChange`, …).

- Specific values: `title: String` when that is all you need
- Section state: `MiddleUiState` / `HeaderUiState` for related fields
- Section actions: `MiddleUiActions` / `FormUiActions` for callbacks
- **Never** pass the entire `DemoUiState` + whole `ViewModel` into every child

If fields exceed ~10–20, group by domain (`FormUiState`, `AddressUiState`, …), not one mega object.

### 3. Group views by state / update frequency

| Group | Why together |
|---|---|
| Header title / subtitle / refresh label | Changes on load |
| Form fields | Change on every keystroke |
| Table + list | Change on refresh |
| Footer status / loading / canSubmit | Changes on load + validation |

### 4. `remember` vs `rememberSaveable`

| Use | When |
|---|---|
| `remember` | Ephemeral UI that can reset on rotation (local hint text) |
| `rememberSaveable` | UI-only state that should survive rotation (expand/collapse tip) |
| ViewModel `StateFlow` | Business / form data (username, list, loading) |

**Do not use `rememberSaveable` all the time.** Business state belongs in the ViewModel (optionally `SavedStateHandle`). Duplicating SSOT in `rememberSaveable` causes desync.

### 5. TextField and similar controls

- Hoist `value` + `onValueChange` to the ViewModel
- Isolate each field in its own `@Composable` (`FormTextField`)
- Same idea for: `Slider`, `Switch`, `Checkbox`, `FilterChip`, dropdown selection

### 6. UI state objects — 1:1 with functions?

**No.** Bind by **ownership / change frequency**, not “one data class per function forever.”

- One function may receive one section state (`DemoHeader(HeaderUiState)`)
- One section state may feed several small leaf composables (`StableLabel(text)`)

### 7. `LazyColumn` keys (and friends)

```kotlin
items(items = items, key = { it.id }) { … }
```

Also use stable keys with:

- `LazyRow` / `LazyGrid` / `LazyVerticalGrid`
- `item(key = …)` slots
- `key(id) { Row… }` for non-lazy tables

### 8. Labels / Text — avoid recomposition

Extract `StableLabel(text: String)` so leaves depend only on the primitive they draw.

### 9. Table as ViewComponents

`StatsTable` + `TableDataRow` keyed by `row.id`. Only changed rows should recompose.

### 10. Coroutines in ViewModel

```kotlin
viewModelScope.launch {
    val a = async(Dispatchers.IO) { … }      // parallel call 1
    val b = async(Dispatchers.IO) { … }      // parallel call 2
    val mapped = async(Dispatchers.Default) { … } // CPU work
    combine(a.await(), b.await(), mapped.await())
}
```

| API | Meaning |
|---|---|
| `viewModelScope.launch` | Coroutine cancelled when ViewModel is cleared |
| `Dispatchers.IO` | Network / disk / DB |
| `Dispatchers.Default` | CPU-bound work |
| `Dispatchers.Main` | UI / publishing state (default for `viewModelScope`) |
| `async` / `await` | Start concurrent work; await results |

### 11. Models: never use `var`

Use `data class` + `val` (+ `@Immutable`). Mutable `var` makes types unstable for Compose skippability.

### 12. Reading state when multiple VM functions update it

- **Write** via `_uiState.update { }`
- **Read** via `_uiState.value` inside the ViewModel (`submit()` reads username set by `onUsernameChange`)
- UI should **not** feed business state back into VM for readability — VM is the single source of truth

---

## Project structure

```text
app/src/main/java/com/example/composebestpractices/
├── MainActivity.kt
├── state/DemoUiState.kt          # immutable section states
├── viewmodel/DemoViewModel.kt    # StateFlow + dispatchers + async
└── ui/
    ├── DemoScreen.kt             # DemoHeader + Middle + DemoFooter
    ├── theme/Theme.kt
    └── components/
        ├── Header.kt             # DemoHeader + StableLabel
        ├── Footer.kt             # DemoFooter
        ├── MiddleView.kt
        ├── ProfileForm.kt
        └── StatsTable.kt
```

## Upload to GitHub

```bash
cd /Users/ayesha.mshahid/AndroidStudioProjects/compose-best-practices-demo
git add .
git commit -m "Initial Compose best-practices demo screen"
gh repo create compose-best-practices-demo --public --source=. --remote=origin --push
```

Or create an empty repo on GitHub, then:

```bash
git remote add origin git@github.com:<you>/compose-best-practices-demo.git
git branch -M main
git push -u origin main
```

## Requirements

- Android Studio Ladybug+ (or recent)
- JDK 17
- minSdk 26 / targetSdk 35
