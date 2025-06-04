package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.DarkBlue
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import br.edu.puc.pi3_time1.ui.theme.White
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

class AccountActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AccountHandler(
                        onNavigateToChangePassword = {
                            startActivity(
                                Intent(
                                    this@AccountActivity,
                                    ChangePasswordActivity::class.java
                                )
                            )
                        },
                        onNavigateToMain = {
                            startActivity(Intent(this@AccountActivity, MainActivity::class.java))
                        },
                        resendEmail = user.sendEmailVerification()
                    )
                }
                }
            }
        }
    }

@Preview(showBackground = true)
@Composable
fun AccountPreview() {
    Pi3_time1Theme {
            AccountHandler(
                modifier = Modifier
                    .fillMaxSize(),
                onNavigateToChangePassword = {},
                onNavigateToMain = {},
                resendEmail = Tasks.forResult(null),
            )
        }
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountHandler(
    modifier: Modifier = Modifier,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToMain: () -> Unit,
    resendEmail: Task<Void>
) {
    val scope = rememberCoroutineScope()
    var userData by remember { mutableStateOf<UserData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var verifiedText by remember { mutableStateOf<String>("Verificando...") }
    var isVerified by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                            email = document.getString("email") ?: "N/A"
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

    LaunchedEffect(resendEmail) {
        resendEmail.addOnSuccessListener {
            println("Email de verificação reenviado com sucesso!")
        }.addOnFailureListener { e ->
            errorMessage = "Erro ao reenviar email: ${e.message}"
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF253475)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                content = {

                },
                containerColor = Color(0xFF253475)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userData != null) {
                Text(
                    text = "Conta",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    modifier = Modifier
                        .border(width = 2.dp, color = Color(0xFF253475), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Nome: ${userData!!.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .border(width = 2.dp, color = Color(0xFF253475), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Email: ${userData!!.email}",
                        fontSize = 16.sp
                    )
                    Text(
                        text = verifiedText,
                        color = if (isVerified == true) Color.Black else Color.Red,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Senha e autenticação"
                    )
                    Button(
                        onClick = {
                            when (isVerified){
                                true-> onNavigateToChangePassword()
                            false -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Por favor, verifique seu email para alterar sua senha mestre!",
                                    actionLabel = "OK",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                            null -> {}
                        }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkBlue,
                            contentColor = White,
                            disabledContainerColor = DarkBlue.copy(alpha = 0.3f),
                            disabledContentColor = White.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(text = "Alterar senha", color = Color.White)
                    }
                }
                if (isVerified == true){

                }
                else{
                Text(
                    text = "Reenviar email de verificação",
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp)
                        .clickable {
                            resendEmail
                        },
                    textAlign = TextAlign.Center,

                )}
                Button(
                    onClick = onNavigateToMain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF253475)
                    ),
                    modifier = Modifier
                        .width(150.dp)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Voltar",
                        color = Color.White,
                        fontSize = 16.sp
                    )
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
}
