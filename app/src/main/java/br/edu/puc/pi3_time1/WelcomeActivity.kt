package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class WelcomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Verificar se o usuário está logado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuário está logado, redirecionar para MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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
@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo Super ID
        Image(
            painter = painterResource(id = R.drawable.icone_superid),
            contentDescription = "Logo do Super ID",
            modifier = Modifier
                .padding(0.dp)
                .width(79.88.dp)
                .height(109.99998.dp)
        )

        // Texto "Super ID"
        Text(
            text = "Super ID",
            fontSize = 64.sp,
            fontWeight = FontWeight(900),
            textAlign = TextAlign.Center,
            color = Color(0xFF253475),
            modifier = Modifier
                .padding(16.dp)
                .width(258.dp)
                .height(77.dp)
        )

        // Texto de descrição
        Text(
            text = "Gerencie suas senhas com segurança e praticidade",
            fontSize = 20.sp,
            fontWeight = FontWeight(400),
            color = Color(0xFF000000),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(258.dp)
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botão Cadastrar
        Button(
            onClick = { onNavigateToSignUp() },
            modifier = Modifier
                .width(258.dp)
                .height(55.dp)

        ) {
            Text(
                text = "Cadastrar",
                fontSize = 16.sp,
                fontWeight = FontWeight(700),
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(79.dp)
                    .height(19.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botão Logar
        Button(
            onClick = { onNavigateToSignIn() },
            modifier = Modifier
                .width(258.dp)
                .height(55.dp)
        ) {
            Text(
                text = "Logar",
                fontSize = 16.sp,
                fontWeight = FontWeight(700),
                color = Color(0xFFFFFFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(45.dp)
                    .height(19.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
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
