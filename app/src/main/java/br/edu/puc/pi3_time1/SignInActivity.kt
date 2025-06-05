package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import br.edu.puc.pi3_time1.ui.theme.InterFontFamily
import kotlin.math.log

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
                    activity = this@SignInActivity,
                    onNavigateToSignUp = {
                        startActivity(Intent(this@SignInActivity, SignUpActivity::class.java))
                    },
                    onNavigateWelcome = {
                        startActivity(Intent(this@SignInActivity, WelcomeActivity::class.java))
                    },
                    onNavigateChangePass = {
                        startActivity(Intent(this@SignInActivity, ChangePasswordActivity::class.java))
                    },
                )
            }
        }
    }
}



@Composable
fun SignIn(modifier: Modifier = Modifier,
           activity: SignInActivity,
           onNavigateToSignUp: () -> Unit,
           onNavigateWelcome: () -> Unit,
           onNavigateChangePass: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val DarkBlue = Color(0xFF253475)
    val Gray = Color(0xFF666666)
    val LightGray = Color(0xFFDDDDDD)
    val SuccessGreen = Color(0xFF07AE33)
    val ErrorRed = Color(0xFFFF1717)

    val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.isNotBlank()
    val isFormValid = isEmailValid && isPasswordValid


    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                //botao para voltar a tela de inicio
                painter = painterResource(id = R.drawable.bot_o_voltar),
                contentDescription = "Botão Voltar",
                modifier = Modifier
                    .clickable{ onNavigateWelcome() }
            )
        }

        Text(
            text = "Bem-vindo de\nvolta ao SuperID",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(170.dp))

        OutlinedTextField(
            modifier = Modifier.width(258.dp),
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Email", color = Gray) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                cursorColor = Black,
                focusedBorderColor = DarkBlue,
                unfocusedBorderColor = DarkBlue,
                errorBorderColor = ErrorRed,
                errorLabelColor = ErrorRed
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            supportingText = {
                if (showErrors && email.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = ErrorRed
                    )
                } else if (showErrors && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Text(
                        text = "Email inválido.",
                        color = ErrorRed
                    )
                }
            }
        )

        OutlinedTextField(
            modifier = Modifier.width(258.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Senha", color = Gray) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                cursorColor = Black,
                focusedBorderColor = DarkBlue,
                unfocusedBorderColor = DarkBlue,
                errorBorderColor = ErrorRed,
                errorLabelColor = ErrorRed
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showPassword) "Esconder senha" else "Mostrar senha",
                        tint = DarkBlue
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            supportingText = {
                if (showErrors && password.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = ErrorRed
                    )
                }
            }
        )

        Text(
            //redireciona para a activity de recuperar senha
            text = "Esqueci minha senha",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DarkBlue,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { onNavigateChangePass() }
        )

        Spacer(modifier = Modifier.height(170.dp))

        Button(
            modifier = Modifier
                .width(258.dp)
                .height(55.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkBlue,
                contentColor = White
            ),
            onClick = {
                // botao de fazer login
                showErrors = true

                if (isFormValid) {
                    isLoading = true
                    signIn(
                        activity = activity,
                        email = email,
                        password = password,
                        onSuccess = { userData ->
                            isLoading = false
                            message = "Login realizado com sucesso!"
                            Toast.makeText(context, "Logado com sucesso!", Toast.LENGTH_LONG).show()
                            val intent = Intent(activity, MainActivity::class.java).apply {
                                putExtra("user_name", userData.name)
                                putExtra("user_email", userData.email)
                            }
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            activity.startActivity(intent)
                        },
                        onFailure = { e ->
                            isLoading = false
                            loginError = "Erro: ${e.message}"
                            message = when {
                                e.message?.contains("The supplied auth credential is incorrect, malformed or has expired.") == true -> "Email ou senha incorretos"
                                else -> "Erro ao fazer login: ${e.message}"
                            }
                            Log.v("Log Teste", message);
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Logar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            // texto clicavel que redireciona para a tela de cadastro
            text = "Não tem uma conta?",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DarkBlue,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { onNavigateToSignUp() }
        )
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
            activity = SignInActivity(),
            onNavigateToSignUp = { },
            onNavigateWelcome = { },
            onNavigateChangePass = { }
        )
    }
}



fun signIn(
    //funcao de conferir os dados do usuario no banco de dados
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
                                onFailure(Exception("Email ou senha incorretos"))
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