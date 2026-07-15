# Best Practices — Composition

Simple guide for Jetpack Compose: stop extra redraws, keep state clean, run work off the main thread.

Each section has a **mini example** so juniors can copy the idea quickly.

### Contents (all in this one file)

1. How Compose redraws the UI  
2. Split the screen into functions  
3. What to pass into each function  
4. Group UI that changes together  
5. remember / rememberSaveable  
6. **TextField inside a list**  
7. **How many UI states on one screen**  
8. **API calls — load on open vs button click**  
9. LazyColumn unique keys (+ other components)  
10. Labels / TextFields  
11. Table → ViewComponent  
12. Background work (dispatchers, async)  
13. Models & sharing state in ViewModel  

---

## How Compose redraws the UI

**Easy idea:** When data changes, Compose does **not** redraw the whole app. It redraws only the parts that used that data.

### Words you will hear

| Word | In simple words | Why care |
| --- | --- | --- |
| Composition Tree | Screen → Header, Middle, Footer → small widgets | One branch can update; others can stay |
| Slot Table | Compose’s memory of what you already built | Reuses old work |
| Snapshot State | Data Compose watches (`mutableStateOf`, StateFlow) | Knows who read it |
| State read | UI looks at a value | That UI updates when it changes |
| State write | You change a value | Only readers of that value update |
| Invalidation | Mark “needs update” | Dirty parts redraw; clean can skip |
| Recomposer | Applies updates on next frame | Smooth batching |

### Mini example — good vs bad

```kotlin
// GOOD — Header only gets title (will NOT redraw when username changes)
@Composable
fun Header(title: String) {
    Text(title)
}

// BAD — Header reads whole state (may redraw on every key press)
@Composable
fun Header(uiState: ScreenUiState) {
    Text(uiState.header.title) // still depends on full object often being new
}
```

**One line:** Change data → Compose finds who read it → only those parts redraw.

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Pass Header only what it needs (`title`) | Pass full `uiState` into Header |
| Keep siblings separate (Header / Middle / Footer) | One giant composable that reads everything |
| Use stable / equal params so Compose can skip | New unstable lambdas / `var` models everywhere |

---

## Split the screen into functions

Do not put the whole screen in one big function. Split like three boxes.

| Function | Job | Updates when |
| --- | --- | --- |
| `Header()` | Top: title | Title changes |
| `MiddleView()` | Form, list | Typing / list reload |
| `Footer()` | Status + buttons | Loading / canSave |

### Mini example

```kotlin
@Composable
fun ProfileScreen(uiState: ScreenUiState, onNameChange: (String) -> Unit) {
    Column {
        Header(title = uiState.header.title)              // only title
        MiddleView(form = uiState.form, onNameChange = onNameChange)
        Footer(status = uiState.footer.status)            // only footer data
    }
}
```

**Goal:** Typing name should not redraw Header if title did not change.

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Make `Header`, `Middle`, `Footer` separate `@Composable` functions | One huge Screen that reads every field |
| Give each part only its own data | Pass the full ViewModel into every small widget |
| Keep small pieces (one text, one field, one row) | Mix form + list + buttons all in one function |

---

## What should you pass into each function?

| Choice | Example | Use when |
| --- | --- | --- |
| Just values | `title: String` | Tiny widget |
| Small state | `headerState` | Few related fields |
| State + actions | `state` + `onSave` | Many clicks / fields |
| Whole screen state | Full `uiState` into Header | Usually **bad** |
| Whole ViewModel | `ProfileForm(viewModel)` | Usually **bad** |

### Mini example — more than 20 values

```kotlin
// BAD — too many params
fun AddressForm(line1, line2, city, zip, country, … /* 20 params */)

// GOOD — group them
data class AddressState(
    val line1: String,
    val line2: String,
    val city: String,
    val zip: String,
    val country: String
)

@Composable
fun AddressForm(state: AddressState, onChange: (AddressState) -> Unit) { … }
```

### Do / Don’t (more than 20 values)

