package com.example.routineforge.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.routineforge.data.CompletedSession
import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineRepository
import com.example.routineforge.data.RoutineStep
import com.example.routineforge.data.StepType
import com.example.routineforge.service.RoutineTimerAction
import com.example.routineforge.service.RoutineTimerController
import com.example.routineforge.service.RoutineTimerService
import com.example.routineforge.util.AlertHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class RoutinePlayerUiState(
    val routine: Routine? = null,
    val currentStepIndex: Int = 0,
    val currentStepRemainingSeconds: Int = 0,
    val currentStepTotalSeconds: Int = 1,
    val categoryTotalSeconds: Int = 0,
    val categoryRemainingSeconds: Int = 0,
    val totalElapsedSeconds: Int = 0,
    val isPlaying: Boolean = false,
    val isCompleted: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibeEnabled: Boolean = true,
    val lastTransitionMessage: String? = null,
    val showTransitionBanner: Boolean = false
) {
    val currentStep: RoutineStep?
        get() = routine?.steps?.getOrNull(currentStepIndex)

    val nextStep: RoutineStep?
        get() = routine?.steps?.getOrNull(currentStepIndex + 1)

    // Current subtask countdown progress (0f to 1f)
    val stepProgress: Float
        get() = if (currentStepTotalSeconds > 0) {
            ((currentStepTotalSeconds - currentStepRemainingSeconds).toFloat() / currentStepTotalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    // Overall category/routine countdown progress (0f to 1f)
    val categoryProgress: Float
        get() = if (categoryTotalSeconds > 0) {
            ((categoryTotalSeconds - categoryRemainingSeconds).toFloat() / categoryTotalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val totalRoutineProgress: Float
        get() {
            val totalSteps = routine?.steps?.size ?: 1
            return (currentStepIndex.toFloat() + stepProgress) / totalSteps.toFloat()
        }

    fun formattedRemainingTime(): String {
        val minutes = currentStepRemainingSeconds / 60
        val seconds = currentStepRemainingSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun formattedCategoryRemainingTime(): String {
        val hours = categoryRemainingSeconds / 3600
        val minutes = (categoryRemainingSeconds % 3600) / 60
        val seconds = categoryRemainingSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formattedCategoryTotalTime(): String {
        val hours = categoryTotalSeconds / 3600
        val minutes = (categoryTotalSeconds % 3600) / 60
        val seconds = categoryTotalSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formattedElapsedTime(): String {
        val hours = totalElapsedSeconds / 3600
        val minutes = (totalElapsedSeconds % 3600) / 60
        val seconds = totalElapsedSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}

class RoutinePlayerViewModel(
    application: Application,
    private val repository: RoutineRepository,
    private val routineId: String
) : AndroidViewModel(application) {

    private val alertHelper = AlertHelper(application)
    private val _uiState = MutableStateFlow(RoutinePlayerUiState())
    val uiState: StateFlow<RoutinePlayerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadRoutine()
        viewModelScope.launch {
            RoutineTimerController.actions.collect { action ->
                when (action) {
                    RoutineTimerAction.TOGGLE_PLAY_PAUSE -> togglePlayPause()
                    RoutineTimerAction.NEXT_STEP -> skipToNextStep()
                    RoutineTimerAction.STOP -> {
                        pause()
                        RoutineTimerService.stop(getApplication())
                    }
                }
            }
        }
    }

    private fun updateNotification(state: RoutinePlayerUiState) {
        val routine = state.routine ?: return
        val currentStep = state.currentStep ?: return
        RoutineTimerService.update(
            context = getApplication(),
            routineId = routine.id,
            subtaskTitle = currentStep.title,
            subtaskRemainingSec = state.currentStepRemainingSeconds,
            categoryName = routine.title,
            stepIndex = state.currentStepIndex,
            stepTotalCount = routine.steps.size,
            categoryRemainingSec = state.categoryRemainingSeconds,
            isPlaying = state.isPlaying
        )
    }

    private fun loadRoutine() {
        val routine = repository.getRoutine(routineId)
        if (routine != null && routine.steps.isNotEmpty()) {
            val firstStep = routine.steps.first()
            val totalCatSeconds = routine.totalDurationSeconds
            _uiState.value = RoutinePlayerUiState(
                routine = routine,
                currentStepIndex = 0,
                currentStepRemainingSeconds = firstStep.durationSeconds,
                currentStepTotalSeconds = firstStep.durationSeconds,
                categoryTotalSeconds = totalCatSeconds,
                categoryRemainingSeconds = totalCatSeconds,
                totalElapsedSeconds = 0,
                isPlaying = false,
                lastTransitionMessage = null,
                showTransitionBanner = false
            )
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_uiState.value.isCompleted) return
        val newState = _uiState.value.copy(isPlaying = true)
        _uiState.value = newState
        updateNotification(newState)
        startTimerJob()
    }

    fun pause() {
        val newState = _uiState.value.copy(isPlaying = false)
        _uiState.value = newState
        timerJob?.cancel()
        timerJob = null
        updateNotification(newState)
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isPlaying) {
                delay(1000L)
                tick()
            }
        }
    }

    private fun tick() {
        val state = _uiState.value
        val routine = state.routine ?: return

        val newStepRemaining = state.currentStepRemainingSeconds - 1
        val newCatRemaining = (state.categoryRemainingSeconds - 1).coerceAtLeast(0)
        val newElapsed = state.totalElapsedSeconds + 1

        // 3-2-1 warning countdown sound & haptic
        if (newStepRemaining in 1..3) {
            alertHelper.playWarningTick(state.soundEnabled, state.vibeEnabled)
        }

        if (newStepRemaining <= 0) {
            // Current sub-task completed!
            val completedStep = routine.steps.getOrNull(state.currentStepIndex)
            val nextIndex = state.currentStepIndex + 1

            if (nextIndex < routine.steps.size) {
                // Advance to next step while keeping category timer ticking!
                val nextStep = routine.steps[nextIndex]
                alertHelper.playStepTransition(state.soundEnabled, state.vibeEnabled)
                val transitionPrompt = "Aapka \"${completedStep?.title ?: "Task"}\" khatam ho gaya! Ab aap \"${nextStep.title}\" shuru karein."

                _uiState.value = state.copy(
                    currentStepIndex = nextIndex,
                    currentStepRemainingSeconds = nextStep.durationSeconds,
                    currentStepTotalSeconds = nextStep.durationSeconds,
                    categoryRemainingSeconds = newCatRemaining,
                    totalElapsedSeconds = newElapsed,
                    lastTransitionMessage = transitionPrompt,
                    showTransitionBanner = true
                )
                updateNotification(_uiState.value)
            } else {
                // Entire routine fully completed!
                finishRoutine(routine, newElapsed)
            }
        } else if (newCatRemaining <= 0) {
            // Category total time reached!
            finishRoutine(routine, newElapsed)
        } else {
            // Normal dual parallel tick
            _uiState.value = state.copy(
                currentStepRemainingSeconds = newStepRemaining,
                categoryRemainingSeconds = newCatRemaining,
                totalElapsedSeconds = newElapsed
            )
            updateNotification(_uiState.value)
        }
    }

    private fun finishRoutine(routine: Routine, elapsedSeconds: Int) {
        pause()
        RoutineTimerService.stop(getApplication())
        alertHelper.playRoutineComplete(_uiState.value.soundEnabled, _uiState.value.vibeEnabled)
        _uiState.value = _uiState.value.copy(
            currentStepRemainingSeconds = 0,
            categoryRemainingSeconds = 0,
            isCompleted = true,
            isPlaying = false,
            totalElapsedSeconds = elapsedSeconds,
            lastTransitionMessage = "Pura Category Session khatam ho gaya! Shandar kaam!",
            showTransitionBanner = true
        )

        // Log session in history
        val category = repository.categories.value.firstOrNull { it.id == routine.categoryId }
        val session = CompletedSession(
            id = UUID.randomUUID().toString(),
            routineId = routine.id,
            routineTitle = routine.title,
            categoryName = category?.name ?: "General",
            categoryEmoji = category?.emoji ?: "🎯",
            categoryColorHex = routine.colorHex,
            completedAt = System.currentTimeMillis(),
            durationSeconds = elapsedSeconds,
            stepsCompleted = routine.steps.size,
            totalSteps = routine.steps.size
        )
        repository.logSession(session)
    }

    fun dismissTransitionBanner() {
        _uiState.value = _uiState.value.copy(showTransitionBanner = false)
    }

    fun skipToNextStep() {
        val state = _uiState.value
        val routine = state.routine ?: return
        val nextIndex = state.currentStepIndex + 1

        if (nextIndex < routine.steps.size) {
            val completedStep = state.currentStep
            val nextStep = routine.steps[nextIndex]
            alertHelper.playStepTransition(state.soundEnabled, state.vibeEnabled)

            // Recalculate remaining category time from next step onwards
            val remainingFromNext = routine.steps.drop(nextIndex).sumOf { it.durationSeconds }

            _uiState.value = state.copy(
                currentStepIndex = nextIndex,
                currentStepRemainingSeconds = nextStep.durationSeconds,
                currentStepTotalSeconds = nextStep.durationSeconds,
                categoryRemainingSeconds = remainingFromNext,
                lastTransitionMessage = "Switched to: ${nextStep.title}",
                showTransitionBanner = true
            )
            updateNotification(_uiState.value)
        } else {
            finishRoutine(routine, state.totalElapsedSeconds)
        }
    }

    fun skipToPreviousStep() {
        val state = _uiState.value
        val routine = state.routine ?: return
        val prevIndex = (state.currentStepIndex - 1).coerceAtLeast(0)
        val prevStep = routine.steps[prevIndex]

        val remainingFromPrev = routine.steps.drop(prevIndex).sumOf { it.durationSeconds }

        _uiState.value = state.copy(
            currentStepIndex = prevIndex,
            currentStepRemainingSeconds = prevStep.durationSeconds,
            currentStepTotalSeconds = prevStep.durationSeconds,
            categoryRemainingSeconds = remainingFromPrev,
            lastTransitionMessage = "Back to: ${prevStep.title}",
            showTransitionBanner = true
        )
        updateNotification(_uiState.value)
    }

    fun restartCurrentStep() {
        val state = _uiState.value
        val currentStep = state.currentStep ?: return
        val diff = currentStep.durationSeconds - state.currentStepRemainingSeconds

        _uiState.value = state.copy(
            currentStepRemainingSeconds = currentStep.durationSeconds,
            currentStepTotalSeconds = currentStep.durationSeconds,
            categoryRemainingSeconds = (state.categoryRemainingSeconds + diff).coerceAtMost(state.categoryTotalSeconds)
        )
        updateNotification(_uiState.value)
    }

    fun addSecondsToCurrentStep(secondsToAdd: Int) {
        val state = _uiState.value
        val updatedStep = (state.currentStepRemainingSeconds + secondsToAdd).coerceAtLeast(1)
        val updatedStepTotal = (state.currentStepTotalSeconds + secondsToAdd).coerceAtLeast(updatedStep)
        val updatedCat = (state.categoryRemainingSeconds + secondsToAdd).coerceAtLeast(1)
        val updatedCatTotal = (state.categoryTotalSeconds + secondsToAdd).coerceAtLeast(updatedCat)

        _uiState.value = state.copy(
            currentStepRemainingSeconds = updatedStep,
            currentStepTotalSeconds = updatedStepTotal,
            categoryRemainingSeconds = updatedCat,
            categoryTotalSeconds = updatedCatTotal
        )
        updateNotification(_uiState.value)
    }

    fun toggleSound() {
        _uiState.value = _uiState.value.copy(soundEnabled = !_uiState.value.soundEnabled)
    }

    fun toggleVibration() {
        _uiState.value = _uiState.value.copy(vibeEnabled = !_uiState.value.vibeEnabled)
    }

    fun resetRoutine() {
        pause()
        RoutineTimerService.stop(getApplication())
        loadRoutine()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        alertHelper.release()
        RoutineTimerService.stop(getApplication())
    }
}

