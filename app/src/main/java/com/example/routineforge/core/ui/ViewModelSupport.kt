package com.example.routineforge.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.routineforge.AppContainer
import com.example.routineforge.container
import com.example.routineforge.data.ValidationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** App-wide snackbar, so any screen can report a result or an error. */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** ViewModel with the app container, created through the regular ViewModel store. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val c = LocalContext.current.container
    return viewModel(key = key) { create(c) }
}

/** Base for screens that write data: failures become readable messages instead of crashes. */
abstract class FFViewModel : ViewModel() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    protected fun say(message: String) {
        _messages.tryEmit(message)
    }

    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ValidationException) {
            say(e.message ?: "Please check your input")
        } catch (e: IllegalArgumentException) {
            say(e.message ?: "Please check your input")
        } catch (e: Exception) {
            say("Something went wrong. Please try again.")
        }
    }

    protected fun <T> Flow<T>.stateIn(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}

/** Emits today's date, and again after midnight (checked every 30 s). */
fun todayFlow(): Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now())
        delay(30_000)
    }
}.distinctUntilChanged()

/** Shows the ViewModel's messages in the app snackbar. */
@Composable
fun ShowMessages(vm: FFViewModel) {
    val snackbar = LocalSnackbar.current
    androidx.compose.runtime.LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }
}