| Do | Don’t |
| --- | --- |
| Make groups: `ProfileState`, `AddressState`, `PrefsState` | One giant object with 40 fields |
| Pass one group + its actions | 20 separate parameters on one function |
| Break UI into smaller composables | One function that reads everything |

**Tip:** Few clicks? `onRefresh = viewModel::refresh`. Many? Small Actions object. Both OK.

---

## Group UI that changes together

If two things change for the same reason, keep them together.

| Group | Examples | Why |
| --- | --- | --- |
| Header | title, subtitle | Load/refresh |
| Form | name, email | Every key press |
| List | items | Refresh / click |
| Footer | loading, canSave | Save / validation |

### Mini example

```kotlin
data class ScreenUiState(
    val header: HeaderState,
    val form: FormState,     // typing updates only this slice
    val list: List<Item>,
    val footer: FooterState
)
```

---

## remember and rememberSaveable

| Tool | After redraw? | After rotate? | Use for |
| --- | --- | --- | --- |
| `remember` | Yes | No | Tiny UI hint you can lose |
| `rememberSaveable` | Yes | Yes | UI-only flag (tip open/closed) |
| ViewModel | Yes | Yes (VM alive) | Real data: name, list, loading |

**Not always `rememberSaveable`.** Business data → ViewModel.

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Use ViewModel for name, list, loading, errors | Put business form data only in `rememberSaveable` |
| Use `rememberSaveable` for UI-only flags (tip open) | Use `rememberSaveable` for everything |
| Hoist TextField value to parent / ViewModel | Keep important text only inside the field with local state |

### Mini example

```kotlin
@Composable
fun ProfileForm(name: String, onNameChange: (String) -> Unit) {
    // UI-only — OK in rememberSaveable
    var tipOpen by rememberSaveable { mutableStateOf(false) }

    // Real data — comes from ViewModel (NOT rememberSaveable)
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Name") }
    )
}
```

### TextField and friends — mini examples

```kotlin
// TextField
OutlinedTextField(value = name, onValueChange = onNameChange)

// Switch
Switch(checked = isDark, onCheckedChange = onDarkChange)

// Checkbox
Checkbox(checked = agreed, onCheckedChange = onAgreedChange)

// Slider
Slider(value = volume, onValueChange = onVolumeChange)
```

**Easy rule:** UI shows the value. UI asks parent to change it.

---

## TextField inside a list (LazyColumn)

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Keep text on item in ViewModel (`item.note`) | `remember` only inside the row |
| Update by `id` + `copy` | Use only index |
| `key = { item.id }` | Index keys |
| Pass stable callback (`onItemNoteChange`) | New lambda every redraw `{ n -> … }` in parent |
| Separate title click from TextField | Make whole card clickable over the TextField |

### Mini example — ViewModel

```kotlin
data class Item(val id: String, val title: String, val note: String)

fun onItemNoteChange(id: String, note: String) {
    _uiState.update { state ->
        val newList = state.items.map { item ->
            if (item.id == id) item.copy(note = note) else item // same object if unchanged
        }
        state.copy(items = newList)
    }
}
```

### Mini example — UI row

```kotlin
items(items = items, key = { it.id }) { item ->
    ServiceRow(
        item = item,
        onNoteChange = onItemNoteChange  // stable — NOT { n -> onItemNoteChange(item.id, n) } in parent
    )
}

@Composable
fun ServiceRow(item: Item, onNoteChange: (String, String) -> Unit) {
    OutlinedTextField(
        value = item.note,
        onValueChange = { text -> onNoteChange(item.id, text) }
    )
}
```

**Does a new list redraw every row?** New list is OK. Unchanged items keep same object → those rows can **skip**. Only edited row must update.

---

## How to manage UI state

| Question | Simple answer |
| --- | --- |
| 1 state per function? | No — group by what changes together |
| Shape? | One big state = small parts (header + form + list + footer) |
| Observe where? | Once on the screen, pass pieces down |
| Update how? | `data class` + `copy()` — no `var` |

### How many UI states on one screen?

