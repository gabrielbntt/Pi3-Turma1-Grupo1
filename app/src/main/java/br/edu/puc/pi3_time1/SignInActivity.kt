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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentWidth

class SignInActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                SignIn(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    activity = this@SignInActivity
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInPreview() {
    Pi3_time1Theme {
        SignIn(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            activity = SignInActivity()
        )
    }
}


@Composable
fun SignIn(modifier: Modifier = Modifier, activity: SignInActivity?) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isEmailValid = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = senha.length >= 6
    val isFormValid = isEmailValid && isPasswordValid

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
                    text = "Entrar",
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
                    isError = email.isNotEmpty(),
                    supportingText = {
                        if (email.isNotEmpty()) {
                            Text("Email inválido")
                        }
                    }
                )
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = senha.isNotEmpty() && !isPasswordValid,
                    supportingText = {
                        if (senha.isNotEmpty() && !isPasswordValid) {
                            Text("A senha deve ter pelo menos 6 caracteres")
                        }
                    }
                )

                Button(
                    onClick = {
                        if (email.isEmpty() || senha.isEmpty()) {
                            message = "Preencha todos os campos."
                        } else if (!isFormValid) {
                            message = "Corrija os erros nos campos."
                        } else {
                            isLoading = true
                            signIn(
                                activity = activity,
                                email = email,
                                password = senha,
                                onSuccess = { userData ->
                                    isLoading = false
                                    message = "Login realizado com sucesso!"
                                    val intent = Intent(activity, MainActivity::class.java).apply {
                                        putExtra("user_name", userData.name)
                                        putExtra("user_email", userData.email)
                                    }
                                    activity?.startActivity(intent)
                                    activity?.finish()
                                },
                                onFailure = { e ->
                                    isLoading = false
                                    message = when {
                                        e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Email ou senha incorretos."
                                        else -> "Erro ao fazer login: ${e.message}"
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(bottom = 16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = "Entrar")
                    }
                }

                Text(
                    text = "Ainda não tem uma conta?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Button(
                    onClick = {
                        activity?.startActivity(Intent(activity, SignUpActivity::class.java))
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                ) {
                    Text(text = "Cadastre-se")
                }
                Text(
                    text = "Esqueceu Sua senha?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "Redefinir Senha",
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp) // Ajustado
                        .clickable {
                            activity?.startActivity(Intent(activity, ChangePasswordActivity::class.java))
                        },
                    textAlign = TextAlign.Center
                )

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 16.dp),
                        fontSize = 14.sp,
                        color = if (message.contains("sucesso")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}
fun signIn(
    activity: SignInActivity?,
    email: String,
    password: String,
    onSuccess: (UserData) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val auth = Firebase.auth
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(activity ?: return) { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val db = Firebase.firestore
                    db.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val userData = UserData(
                                    name = document.getString("name") ?: "Desconhecido",
                                    email = document.getString("email") ?: "N/A",
                                )
                                onSuccess(userData)
                            } else {
                                onFailure(Exception("Dados do usuário não encontrados."))
                            }
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                } else {
                    onFailure(Exception("UID do usuário não encontrado."))
                }
            } else {
                onFailure(task.exception ?: Exception("Erro desconhecido ao fazer login"))
            }
        }
}


