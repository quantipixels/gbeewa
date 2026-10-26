# Gbéewá

[![Maven Central](https://img.shields.io/maven-central/v/com.quantipixels/gbeewa.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.quantipixels/gbeewa)
[![License](https://img.shields.io/github/license/quantipixels/gbeewa)](https://github.com/quantipixels/gbeewa/blob/main/LICENSE)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-blue?logo=kotlin)
![Android](https://img.shields.io/badge/Android-%E2%9C%94-green?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-%E2%9C%94-lightgrey?logo=apple&logoColor=white)

Gbéewá is the library name. Its repository and artifact use `gbeewa`, and its Kotlin package is `com.quantipixels.gbeewa`.

Gbéewá delivers typed, transient results to app-wired Compose Multiplatform consumers. It does not own navigation, identify a previous destination, or select a destination. A mounted consumer can receive a result on the same screen or after any navigation change while the shared result store remains alive.

The library uses a common runtime API. It does not require KSP, code generation, or a navigation-framework compiler plugin.

Quanti Pixels authors, maintains, and publishes this library. The library originated from the result mechanism in Yétúndé The Alárìná.

Version `0.1.0` supports Android and iOS. Future platform targets are added by request.

## Demo

https://github.com/user-attachments/assets/91bba794-1b87-497d-a1cd-9e5c4fc329f2

## Why Gbéewá

- **Omni-directional delivery:** Deliver current-to-previous, current-to-next, or current-to-current. Shared store ownership, key equality, and consumption timing select the receiver.
- **Typed result lanes:** Use `ResultKey<T>` instead of shared string keys and manual payload casts.
- **Parameterized keys:** Use data-class keys to isolate concurrent requests or screen instances.
- **Pure runtime API:** Add no KSP processor or generated navigation wrapper.
- **Framework agnostic:** Use the same contract with Navigation 2, Navigation 3, Decompose, Voyager, or app-owned navigation.
- **Compose Multiplatform:** Use the same common API on Android and iOS.

Gbéewá provides transient, one-consumer result delivery. It is not a broadcast channel or a replacement for durable application state.

## Navigation direction

`Result` describes the outcome of an action. It does not define a navigation direction.

`ResultStore.put` stores one pending value under a key. It does not address a screen. The first eligible consumer that uses a `ResultConsumer` backed by the same store and an equal key consumes that value. Therefore, a publication can deliver in these directions:

- current to previous: publish, then pop;
- current to current: publish while the consumer remains mounted, including between sibling composables that share the store;
- current to next: publish, then push a screen that consumes the key.

Other route relationships use the same rules. Navigation direction does not select the receiver. Shared `ResultStore` ownership, key equality, and consumption timing select it. Delivery is not a broadcast. If more than one consumer can observe an equal key, only the first consumer receives the pending value. A mounted consumer can consume it before a later screen enters composition. Use request-specific or parameterized keys when the receiver must be unambiguous.

```text
Producer A ── put(ResultKey<T>, value) ──► ResultStore
                                                │
                              equal key + first eligible consumer
                                                │
                                                ▼
                              Consumer B or a later destination
```

The producer and consumer can be on the same screen, on different mounted screens, or on screens separated by a navigation change. The diagram shows one-result consumption. It does not show a broadcast channel.

## Samples

The runnable Android and iOS showcase uses two flows derived from Yétúndé:

- a result from a pushed edit screen;
- two independent selection results from one shared producer, separated by a receiver ID.

These flows also cover country selection, dropdowns, conversation starters, media selection, audio recording, and chat actions because they use the same publish-before-dismiss contract.

One shared `samples:composeApp` module contains real integrations for [Decompose](samples/composeApp/src/commonMain/kotlin/com/quantipixels/gbeewa/showcase/DecomposeSample.kt), [typed Navigation 2](samples/composeApp/src/commonMain/kotlin/com/quantipixels/gbeewa/showcase/Navigation2Sample.kt), and [Voyager](samples/composeApp/src/commonMain/kotlin/com/quantipixels/gbeewa/showcase/VoyagerSample.kt). Each integration uses a real back stack. The app root owns `ResultStore`. Route callbacks publish before they pop. Screen composables receive plain value callbacks and do not receive the store. A parameterized key keeps the two selection receivers separate.

The app uses the library toolchain and depends directly on `:gbeewa`. It builds the Android application and the `GbeewaShowcase` frameworks for `iosArm64`, `iosSimulatorArm64`, and `iosX64`.

Open the repository root in Android Studio and sync it. Run the `composeApp` Android application. For iOS, open `samples/iosApp/iosApp.xcodeproj` and run the `iosApp` scheme. Both hosts render the same `ShowcaseApp` composable. The iOS host targets iOS 17.2.

Build and test the showcase with:

```shell
./gradlew \
  :samples:composeApp:testDebugUnitTest \
  :samples:composeApp:iosSimulatorArm64Test \
  :samples:composeApp:assembleDebug \
  :samples:composeApp:linkDebugFrameworkIosArm64 \
  :samples:composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Install

Add Maven Central to the consumer build. Then add the library to `commonMain`:

```kotlin
commonMain.dependencies {
    implementation("com.quantipixels:gbeewa:<version>")
}
```

## Quick start

Define a typed key. Create one store and consumer for the required communication lifetime. Keep the store at the navigation root. Provide only the consumer through the composition local:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.quantipixels.gbeewa.LocalResultConsumer
import com.quantipixels.gbeewa.ResultConsumer
import com.quantipixels.gbeewa.ResultKey
import com.quantipixels.gbeewa.ResultStore

object SelectedCountryKey : ResultKey<String>

@Composable
fun App() {
    val resultStore = remember { ResultStore() }
    val resultConsumer = remember(resultStore) { ResultConsumer(resultStore) }

    CompositionLocalProvider(LocalResultConsumer provides resultConsumer) {
        AppNavigation(resultStore)
    }
}
```

Receive the result as Compose state:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.quantipixels.gbeewa.rememberResultAsState

@Composable
fun CountryRoute(openPicker: () -> Unit) {
    val selectedCountry by rememberResultAsState(SelectedCountryKey) { "Ireland" }

    CountryScreen(
        selectedCountry = selectedCountry,
        onSelectCountry = openPicker,
    )
}
```

Publish before the navigation action that completes the handoff. The picker screen receives a plain callback and does not know about Gbéewá:

```kotlin
@Composable
fun CountryPickerRoute(
    resultStore: ResultStore,
    pop: () -> Unit,
) {
    CountryPickerScreen(
        onCountrySelected = { country ->
            resultStore.put(SelectedCountryKey, country)
            pop()
        },
    )
}
```

## Compatibility

The first release is built and verified with this matrix:

| Area | Support |
| --- | --- |
| Android | API 24 or later |
| Apple targets | `iosArm64`, `iosSimulatorArm64`, and `iosX64` |
| Kotlin | 2.1.21 |
| Compose Multiplatform | 1.8.2 |

The showcase iOS application targets iOS 17.2. This value is the host application deployment target, not a library delivery rule.

## Public API

| API | Purpose |
| --- | --- |
| `ResultKey<T>` | Defines a typed result lane. Key equality defines lane identity. |
| `ResultStore` | Publishes and retains the latest pending value for each equal key. |
| `ResultConsumer` | Provides callback and direct consume-only access to a store. |
| `PendingResult<T>` | Distinguishes a removed value from a missing pending result. |
| `LocalResultConsumer` | Provides a consumer to Compose descendants. |
| `ResultEffect` | Consumes one pending result in a non-suspending callback. |
| `rememberResultAsState` | Converts delivered results into read-only Compose state. |
| `rememberSaveableResultAsState` | Converts delivered results into read-only saveable Compose state with automatic or custom saving. |

Read the contracts in order: [key identity](gbeewa/src/commonMain/kotlin/com/quantipixels/gbeewa/ResultKey.kt)
→ [store lifetime](gbeewa/src/commonMain/kotlin/com/quantipixels/gbeewa/ResultStore.kt)
and [presence](gbeewa/src/commonMain/kotlin/com/quantipixels/gbeewa/PendingResult.kt)
→ [consumer](gbeewa/src/commonMain/kotlin/com/quantipixels/gbeewa/ResultConsumer.kt)
→ [effect](gbeewa/src/commonMain/kotlin/com/quantipixels/gbeewa/ResultEffect.kt)
→ [state helpers](gbeewa/src/commonMain/kotlin/com/quantipixels/gbeewa/RememberResultAsState.kt).
The focused [store tests](gbeewa/src/commonTest/kotlin/com/quantipixels/gbeewa/ResultStoreTest.kt)
and [Compose runtime tests](gbeewa/src/commonTest/kotlin/com/quantipixels/gbeewa/ResultEffectTest.kt)
show these contracts in use.

## Define a typed key

Key equality defines the result lane. An object is suitable for one shared lane. A data class is suitable when an identifier is part of the lane.

```kotlin
import com.quantipixels.gbeewa.ResultKey

object SelectedCountryKey : ResultKey<String>
data class EditedProfileKey(val profileId: String) : ResultKey<Boolean>
```

The key type controls the payload type at normal call sites.

## Choose store ownership and exposure

Create one `ResultStore` at the application or navigation root. Keep it for the communication lifetime that the app needs. Gbéewá does not require the store to be exposed to screen content.

### Transient handoffs and restored navigation

`ResultStore` lives in memory and is not serialized or restored. Restoring a navigation destination
does not restore its pending result. If a destination requires a transient handoff, choose its
missing-value recovery before starting dependent work. For example, return to an earlier step or
close the destination when the handoff is missing. Check and consume the handoff in an event
handler, effect, or app-owned entry boundary, never during composition or in a `remember` initializer.
Save durable identifiers through the app's own navigation or storage contract.

`rememberSaveableResultAsState` saves a receiver's UI value, not a pending handoff. Restoring that
value does not establish that a new result was delivered.

### Store exposure

The app chooses how far each capability travels:

- Keep `ResultStore` at the root and capture it in route-owned callbacks.
- Pass or inject `ResultStore` into a producer when direct publication is useful.
- Provide `ResultConsumer` through `LocalResultConsumer` for `ResultEffect` and the result-to-state helpers.
- Keep `ResultConsumer` beside the store and pass it explicitly when composition-local access is not appropriate.

This scaffold provides only the consume capability to its descendants. The `content` lambda does not receive the store:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.quantipixels.gbeewa.ResultConsumer
import com.quantipixels.gbeewa.ResultStore
import com.quantipixels.gbeewa.LocalResultConsumer

@Composable
fun AppScaffold(
    resultStore: ResultStore,
    content: @Composable () -> Unit,
) {
    val resultConsumer = remember(resultStore) {
        ResultConsumer(resultStore)
    }

    CompositionLocalProvider(
        LocalResultConsumer provides resultConsumer,
        // Keep the app's other existing providers here.
    ) {
        content()
    }
}
```

The route or application callback that publishes needs access to `ResultStore`. The screen that produces the value does not need access when it reports the value through a callback. The receiving route needs a `ResultConsumer`, usually through `LocalResultConsumer`.

### Android configuration changes

The `remember` example keeps the store across recomposition. It does not keep the store when Android recreates the activity after a configuration change.

Keep `ResultStore` in an app-owned `ViewModel` when pending results must survive configuration changes. `ResultConsumer` is a small facade over the store. It does not require separate lifecycle ownership. You can create it with `remember(resultStore)`, or keep it in the same `ViewModel` when that is more convenient:

```kotlin
import androidx.lifecycle.ViewModel
import com.quantipixels.gbeewa.ResultConsumer
import com.quantipixels.gbeewa.ResultStore

class AppResultViewModel : ViewModel() {
    val resultStore = ResultStore()
    val resultConsumer = ResultConsumer(resultStore)
}
```

Provide `resultConsumer` through `LocalResultConsumer`. Capture `resultStore` in route callbacks, or pass it to producers when the application chooses direct exposure. A `ViewModel` preserves this in-memory owner across Android configuration changes. It does not restore pending results after process death.

Apps that use Compose Runtime 1.10.0 or later can instead keep a composition-owned store with [`retain`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/retain/retain.composable). Add the `androidx.compose.runtime:runtime-retain` artifact to the app, then retain the store at the application or navigation root:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import com.quantipixels.gbeewa.LocalResultConsumer
import com.quantipixels.gbeewa.ResultConsumer
import com.quantipixels.gbeewa.ResultStore

@Composable
fun App() {
    val resultStore = retain { ResultStore() }
    val resultConsumer = remember(resultStore) { ResultConsumer(resultStore) }

    CompositionLocalProvider(LocalResultConsumer provides resultConsumer) {
        AppNavigation(resultStore)
    }
}
```

On Android, Compose installs a lifecycle-aware retained-values store at the composition root. It keeps `resultStore` in memory when an activity is recreated for a configuration change. Use a `ViewModel` when the store belongs to an app-owned lifecycle outside composition. Neither option restores pending results after process death.

## Receive a result

The application also chooses where the receive API appears. Install `ResultEffect` in a route when screen content must remain unaware of Gbéewá. Install it directly in a screen when that coupling is useful:

```kotlin
import androidx.compose.runtime.Composable
import com.quantipixels.gbeewa.ResultEffect

@Composable
fun CountryRoute(
    onCountrySelected: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    ResultEffect(SelectedCountryKey, onCountrySelected)
    content()
}
```

The route can pass ordinary values and callbacks to its screen content. The screen does not need a `ResultConsumer`, `ResultKey`, or `ResultStore`.

The handler is not a suspend function. Keep it quick. Update state directly, or launch longer work in a caller-owned scope that has the correct lifecycle and error policy.

Use direct consumption when an event handler, effect, or non-Compose function needs the removed
value as an expression:

```kotlin
import com.quantipixels.gbeewa.PendingResult
import com.quantipixels.gbeewa.ResultConsumer

fun openSelectedCountry(
    resultConsumer: ResultConsumer,
    openCountry: (String) -> Unit,
) {
    when (val result = resultConsumer.consume(SelectedCountryKey)) {
        PendingResult.Missing -> Unit
        is PendingResult.Value -> openCountry(result.value)
    }
}
```

Direct consumption removes the pending result before it returns. Do not call it during composition
because removal changes observable Compose state. Use `ResultEffect`, `rememberResultAsState`, or
`rememberSaveableResultAsState` for composition-owned delivery.

### Cancel or abandon a request

Use the existing `ResultConsumer.consume(key)` overload to remove a retired request's pending
value without invoking a result callback. Cleanup affects only that key, so other requests keep
their pending values. Run it on the UI or main context that drives Compose consumption, such as
the Cancel event handler:

```kotlin
import com.quantipixels.gbeewa.ResultConsumer
import com.quantipixels.gbeewa.ResultKey

data class EditNoteKey(val requestId: String) : ResultKey<String?>

fun cancelNoteEdit(
    consumer: ResultConsumer,
    key: EditNoteKey,
    dismiss: () -> Unit,
) {
    consumer.consume(key)
    dismiss()
}
```

The return value can be ignored for cleanup. If it matters, `PendingResult.Value(null)` means a
present nullable value was removed; `PendingResult.Missing` means no pending value existed.
Cleanup does not cancel the producer or reject later publications. The app must stop or ignore
late producer callbacks for a retired request and use distinct keys for overlapping requests.
No discard API, registry, session manager, or navigation policy is needed for this cleanup.

### Receive as state

Use `rememberResultAsState` when the result directly updates local Compose state. The `AsState` suffix distinguishes the returned `State<T>` from the root-owned `ResultStore`. Extra remember keys reset the initial value when their context changes:

```kotlin
val selectedCountry = rememberResultAsState(
    resultKey = SelectedCountryKey,
    userId,
) {
    profileCountry
}
```

Use `rememberSaveableResultAsState` when the destination must restore its initial or last delivered UI value through Compose saveable state:

```kotlin
val selectedCountry = rememberSaveableResultAsState(
    resultKey = SelectedCountryKey,
) {
    profileCountry
}
```

The automatic saver supports values accepted by the current `SaveableStateRegistry`. The helper preserves a delivered nullable result, including `null`. Keep saved values small and destination-scoped.

Provide a value-level `Saver` when the result type is not supported automatically:

```kotlin
val selectedCountry = rememberSaveableResultAsState(
    resultKey = SelectedCountryKey,
    saver = CountrySaver,
) {
    profileCountry
}
```

The custom saver overload passes `CountrySaver` to Compose as the mutable state's `stateSaver`. The overload without `saver` continues to use Compose automatic mutable-state saving.

This saveable helper does not save `ResultStore`, pending keys, or pending results. It restores the receiver's UI value without consuming or replaying a result. Live input changes reset the state. Compose restoration does not validate the inputs that existed when it saved the value.

Do not mount `ResultEffect` and either result-to-state helper for the same key when one receiver must own delivery.

## Keep the store outside screen content

A reusable screen can expose a value callback without depending on Gbéewá or a navigation framework. The route layer decorates that callback with publication and navigation:

```kotlin
@Composable
fun CountryPickerRoute(
    resultStore: ResultStore,
    pop: () -> Unit,
) {
    CountryPickerScreen(
        onSuccess = { country ->
            resultStore.put(SelectedCountryKey, country)
            pop()
        },
    )
}
```

`CountryPickerScreen` knows only `onSuccess(country)`. The route owns the store, the result key, the navigation action, and the required publish-before-pop order.

The same pattern works with a `ViewModel`, Decompose component, presenter, or other app-level owner. The callback can publish and then pop, push, replace, dismiss, or remain on the current screen.

## Direct store access is also valid

An application can pass or inject `ResultStore` into a producer when that boundary is more useful. Gbéewá does not enforce one exposure level:

```kotlin
fun selectCountry(
    resultStore: ResultStore,
    country: String,
    dismiss: () -> Unit,
) {
    resultStore.put(SelectedCountryKey, country)
    dismiss()
}
```

In both styles, publish the result before the application action that completes the handoff. Navigation is optional.

Do not publish when the intended receiver will not remain active or become active while the shared `ResultStore` still exists. For example, a flow that returns to the app root must not publish a result intended for an intermediate screen. If another mounted consumer uses an equal key, it can consume the value first.

## Nullable results

Null is a valid result only when the key declares a nullable payload:

```kotlin
object OptionalNoteKey : ResultKey<String?>

resultStore.put(OptionalNoteKey, null)
```

Presence is stored separately from the value. A pending `null` is delivered once. It is not treated as no result.
Direct consumption returns `PendingResult.Value(null)`. It returns `PendingResult.Missing` only when
the key has no pending result.

## Delivery contract

- The store keeps only the latest pending value for each equal key.
- Consumption removes the value before the handler runs.
- Direct consumption returns `PendingResult.Value` after removal, or `PendingResult.Missing` when no value exists.
- One result is consumed at most once, including when multiple effects compete for one key.
- A handler failure does not restore the result.
- A handler must not publish another value for its own key. That value can remain pending until the effect leaves and re-enters composition.
- A pending result remains available when no receiver is mounted.
- The library does not remove stale results automatically.
- The root owner discards pending results when it discards the `ResultStore`.
- The saveable helper can restore a receiver's delivered UI value. It does not restore pending results.
- Any app layer can own the store or call `put`, including a `ViewModel` or navigation component. The call must execute on the UI or main context that drives Compose consumption. The library does not switch contexts or synchronize concurrent access.

### Same-key publication inside a callback

Do not publish another value for the same key from inside its result callback. The replacement,
including an equal payload, can remain pending until the receiver leaves and re-enters composition.
Publishing twice retains only the latest pending value. There is no queue, automatic redelivery,
or retry guarantee.

## Contributing

Open an issue before a behavior change or new platform target. Keep navigation policy outside the library, add tests for contract changes, and update `CHANGELOG.md` for public behavior changes. Before a pull request, run:

```shell
./gradlew check apiCheck koverVerify
```

Contributions are licensed under Apache-2.0.

## License

Gbéewá is available under the Apache License 2.0. See [LICENSE](LICENSE).

## Project links

- [Changelog](CHANGELOG.md)
- [Security policy](SECURITY.md)
- [Issue tracker](https://github.com/quantipixels/gbeewa/issues)
- [Showcase source](samples/composeApp/src/commonMain/kotlin/com/quantipixels/gbeewa/showcase)
