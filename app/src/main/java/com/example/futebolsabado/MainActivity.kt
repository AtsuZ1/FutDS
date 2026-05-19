package com.example.futebolsabado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Colors
// ─────────────────────────────────────────────

private val GreenDark   = Color(0xFF1B5E20)
private val GreenMedium = Color(0xFF2E7D32)
private val GreenLight  = Color(0xFF4CAF50)
private val GreenAccent = Color(0xFF69F0AE)
private val YellowGoal  = Color(0xFFFFC107)
private val RedTimer    = Color(0xFFD32F2F)
private val BlackBg     = Color(0xFF0D0D0D)
private val CardBg      = Color(0xFF1C1C1C)
private val CardBg2     = Color(0xFF252525)
private val NextTeamBg  = Color(0xFF1A3A1A)

// ─────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary      = GreenLight,
                    secondary    = GreenAccent,
                    background   = BlackBg,
                    surface      = CardBg,
                    onPrimary    = Color.White,
                    onBackground = Color.White,
                    onSurface    = Color.White
                )
            ) {
                val state by vm.state.collectAsState()
                Surface(modifier = Modifier.fillMaxSize(), color = BlackBg) {
                    when (state.currentScreen) {
                        Screen.SETUP -> SetupScreen(state, vm)
                        Screen.GAME  -> GameScreen(state, vm)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// SETUP SCREEN
// ═══════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(state: AppState, vm: GameViewModel) {
    var playerName by remember { mutableStateOf("") }
    var timerInput by remember { mutableStateOf(state.timerDurationMinutes.toString()) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        if (playerName.isNotBlank()) {
            vm.addPlayer(playerName.trim())
            playerName = ""
            keyboard?.hide()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚽", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Gerenciar Partida", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        },
        containerColor = BlackBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Timer duration
            item {
                SectionCard {
                    Label("⏱️  Duração da Partida")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = timerInput,
                            onValueChange = { v ->
                                timerInput = v
                                v.toIntOrNull()?.takeIf { it > 0 }?.let { vm.setTimerDuration(it) }
                            },
                            label = { Text("", color = Color.Gray) },
                            modifier = Modifier.width(110.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            colors = fieldColors(),
                            singleLine = true
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Minutos por partida", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Add player
            item {
                SectionCard {
                    Label("👤  Adicionar Jogador")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = { playerName = it },
                            label = { Text("Nome do jogador", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            colors = fieldColors(),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        FloatingActionButton(onClick = { submit() }, containerColor = GreenLight, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                        }
                    }
                }
            }

            // Header
            item {
                val teams    = state.players.size / 5
                val leftover = state.players.size % 5
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🧑‍🤝‍🧑  Jogadores (${state.players.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text("$teams time(s)" + if (leftover > 0) " + $leftover" else "", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Player list
            itemsIndexed(state.players) { index, player ->
                if (index % 5 == 0) {
                    Text("─── Time ${(index / 5) + 1} ───", color = GreenAccent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 4.dp))
                }
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg2), shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(GreenMedium), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(player.name, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { vm.removePlayer(player) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Start button
            item {
                val canStart = state.players.size >= 10
                Button(
                    onClick = { vm.startGame() },
                    enabled = canStart,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenLight, disabledContainerColor = Color.Gray.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (canStart) "⚽  Iniciar Jogo!" else "Adicione pelo menos 10 jogadores",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            item {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "© 2026 Gustavo G. Dluzniewski. Todos os direitos reservados.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════
// GAME SCREEN
// ═══════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(state: AppState, vm: GameViewModel) {

    if (state.winnerMessage != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissMessage() },
            title = { Text("Fim de Partida", fontWeight = FontWeight.Bold, color = GreenAccent) },
            text = { Text(state.winnerMessage, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center, color = Color.White, lineHeight = 26.sp) },
            confirmButton = {
                Button(onClick = { vm.dismissMessage() }, colors = ButtonDefaults.buttonColors(containerColor = GreenLight)) {
                    Text("Próxima Partida  ▶", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardBg
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚽", fontSize = 20.sp); Spacer(Modifier.width(8.dp))
                        Text("Partida em Andamento", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.backToSetup() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenDark)
            )
        },
        containerColor = BlackBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item { TimerCard(state, vm) }
            item { ScoreboardCard(state, vm) }

            item {
                AddPlayerDuringGameCard(vm)
            }

            if (state.playerQueue.isNotEmpty()) {
                item { QueueCard(state.playerQueue) }
            }
            item { Spacer(Modifier.height(16.dp)) }

            item {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "© 2026 Gustavo G. Dluzniewski. Todos os direitos reservados.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// TIMER CARD
// ─────────────────────────────────────────────

@Composable

fun AddPlayerDuringGameCard(vm: GameViewModel) {
    var playerName by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        if (playerName.isNotBlank()) {
            vm.addPlayer(playerName.trim())
            playerName = ""
            keyboard?.hide()
        }
    }

    SectionCard {
        Label("👤  Adicionar jogador:")
        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Nome do jogador", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = fieldColors(),
                singleLine = true
            )

            Spacer(Modifier.width(8.dp))

            FloatingActionButton(
                onClick = { submit() },
                containerColor = GreenLight,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Adicionar jogador",
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "O jogador será colocado no fim da fila de espera.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TimerCard(state: AppState, vm: GameViewModel) {
    val mins     = state.timerRemainingSeconds / 60
    val secs     = state.timerRemainingSeconds % 60
    val finished = state.timerRemainingSeconds <= 0
    val urgent   = state.timerRemainingSeconds in 1..60

    val timeColor = when {
        finished             -> Color.Gray
        urgent               -> RedTimer
        state.isTimerRunning -> GreenAccent
        else                 -> Color.White
    }
    SectionCard {
        Label("⏱️  CRONÔMETRO")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "%02d:%02d".format(mins, secs),
            fontSize = 72.sp, fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace, color = timeColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        if (finished) Text("⏰  Tempo esgotado!", color = RedTimer, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.resetTimer() }, border = BorderStroke(1.dp, Color.Gray), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)) {
                Icon(Icons.Default.Refresh, contentDescription = "Zerar", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp)); Text("Zerar")
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { if (state.isTimerRunning) vm.pauseTimer() else vm.playTimer() },
                enabled = !finished,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.isTimerRunning) YellowGoal else GreenLight, disabledContainerColor = Color.Gray.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (state.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(if (state.isTimerRunning) "Pausar" else "  Play  ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────
// SCOREBOARD CARD
// ─────────────────────────────────────────────

@Composable
fun ScoreboardCard(state: AppState, vm: GameViewModel) {
    val teamA = state.teamA ?: return
    val teamB = state.teamB ?: return

    SectionCard {
        Label("🥅  PLACAR")
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TeamScore(teamA, state.scoreA, state.scoreA >= 2, { vm.addGoalA() }, { vm.removeGoalA() })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✕", fontSize = 28.sp, color = Color.Gray, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(4.dp))
                Text("2 gols\nvence", style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 16.sp)
            }
            TeamScore(teamB, state.scoreB, state.scoreB >= 2, { vm.addGoalB() }, { vm.removeGoalB() })
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.25f))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            PlayersList(teamA, Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(100.dp).background(Color.Gray.copy(alpha = 0.25f)).align(Alignment.CenterVertically))
            PlayersList(teamB, Modifier.weight(1f))
        }
    }
}

@Composable
fun TeamScore(team: Team, score: Int, isWinner: Boolean, onAddGoal: () -> Unit, onUndoGoal: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isWinner) Text("🏆", fontSize = 22.sp)
        Text(team.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (isWinner) YellowGoal else Color.White)
        Box(modifier = Modifier.size(78.dp).clip(CircleShape).background(if (isWinner) YellowGoal.copy(alpha = 0.18f) else GreenMedium.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
            Text("$score", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = if (isWinner) YellowGoal else GreenAccent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { i -> Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (i < score) GreenAccent else Color.Gray.copy(alpha = 0.3f))) }
        }
        Button(onClick = onAddGoal, enabled = score < 2, modifier = Modifier.height(38.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenLight, disabledContainerColor = Color.Gray.copy(alpha = 0.2f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Gol", modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(4.dp)); Text("Gol", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }
        TextButton(onClick = onUndoGoal, enabled = score > 0, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
            Text("↩  Desfazer", fontSize = 11.sp, color = if (score > 0) Color.Gray else Color.Transparent)
        }
    }
}

@Composable
fun PlayersList(team: Team, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(team.name, style = MaterialTheme.typography.labelMedium, color = GreenAccent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        team.players.forEach { p ->
            Text("• ${p.name}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────
// QUEUE CARD — fila individual de jogadores
// ─────────────────────────────────────────────

@Composable
fun QueueCard(playerQueue: List<Player>) {
    val nextTeamPlayers = playerQueue.take(5)
    val waitingPlayers  = playerQueue.drop(5)
    val nextIsFull      = nextTeamPlayers.size == 5

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("🕐  FILA DE ESPERA")
            Spacer(Modifier.width(8.dp))
            Text("(${playerQueue.size} jogador${if (playerQueue.size != 1) "es" else ""})", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(14.dp))

        // ── Próximo time ────────────────────────
        Text(
            text  = if (nextIsFull) "▶  Próximo time (completo):" else "⏳  Formando próximo time (${nextTeamPlayers.size}/5):",
            style = MaterialTheme.typography.labelMedium,
            color = if (nextIsFull) GreenAccent else YellowGoal,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = if (nextIsFull) NextTeamBg else CardBg2),
            shape    = RoundedCornerShape(10.dp),
            border   = BorderStroke(1.dp, if (nextIsFull) GreenAccent.copy(alpha = 0.5f) else YellowGoal.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                nextTeamPlayers.forEachIndexed { i, player ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(if (nextIsFull) GreenAccent.copy(alpha = 0.25f) else YellowGoal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i + 1}", color = if (nextIsFull) GreenAccent else YellowGoal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(player.name.take(7) + if (player.name.length > 7) "." else "", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
                // Slots vazios
                repeat(5 - nextTeamPlayers.size) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // ── Aguardando ──────────────────────────
        if (waitingPlayers.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("⌛  Aguardando (${waitingPlayers.size}):", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            waitingPlayers.chunked(5).forEachIndexed { teamIdx, chunk ->
                val label = "Time ${teamIdx + 2} (formando)"
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chunk.forEach { player ->
                        Text(
                            "• ${player.name}",
                            style  = MaterialTheme.typography.bodySmall,
                            color  = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun Label(text: String) {
    Text(text, color = GreenAccent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, style = MaterialTheme.typography.labelLarge)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = GreenLight,
    unfocusedBorderColor = Color.Gray,
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    cursorColor          = GreenAccent
)