| Approach | Recommendation |
| --- | --- |
| Root states | Prefer **1** `StateFlow` / screen |
| Pieces inside | About **3–6** (header, form, list, footer, dialog?) |
| Per TextField flow? | **No** |

### Mini example

```kotlin
// ONE root state
data class ScreenUiState(
    val header: HeaderState,
    val form: FormState,
    val items: List<Item>,
    val footer: FooterState,
    val dialog: DialogState? = null
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScreenUiState(...))
    val uiState = _uiState.asStateFlow()
}

@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    // pass uiState.header, uiState.form, … — not whole vm into every child
}
```

### Do / Don’t

| Do | Don’t |
| --- | --- |
| One screen → one ViewModel → one main `uiState` | New `StateFlow` for every TextField |
| Nest 3–6 slices (header, form, list, footer…) | One flat bag of 50 unrelated fields |
| Collect once on screen, pass pieces down | Pass whole ViewModel into every child |
| Use `data class` + `copy()` | Mutate with `var` |

---

## API calls on screen — load on open vs button click

You often have **two different API moments**:

| When | Example | Who starts it |
| --- | --- | --- |
| User **opens** the screen | Load profile / list to show | ViewModel `init { }` (or one `load()` ) |
| User **taps a button** | Save, Submit, Refresh | `viewModel.onSaveClick()` |

**Still use one screen `UiState`.** Do not make two separate sources of truth. Use **two loading flags** so the UI can show:

- full-screen / list loading on open  
- button spinner on click  

### Simple UiState shape (easy for juniors)

```kotlin
data class ScreenUiState(
    // Data to show
    val items: List<Item> = emptyList(),
    val userName: String = "",

    // API #1 — when screen opens
    val isLoadingScreen: Boolean = true,
    val screenError: String? = null,

    // API #2 — when button is clicked
    val isSubmitting: Boolean = false,
    val submitMessage: String? = null,
    val submitError: String? = null
)
```

**Why two flags?**

| Flag | Meaning |
| --- | --- |
| `isLoadingScreen` | First API running → show list shimmer / full loader |
| `isSubmitting` | Button API running → disable button + small spinner |

If you use only one `isLoading`, a button press can look like the whole screen is loading again.

### Mini example — ViewModel

```kotlin
class ProfileViewModel(
    private val api: ProfileApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // API #1: user navigated to this screen
        loadScreenData()
    }

    fun loadScreenData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingScreen = true, screenError = null)
            }
            try {
                // Two APIs on open (both needed to draw the screen)
                val profileDeferred = async(Dispatchers.IO) { api.fetchProfile() }
                val itemsDeferred = async(Dispatchers.IO) { api.fetchItems() }

                val profile = profileDeferred.await()
                val items = itemsDeferred.await()

                _uiState.update {
                    it.copy(
                        userName = profile.name,
                        items = items,
                        isLoadingScreen = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingScreen = false,
                        screenError = e.message ?: "Load failed"
                    )
                }
            }
        }
    }

    // API #2: button click
    fun onSaveClick() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            if (snapshot.isSubmitting || snapshot.isLoadingScreen) return@launch

            _uiState.update {
                it.copy(isSubmitting = true, submitError = null, submitMessage = null)
            }
            try {
                withContext(Dispatchers.IO) {
                    api.save(snapshot.userName)
                }
                _uiState.update {
                    it.copy(isSubmitting = false, submitMessage = "Saved!")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = e.message ?: "Save failed"
                    )
                }
            }
        }
    }
}
```

### Mini example — UI

```kotlin
@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    when {
        state.isLoadingScreen -> FullScreenLoader()
        state.screenError != null -> ErrorBox(
            message = state.screenError!!,
            onRetry = vm::loadScreenData
        )
        else -> {
            Column {
                Text(state.userName)
                ItemList(state.items)

                Button(
                    onClick = vm::onSaveClick,
                    enabled = !state.isSubmitting
                ) {
                    if (state.isSubmitting) CircularProgressIndicator()
                    else Text("Save")
                }

                state.submitMessage?.let { Text(it) }
                state.submitError?.let { Text(it) }
            }
        }
    }
}
```

