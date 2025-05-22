package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

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
            finish() // Finalizar WelcomeActivity para não voltar para ela
            return
        }
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                WelcomeScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    onNavigateToSignUp = {
                        startActivity(Intent(this@WelcomeActivity, SignUpActivity::class.java))
                    },
                    onNavigateToSignIn = {
                        startActivity(Intent(this@WelcomeActivity, SignInActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSignUp: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val darkBlue = Color(0xFF253475)
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.icone_superid),
            contentDescription = "Icone SuperID",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "SuperID",
            fontSize = 68.sp,
            color = darkBlue,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gerencie suas senhas com\nsegurança e praticidade",
            fontSize = 20.sp,
            color = black,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(250.dp))
        OutlinedButton(
            onClick = onNavigateToSignUp,
            border = BorderStroke(2.dp, darkBlue),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = darkBlue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(258.dp)
                .height(50.dp)
        ) {
            Text(text = "Cadastrar", fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onNavigateToSignIn,
            colors = ButtonDefaults.buttonColors(
                containerColor = darkBlue,
                contentColor = white
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(258.dp)
                .height(50.dp)
        ) {
            Text(text = "Logar", fontWeight = FontWeight.ExtraBold)
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
            onNavigateToSignUp = { },
            onNavigateToSignIn = { }
        )
    }
}
