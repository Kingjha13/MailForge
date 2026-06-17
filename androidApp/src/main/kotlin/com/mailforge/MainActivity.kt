package com.mailforge

import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


private val BgDeep = Color(0xFF0B0F0E)
private val BgPanel = Color(0xFF111816)
private val BgField = Color(0xFF182320)
private val LineMuted = Color(0xFF24302C)
private val TextPrimary = Color(0xFFE7F2EC)
private val TextMuted = Color(0xFF7C9389)
private val AccentSignal = Color(0xFF4ADE80)
private val AccentPending = Color(0xFFE8B339)
private val AccentError = Color(0xFFEF5350)
private val Mono = FontFamily.Monospace

enum class StepState { PENDING, ACTIVE, DONE, FAILED }

data class Step(val label: String, var state: StepState = StepState.PENDING)

data class LogLine(val tag: String, val text: String, val isError: Boolean = false)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MailForgeTestScreen(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailForgeTestScreen(activity: ComponentActivity) {
    var email by remember { mutableStateOf("") }
    var appPassword by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }

    var isSending by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<LogLine>()) }
    val steps = remember {
        mutableStateListOf(
            Step("Connect"),
            Step("STARTTLS"),
            Step("Authenticate"),
            Step("Send message"),
        )
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun resetSteps() {
        for (i in steps.indices) steps[i] = steps[i].copy(state = StepState.PENDING)
    }

    fun appendLog(tag: String, text: String, isError: Boolean = false) {
        logs = logs + LogLine(tag, text, isError)
        scope.launch { listState.animateScrollToItem((logs.size - 1).coerceAtLeast(0)) }
    }

    fun setStep(index: Int, state: StepState) {
        steps[index] = steps[index].copy(state = state)
    }

    val canSend = email.isNotBlank() && appPassword.isNotBlank() && recipient.isNotBlank() && !isSending

    Surface(modifier = Modifier.fillMaxSize(), color = BgDeep) {
        Column(modifier = Modifier.fillMaxSize()) {


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSending) AccentPending else AccentSignal)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "MAILFORGE",
                        color = TextPrimary,
                        fontFamily = Mono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Raw SMTP test console",
                    color = TextMuted,
                    fontFamily = Mono,
                    fontSize = 12.sp,
                )
            }

            HorizontalDivider(color = LineMuted, thickness = 1.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Panel {
                    FieldLabel("Gmail address")
                    TermField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "you@gmail.com",
                        keyboardType = KeyboardType.Email,
                    )
                    Spacer(Modifier.height(12.dp))
                    FieldLabel("App password")
                    TermField(
                        value = appPassword,
                        onValueChange = { appPassword = it },
                        placeholder = "16-character app password",
                        visualTransformation = PasswordVisualTransformation('•'),
                        keyboardType = KeyboardType.Password,
                    )
                    Spacer(Modifier.height(12.dp))
                    FieldLabel("Send to")
                    TermField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        placeholder = "recipient@example.com",
                        keyboardType = KeyboardType.Email,
                    )
                }

                Panel {
                    Text(
                        "HANDSHAKE",
                        color = TextMuted,
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    steps.forEachIndexed { index, step ->
                        StepRow(step)
                        if (index != steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 9.dp)
                                    .width(1.dp)
                                    .height(14.dp)
                                    .background(LineMuted)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgPanel)
                        .padding(14.dp)
                ) {
                    Text(
                        "LOG",
                        color = TextMuted,
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (logs.isEmpty()) {
                        Text(
                            "Nothing sent yet. Fill in the fields above and tap Send.",
                            color = TextMuted,
                            fontFamily = Mono,
                            fontSize = 12.sp,
                        )
                    } else {
                        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(logs) { line ->
                                Text(
                                    "${line.tag}  ${line.text}",
                                    color = if (line.isError) AccentError else TextPrimary,
                                    fontFamily = Mono,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = {
                        isSending = true
                        logs = emptyList()
                        resetSteps()
                        activity.lifecycleScope.launch {
                            runSmtpTest(
                                email = email,
                                appPassword = appPassword,
                                recipient = recipient,
                                onLog = ::appendLog,
                                onStep = ::setStep,
                            )
                            isSending = false
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSignal,
                        contentColor = Color(0xFF06120D),
                        disabledContainerColor = LineMuted,
                        disabledContentColor = TextMuted,
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF06120D),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("SENDING…", fontFamily = Mono, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    } else {
                        Text("SEND TEST EMAIL", fontFamily = Mono, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgPanel)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextMuted,
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun TermField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted, fontFamily = Mono, fontSize = 13.sp) },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = TextPrimary,
            fontFamily = Mono,
            fontSize = 13.sp,
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = BgField,
            unfocusedContainerColor = BgField,
            focusedBorderColor = AccentSignal,
            unfocusedBorderColor = LineMuted,
            cursorColor = AccentSignal,
        ),
    )
}

@Composable
private fun StepRow(step: Step) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    when (step.state) {
                        StepState.DONE -> AccentSignal
                        StepState.FAILED -> AccentError
                        StepState.ACTIVE -> Color.Transparent
                        StepState.PENDING -> LineMuted
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (step.state) {
                StepState.DONE -> Text("✓", color = Color(0xFF06120D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                StepState.FAILED -> Text("✕", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                StepState.ACTIVE -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = AccentPending,
                    strokeWidth = 2.dp,
                )
                StepState.PENDING -> {}
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            step.label,
            color = when (step.state) {
                StepState.DONE -> TextPrimary
                StepState.ACTIVE -> AccentPending
                StepState.FAILED -> AccentError
                StepState.PENDING -> TextMuted
            },
            fontFamily = Mono,
            fontSize = 13.sp,
        )
    }
}


private suspend fun runSmtpTest(
    email: String,
    appPassword: String,
    recipient: String,
    onLog: (String, String, Boolean) -> Unit,
    onStep: (Int, StepState) -> Unit,
) {
    try {
        val socket = createSmtpSocket()

        onStep(0, StepState.ACTIVE)
        socket.connect(host = "smtp.gmail.com", port = 587, useTls = false)
        onLog("BANNER", socket.readLine() ?: "", false)

        socket.writeLine("EHLO localhost")
        while (true) {
            val line = socket.readLine() ?: break
            onLog("EHLO", line, false)
            if (line.length > 3 && line[3] == ' ') break
        }
        onStep(0, StepState.DONE)


        onStep(1, StepState.ACTIVE)
        socket.writeLine("STARTTLS")
        onLog("STARTTLS", socket.readLine() ?: "", false)
        socket.startTls()

        socket.writeLine("EHLO localhost")
        while (true) {
            val line = socket.readLine() ?: break
            onLog("EHLO", line, false)
            if (line.length > 3 && line[3] == ' ') break
        }
        onStep(1, StepState.DONE)

        onStep(2, StepState.ACTIVE)
        socket.writeLine("AUTH LOGIN")
        onLog("AUTH", socket.readLine() ?: "", false)

        socket.writeLine(Base64.encodeToString(email.toByteArray(), Base64.NO_WRAP))
        onLog("USER", socket.readLine() ?: "", false)

        socket.writeLine(Base64.encodeToString(appPassword.toByteArray(), Base64.NO_WRAP))
        val authResult = socket.readLine()
        onLog("PASS", authResult ?: "", authResult?.startsWith("235") != true)

        if (authResult?.startsWith("235") != true) {
            onStep(2, StepState.FAILED)
            onLog("ERROR", "Authentication failed — check address and app password", true)
            socket.close()
            return
        }
        onStep(2, StepState.DONE)

        onStep(3, StepState.ACTIVE)
        socket.writeLine("MAIL FROM:<$email>")
        onLog("MAIL FROM", socket.readLine() ?: "", false)

        socket.writeLine("RCPT TO:<$recipient>")
        onLog("RCPT TO", socket.readLine() ?: "", false)

        socket.writeLine("DATA")
        onLog("DATA", socket.readLine() ?: "", false)

        val message = buildString {
            append("From: $email\r\n")
            append("To: $recipient\r\n")
            append("Subject: MailForge test\r\n")
            append("\r\n")
            append("Hello from my own KMP SMTP library!\r\n")
        }
        socket.writeLine(message)
        socket.writeLine(".")
        onLog("SEND", socket.readLine() ?: "", false)

        socket.writeLine("QUIT")
        socket.close()
        onStep(3, StepState.DONE)
        onLog("DONE", "Check your inbox", false)

    } catch (e: Exception) {
        onLog("ERROR", e.message ?: e.toString(), true)
    }
}