### Quick rules

| Question | Answer |
| --- | --- |
| Where do we call the API? | **ViewModel**, not in the Composable body |
| Open screen API | `init { loadScreenData() }` |
| Button API | `onSaveClick()` from the button |
| How many UiStates? | Still **one** root `ScreenUiState` |
| Two APIs on open (both needed to show UI) | `async` + `await` together |
| One on open + one on click | Separate functions + **separate loading flags** |
| Can submit use data loaded earlier? | Yes — read `_uiState.value` in ViewModel |

### Optional: status objects (same idea)

```kotlin
sealed interface LoadState {
    data object Idle : LoadState
    data object Loading : LoadState
    data class Error(val message: String) : LoadState
    data object Success : LoadState
}

data class ScreenUiState(
    val items: List<Item> = emptyList(),
    val screenLoad: LoadState = LoadState.Loading, // open screen
    val submitLoad: LoadState = LoadState.Idle     // button
)
```

Same pattern — two statuses inside **one** UiState.

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Call APIs in the **ViewModel** | Call network APIs inside the Composable body |
| Use `isLoadingScreen` for open-screen API | Use one `isLoading` for both open and button |
| Use `isSubmitting` for button API | Blank the whole screen on Save click |
| `async` for two APIs needed on open | Always call two open APIs one-after-another if they are independent |
| Read saved data from `_uiState.value` on submit | Ask UI to send back every field ViewModel already has |

---

## LazyColumn → unique key — what other components need?

Same idea everywhere: **stable unique id**, not only index.

| Component | What to use |
| --- | --- |
| **LazyColumn** | `items(list, key = { it.id })` |
| **LazyRow** | same |
| **LazyGrid** | unique key per cell |
| **`item { }`** | `item(key = "form") { … }` |
| **Column / forEach** | `key(id) { Row(…) }` |
| **Pager** | key by id, not only 0,1,2 |

### Mini examples

```kotlin
// LazyColumn
LazyColumn {
    items(items = users, key = { it.id }) { user ->
        UserRow(user)
    }
}

// LazyRow
LazyRow {
    items(items = chips, key = { it.id }) { chip ->
        ChipItem(chip)
    }
}

// One block inside LazyColumn
item(key = "header") {
    Header(title)
}

// Normal Column (not lazy)
Column {
    rows.forEach { row ->
        key(row.id) {
            TableRow(row)
        }
    }
}
```

**Bad:** `key = { index }` only → after reorder, wrong row keeps old TextField text.

### Do / Don’t

| Do | Don’t |
| --- | --- |
| `key = { it.id }` (stable unique id) | Key only by index `0, 1, 2…` |
| Same idea for LazyRow / LazyGrid / Pager | Forget keys on non-lazy `forEach` tables |
| `item(key = "form")` for section blocks | Reuse the same key for different items |

---

## Labels / TextFields → avoid recomposition

| UI | What to do |
| --- | --- |
| Label / Text | Small function with only the `String` |
| TextField | Small function; hoist `value` + `onValueChange` |
| Switch / Slider / Chip | Same hoist idea |

### Mini examples

```kotlin
// Label — only gets the string
@Composable
fun StableLabel(text: String) {
    Text(text)
}

// TextField — own small composable
@Composable
fun NameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Name") }
    )
}

// Usage
StableLabel(text = title)           // skips if title same
NameField(value = name, onValueChange = onNameChange)
```

**Goal:** Changing name should not force unrelated labels to re-run.

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Small `StableLabel(text)` with only the string | Pass whole screen state into a simple Text |
| Isolate each TextField (`value` + `onValueChange`) | Bury fields inside a huge parent that also reads list state |
| Hoist Switch / Slider / Chip the same way | Keep business values only in local composable state |

---

## Table → ViewComponent (avoid recomposition)

