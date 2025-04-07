package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import kotlin.jvm.java
import com.google.firebase.FirebaseApp

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
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
                            startActivity(Intent(this@WelcomeActivity,SignUpActivity::class.java))
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
            onNavigateToSignUp = { /* No-op for preview */ }
        )
    }
}
@Composable
fun  WelcomeScreen( modifier: Modifier = Modifier,
                    onNavigateToSignIn: () -> Unit,
                    onNavigateToSignUp: () -> Unit) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bem-vindo ao Super ID",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Botoes(
                onNavigateToSignIn = onNavigateToSignIn,
                onNavigateToSignUp = onNavigateToSignUp,
            )
        }}
@Composable
fun Botoes(
        onNavigateToSignIn: () -> Unit,
        onNavigateToSignUp: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onNavigateToSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Entrar")
        }
        Button(
            onClick = onNavigateToSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Faça seu cadastro ")
        }
    }
}
