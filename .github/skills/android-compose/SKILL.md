---
name: android-compose
description: Reference for CashClone Android app's file structure, naming, state management, navigation, and testing conventions (Kotlin/Jetpack Compose, package com.cashclone.app). Use when asked where does this file go, how is state managed, how is navigation wired, what's the naming convention for a screen/viewmodel, or how are tests structured in the Android client.
---

# CashClone Android conventions

Module: `android/app/src/main/kotlin/com/cashclone/app/`.

## FILE STRUCTURE
- `data/model/Models.kt` — all `@Serializable` DTOs in one file, mirroring `backend/.../models/Dto.kt`.
- `data/remote/` — `ApiService.kt` (Retrofit interface), `RetrofitClient.kt`, `TokenStore.kt`.
- `data/repository/CashCloneRepository.kt` — single repository wrapping the API + token store.
- `di/AppContainer.kt` — hand-rolled DI (no Hilt): builds `TokenStore` → `RetrofitClient` → `CashCloneRepository`.
- `viewmodel/` — one file per ViewModel, plus shared `UiState.kt` and `ViewModelFactory.kt`.
- `ui/screens/<feature>/<Feature>Screen.kt` — one package per feature (`auth`, `home`, `send`, `request`, `history`, `kyc`, `profile`), one screen composable per file.
- `ui/nav/AppNav.kt` — single nav graph. `ui/theme/Theme.kt` — Material3 theme.

## COMPONENT / CLASS NAMING & ORGANIZATION
- Screens: `<Feature>Screen` composable, e.g. `SendScreen(viewModel: SendRequestViewModel, onDone: () -> Unit)` in `ui/screens/send/SendScreen.kt` — callbacks are trailing lambdas named `onDone`/`onLogin`/`onSend` etc., not exposed as event sealed classes.
- ViewModels: `<Feature>ViewModel : ViewModel()`, e.g. `SendRequestViewModel` shared by both `SendScreen` and `RequestScreen`. Constructed via `GenericViewModelFactory` (`viewmodel/ViewModelFactory.kt`), which maps `KClass -> (CashCloneRepository) -> ViewModel`.
- Routes: private `object Routes` with `const val` string constants inside `AppNav.kt` (not a sealed class).

## STATE MANAGEMENT
- Shared generic `sealed interface UiState<out T>` (`viewmodel/UiState.kt`): `Idle`, `Loading`, `Success<T>(data: T)`, `Error(message: String)`. ViewModels expose `MutableStateFlow<UiState<X>>` privately and a `StateFlow` publicly via `.asStateFlow()`.
- Pattern: set `Loading`, run `repository` call in `viewModelScope.launch { try { ... } catch (e: Exception) { UiState.Error(e.toUserMessage()) } }` (see `SendRequestViewModel.send`).
- Screens read state with `collectAsState()` and branch with `is UiState.X` checks directly in Compose (e.g. `SendScreen.kt`), not a separate reducer.
- Local input fields use `remember { mutableStateOf("") }`, not hoisted to the ViewModel.

## NAVIGATION
- `androidx.navigation.compose` `NavHost`/`composable` in `AppNav.kt`, one `NavHostController` from `rememberNavController()`. Screen-to-screen flow via callback params (`onSend`, `onProfile`), not routes passed into screens. Post-login/logout resets stack with `popUpTo(0)`.

## TESTING PATTERNS
- None exist yet — there is no `androidTest`/`test` source set under `android/` in this repo. Don't assume a testing convention; flag new tests as the first of their kind rather than matching a pattern that isn't there.
