package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.DarkBlue
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import br.edu.puc.pi3_time1.ui.theme.White
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                ChangePassword(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordPreview() {
    Pi3_time1Theme {
        ChangePassword(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),

            )
    }
}

@Composable
fun ChangePassword(
    modifier: Modifier = Modifier,
    snackbarMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isEmailVerified by remember { mutableStateOf<Boolean?>(null) }
    var refreshTrigger by remember { mutableStateOf(false) }
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        isEmailVerified = user?.isEmailVerified ?: false
    }
    LaunchedEffect(snackbarMessage,refreshTrigger) {
        if (!snackbarMessage.isNullOrEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = snackbarMessage,
                    actionLabel = "Entendido",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Alterar Senha",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = email.isNotEmpty() && !isValidEmail(email),
                supportingText = {
                    if (email.isNotEmpty() && !isValidEmail(email)) {
                        Text("Email inválido")
                    }
                }
            )

            Button(
                onClick = {
                    if (email.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Digite seu Email!",
                                actionLabel = "OK",
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else if (!isValidEmail(email)) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Por favor, insira um e-mail válido!",
                                actionLabel = "OK",
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else if (isEmailVerified == false) {
                        scope.launch {
                            refreshTrigger = !refreshTrigger
                            snackbarHostState.showSnackbar(
                                message = "Por favor, verifique seu email antes de recuperar a senha.",
                                actionLabel = "OK",
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else if (isEmailVerified == true) {
                        sendEmail(auth, email) { resultMessage ->
                            scope.launch {
                                refreshTrigger = !refreshTrigger
                                snackbarHostState.showSnackbar(
                                    message = resultMessage,
                                    actionLabel = "OK",
                                    duration = SnackbarDuration.Long
                                )

                            }

                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Aguarde, verificando o status do email...",
                                actionLabel = "OK",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = White,
                    disabledContainerColor = DarkBlue.copy(alpha = 0.3f),
                    disabledContentColor = White.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "Enviar E-mail de Redefinição",
                    fontSize = 16.sp
                )
            }
        }
    }
}

fun sendEmail(auth: FirebaseAuth, email: String, onResult: (String) -> Unit) {
    auth.sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult("E-mail de redefinição enviado para $email")
            } else {
                onResult("Falha ao enviar o e-mail: ${task.exception?.message}")
            }
        }
}