package com.example.futebolsabado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { SETUP, GAME }

data class Player(val id: Int, val name: String)
data class Team(val id: Int, val name: String, val players: List<Player>)

data class AppState(
    val players: List<Player> = emptyList(),
    val currentScreen: Screen = Screen.SETUP,
    val teamA: Team? = null,
    val teamB: Team? = null,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    // Fila INDIVIDUAL — perdedores vão pro fim, próximo time = primeiros 5
    val playerQueue: List<Player> = emptyList(),
    val timerDurationMinutes: Int = 10,
    val timerRemainingSeconds: Int = 600,
    val isTimerRunning: Boolean = false,
    val winnerMessage: String? = null
)

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var playerCounter = 0
    private var teamCounter = 0
    private var timerJob: Job? = null

    fun addPlayer(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        playerCounter++
        val newPlayer = Player(playerCounter, trimmed)

        _state.update { state ->
            if (state.currentScreen == Screen.SETUP) {
                state.copy(
                    players = state.players + newPlayer
                )
            } else {
                val newQueue = state.playerQueue + newPlayer

                state.copy(
                    players = state.players + newPlayer,
                    playerQueue = newQueue
                )
            }
        }

        tryFormMissingTeam()
    }

    private fun tryFormMissingTeam() {
        val s = _state.value

        if (s.currentScreen != Screen.GAME) return
        if (s.playerQueue.size < 5) return

        val hasMissingTeamA = s.teamA == null
        val hasMissingTeamB = s.teamB == null

        if (!hasMissingTeamA && !hasMissingTeamB) return

        teamCounter++

        val newTeam = Team(
            id = teamCounter,
            name = "Time $teamCounter",
            players = s.playerQueue.take(5)
        )

        val remainingQueue = s.playerQueue.drop(5)

        _state.update {
            it.copy(
                teamA = if (hasMissingTeamA) newTeam else it.teamA,
                teamB = if (hasMissingTeamB) newTeam else it.teamB,
                playerQueue = remainingQueue
            )
        }
    }

    fun removePlayer(player: Player) {
        _state.update { it.copy(players = it.players - player) }
    }

    fun setTimerDuration(minutes: Int) {
        if (minutes <= 0) return
        _state.update { it.copy(timerDurationMinutes = minutes, timerRemainingSeconds = minutes * 60) }
    }

    fun startGame() {
        val players = _state.value.players
        if (players.size < 10) return

        teamCounter++
        val teamA = Team(teamCounter, "Time $teamCounter", players.take(5))
        teamCounter++
        val teamB = Team(teamCounter, "Time $teamCounter", players.drop(5).take(5))
        val queue = players.drop(10)

        val duration = _state.value.timerDurationMinutes * 60
        _state.update {
            it.copy(
                currentScreen = Screen.GAME,
                teamA = teamA,
                teamB = teamB,
                playerQueue = queue,
                scoreA = 0,
                scoreB = 0,
                timerRemainingSeconds = duration,
                isTimerRunning = false,
                winnerMessage = null
            )
        }
    }

    fun backToSetup() {
        stopTimer()
        _state.update {
            AppState(
                players = it.players,
                timerDurationMinutes = it.timerDurationMinutes,
                timerRemainingSeconds = it.timerDurationMinutes * 60
            )
        }
    }

    fun addGoalA() {
        val s = _state.value
        if (s.scoreA >= 2 || s.winnerMessage != null) return
        val newScore = s.scoreA + 1
        _state.update { it.copy(scoreA = newScore) }
        if (newScore >= 2) teamBLoses()
    }

    fun addGoalB() {
        val s = _state.value
        if (s.scoreB >= 2 || s.winnerMessage != null) return
        val newScore = s.scoreB + 1
        _state.update { it.copy(scoreB = newScore) }
        if (newScore >= 2) teamALoses()
    }

    fun removeGoalA() {
        val s = _state.value
        if (s.scoreA <= 0 || s.winnerMessage != null) return
        _state.update { it.copy(scoreA = it.scoreA - 1) }
    }

    fun removeGoalB() {
        val s = _state.value
        if (s.scoreB <= 0 || s.winnerMessage != null) return
        _state.update { it.copy(scoreB = it.scoreB - 1) }
    }

    private fun teamALoses() { stopTimer(); val s = _state.value; rotate(s.teamB ?: return, s.teamA ?: return, winnerIsA = false) }
    private fun teamBLoses() { stopTimer(); val s = _state.value; rotate(s.teamA ?: return, s.teamB ?: return, winnerIsA = true) }

    /**
     * Lógica de rotação por jogador individual:
     *
     * Ex: 3 times + 2 extras (17 jogadores)
     *   Fila antes  : [J11, J12, J13, J14, J15, J16, J17]
     *   Time perde  : [J1, J2, J3, J4, J5]
     *   Fila depois : [J11, J12, J13, J14, J15, J16, J17, J1, J2, J3, J4, J5]
     *   Próx. time  : [J11, J12, J13, J14, J15]   ← primeiros 5
     *   Fila rest.  : [J16, J17, J1, J2, J3, J4, J5]
     */
    private fun rotate(winner: Team, loser: Team, winnerIsA: Boolean) {
        val s = _state.value
        val newQueue = s.playerQueue + loser.players
        val duration = s.timerDurationMinutes * 60

        val nextTeam: Team?
        val remaining: List<Player>

        if (newQueue.size >= 5) {
            teamCounter++
            nextTeam = Team(teamCounter, "Time $teamCounter", newQueue.take(5))
            remaining = newQueue.drop(5)
        } else {
            nextTeam = null
            remaining = newQueue
        }

        val msg = buildString {
            append("🏆 ${winner.name} venceu!\n\n")
            if (nextTeam != null) {
                append("Próxima Partida!\n\n${nextTeam.name}:\n")
                append(nextTeam.players.joinToString(", ") { it.name })
            } else {
                val need = 5 - newQueue.size
                append("\n\nFaltam $need jogador(es) para formar o próximo time.")
            }
        }

        _state.update {
            it.copy(
                teamA = if (winnerIsA) winner else nextTeam,
                teamB = if (winnerIsA) nextTeam else winner,
                playerQueue = remaining,
                scoreA = 0,
                scoreB = 0,
                timerRemainingSeconds = duration,
                isTimerRunning = false,
                winnerMessage = msg
            )
        }
    }

    fun dismissMessage() { _state.update { it.copy(winnerMessage = null) } }

    fun playTimer() {
        if (_state.value.isTimerRunning || _state.value.timerRemainingSeconds <= 0) return
        _state.update { it.copy(isTimerRunning = true) }
        timerJob = viewModelScope.launch {
            while (_state.value.timerRemainingSeconds > 0 && _state.value.isTimerRunning) {
                delay(1_000L)
                _state.update { it.copy(timerRemainingSeconds = it.timerRemainingSeconds - 1) }
            }
            if (_state.value.timerRemainingSeconds <= 0) _state.update { it.copy(isTimerRunning = false) }
        }
    }

    fun pauseTimer() { timerJob?.cancel(); _state.update { it.copy(isTimerRunning = false) } }

    fun resetTimer() {
        stopTimer()
        val duration = _state.value.timerDurationMinutes * 60
        _state.update { it.copy(timerRemainingSeconds = duration, isTimerRunning = false) }
    }

    private fun stopTimer() { timerJob?.cancel(); timerJob = null; _state.update { it.copy(isTimerRunning = false) } }
}