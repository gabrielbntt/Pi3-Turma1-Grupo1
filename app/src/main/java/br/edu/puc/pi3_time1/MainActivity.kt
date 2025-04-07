package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        // Verifica se o usuário está logado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Usuário não está logado, redireciona para SignInActivity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                    Greeting(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        activity = this@MainActivity
                    )
                }
            }
        }
    }


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Pi3_time1Theme {
        Greeting(modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
            activity = MainActivity())
    }
}
@Composable
fun Greeting(modifier: Modifier = Modifier, activity: MainActivity?) {
    var userData by remember { mutableStateOf<UserData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            val db = Firebase.firestore
            db.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData = UserData(
                            name = document.getString("name") ?: "Desconhecido",
                            email = document.getString("email") ?: "N/A",
                        )
                    } else {
                        errorMessage = "Dados do usuário não encontrados."
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Erro ao buscar dados: ${e.message}"
                }
        } else {
            errorMessage = "Usuário não está logado."
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userData != null) {
            Text(
                text = "Bem-vindo, ${userData!!.name}!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Email: ${userData!!.email}",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    Firebase.auth.signOut()
                    activity?.startActivity(Intent(activity, WelcomeActivity::class.java))
                    activity?.finish()
                }
            ) {
                Text(text = "Sair")
            }
        } else if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Text(
                text = "Carregando dados...",
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}