| Piece | What to do |
| --- | --- |
| Outer table | Gets list of rows only |
| Each row | Own **ViewComponent** |
| Identity | `key(row.id)` |
| Big table | Prefer Lazy + keys |

### Mini example

```kotlin
data class TableRow(val id: String, val label: String, val value: String)

@Composable
fun StatsTable(rows: List<TableRow>) {
    Column {
        rows.forEach { row ->
            key(row.id) {
                TableRowView(row)   // ViewComponent
            }
        }
    }
}

@Composable
fun TableRowView(row: TableRow) {
    Row {
        Text(row.label)
        Text(row.value)
    }
}
```

If only one row’s `value` changes → other `TableRowView`s can skip.

### Do / Don’t

| Do | Don’t |
| --- | --- |
| One ViewComponent per row (`TableRowView`) | One giant table composable that reads form + list + footer |
| `key(row.id) { … }` | No keys / index-only keys |
| Pass only that row’s data | Pass entire `ScreenUiState` into every row |
| LazyColumn for big tables | Hundreds of rows in a normal Column |

---

## Background work in ViewModel

Do not run slow work on the UI thread.

| Tool | Plain meaning | Use for |
| --- | --- | --- |
| `Dispatchers.Main` | UI thread | Update screen state |
| `Dispatchers.IO` | Wait on network/disk | API, DB, files |
| `Dispatchers.Default` | Heavy CPU | Sort, parse |
| `viewModelScope.launch` | Stop when ViewModel dies | Load / save |
| `async` + `await` | Start together, wait later | Two APIs at once |

### Mini example — viewModelScope.launch

```kotlin
fun load() {
    viewModelScope.launch {
        // cancelled automatically when user leaves and VM is cleared
        val data = withContext(Dispatchers.IO) { api.fetch() }
        _uiState.update { it.copy(items = data) }
    }
}
```

### Mini example — async (concurrent / parallel)

```kotlin
fun refresh() {
    viewModelScope.launch {
        // CONCURRENT = both start now (overlap)
        val itemsDeferred = async(Dispatchers.IO) { api.fetchItems() }
        val statsDeferred = async(Dispatchers.IO) { api.fetchStats() }

        val items = itemsDeferred.await()
        val stats = statsDeferred.await()

        _uiState.update { it.copy(items = items, stats = stats) }
    }
}
```

**Simple words:**

- **Concurrent** = started together, overlapping  
- **Parallel** = really running at same time on different threads (IO/Default pool)

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Use `viewModelScope.launch` for screen work | Start fire-and-forget jobs that outlive the ViewModel |
| `Dispatchers.IO` for API / DB | Heavy network on Main (UI freeze) |
| `async` when two calls are independent | Wait for A then B when they could run together |
| Publish UI state on Main / after await | Update Compose state from random threads without care |

---

## Models and sharing state in ViewModel

| Rule | Why |
| --- | --- |
| Use `val`, not `var` | `var` → unstable → more redraws |
| `data class` + `copy()` | Clean new snapshot each update |
| `@Immutable` when safe | Helps Compose skip |

### Mini example — no var

```kotlin
// BAD
class FormState {
    var username: String = ""
}

// GOOD
data class FormState(val username: String = "")
```

### Mini example — another function can read updated state

```kotlin
fun onUsernameChange(value: String) {
    _uiState.update { it.copy(form = it.form.copy(username = value)) }
}

fun submit() {
    // YES — read from ViewModel state (best practice)
    val name = _uiState.value.form.username
    viewModelScope.launch(Dispatchers.IO) {
        api.save(name)
    }
}

// Usually NOT needed:
// fun submit(usernameFromUi: String)  ← UI should not re-send what VM already has
```

### Do / Don’t

| Do | Don’t |
| --- | --- |
| Use `val` + `data class` + `copy()` | Use `var` on UI models |
| Read shared data from `_uiState.value` in ViewModel | Ask UI to pass the same data back every time |
| All updates go through one state holder | Several places mutating the same object differently |

**Say to juniors:** UI sends events (“user typed”, “pressed save”). ViewModel keeps the truth.

