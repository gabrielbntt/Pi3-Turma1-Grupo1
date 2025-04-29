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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                ChangePassword(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    activity = this@ChangePasswordActivity
                )
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    Pi3_time1Theme {
        ChangePassword(
            modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
            activity = ChangePasswordActivity())
    }
}
@Composable
fun ChangePassword(modifier: Modifier = Modifier, activity: ChangePasswordActivity?) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    // Função para validar o e-mail
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    Scaffold(
        modifier = modifier,
        content = { padding ->
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
                            message = "Digite seu Email!"
                        } else if (!isValidEmail(email)) {
                            message = "Por favor, insira um e-mail válido!"
                        } else {
                            sendEmail(auth, email, activity) { resultMessage ->
                                message = resultMessage
                            }
                        }
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Enviar E-mail de Redefinição",
                        fontSize = 16.sp
                    )
                }

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = if (message.contains("enviado")) Color.Green else Color.Red,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    )
}

// Função para enviar o e-mail de redefinição de senha
fun sendEmail(auth: FirebaseAuth, email: String, activity: ChangePasswordActivity?, onResult: (String) -> Unit) {
    auth.sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult("E-mail de redefinição enviado para $email")
                // Redireciona para a tela de login
                activity?.startActivity(Intent(activity, SignInActivity::class.java))
                activity?.finish() // Fecha a ChangePasswordActivity
            } else {
                onResult("Falha ao enviar o e-mail: ${task.exception?.message}")
            }
        }
}

