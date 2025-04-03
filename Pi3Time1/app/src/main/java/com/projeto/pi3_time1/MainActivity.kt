package com.puccamp.pi3_time1

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.puccamp.pi3_time1.ui.theme.Pi3time1Theme

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        enableEdgeToEdge()
        setContent {
            Pi3time1Theme {
                LoginScreen(
                    auth = auth,
                    activity = this,
                    onLoginSuccess = {
                        val user = auth.currentUser
                        updateUI(user)
                    },
                    onLoginFailure = { exception ->
                        Toast.makeText(
                            baseContext,
                            "Authentication failed: ${exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                // Email Verification sent
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        // Lógica para atualizar UI
    }

    private fun reload() {
        // Lógica para recarregar
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Pi3time1Theme {
        LoginScreen(
            auth = FirebaseAuth.getInstance(),
            activity = Activity(), // Apenas para preview
            onLoginSuccess = { /* Simula sucesso */ },
            onLoginFailure = { /* Simula falha */ }
        )
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    activity: Activity,
    onLoginSuccess: () -> Unit,
    onLoginFailure: (Exception?) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") }
        )

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(activity) { task ->
                            if (task.isSuccessful) {
                                message = "Login realizado com sucesso!"
                                onLoginSuccess()
                            } else {
                                message = "Falha no login: ${task.exception?.message}"
                                onLoginFailure(task.exception)
                            }
                        }
                } else {
                    message = "Preencha todos os campos."
                }
            }
        ) {
            Text(text = "Entrar")
        }

        if (message.isNotEmpty()) {
            Text(text = message, modifier = Modifier.padding(10.dp))
        }
    }
}
