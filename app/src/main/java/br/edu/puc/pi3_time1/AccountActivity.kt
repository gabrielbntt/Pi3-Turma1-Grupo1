package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
class AccountActivity : ComponentActivity() {
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
                    AccountHandler(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        onNavigateToChangePassword = {
                            startActivity(Intent(this@AccountActivity, ChangePasswordActivity::class.java))
                        },
                        onNavigateToMain = {
                            startActivity(Intent(this@AccountActivity, MainActivity::class.java))
                        }
                    )
                }
            }
        }
    }

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Pi3_time1Theme {
            AccountHandler(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                onNavigateToChangePassword = {},
                onNavigateToMain = {}
            )
        }
    }


@Composable
fun AccountHandler(modifier: Modifier = Modifier, onNavigateToChangePassword: () -> Unit,onNavigateToMain:() -> Unit) {
    var userData by remember { mutableStateOf<UserData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var verifiedText by remember { mutableStateOf<String>("Verificando...") }
    var isVerified by remember { mutableStateOf<Boolean?>(null) } // Estado local para o resultado
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
            checkEmailVerification { result ->
                isVerified = result
                verifiedText = if (result) "Verificado" else "Não Verificado"
            }
        } else {
            errorMessage = "Usuário não está logado."
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        if (userData != null) {
            Text(
                text = "Conta",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,

            )
            Text(
                text ="Nome: ${ userData!!.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,

            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Alinha "Verificado" à direita
            ) {
                Text(
                    text = "Email: ${userData!!.email}",
                    fontSize = 16.sp
                )
                Text(
                    text = verifiedText,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quer trocar sua senha?"
                )
                Button(
                    onClick = { onNavigateToChangePassword() }
                ) {
                    Text(text = "Alterar Senha")
                }
            }
            Spacer(modifier = Modifier.padding(bottom = 32.dp))
                Button(onClick = { onNavigateToMain() }) {
                    Text(text = "Retornar")

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
