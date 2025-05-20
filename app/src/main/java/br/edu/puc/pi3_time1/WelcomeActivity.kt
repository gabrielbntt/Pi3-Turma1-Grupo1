package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.Firebase
import kotlin.jvm.java
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class WelcomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        // Verificar se o usuário está logado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuário está logado, redirecionar para MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            return
        }
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                WelcomeScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    onNavigateToSignIn = {
                        startActivity(Intent(this@WelcomeActivity, SignInActivity::class.java))
                    },
                    onNavigateToSignUp = {
                        startActivity(Intent(this@WelcomeActivity, SignUpActivity::class.java))
                    }

                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun  WelcomeScreenPreview() {
    Pi3_time1Theme {
        WelcomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            onNavigateToSignIn = { /* No-op for preview */ },
            onNavigateToSignUp = { /* No-op for preview */ },
        )
    }
}
@Composable
fun  WelcomeScreen( modifier: Modifier = Modifier, onNavigateToSignIn: () -> Unit, onNavigateToSignUp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bem-vindo ao Super ID",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onNavigateToSignUp() },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
        ) {
            Text("Cadastrar")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onNavigateToSignIn() },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
        ) {
            Text("Logar")
        }
    }
}
