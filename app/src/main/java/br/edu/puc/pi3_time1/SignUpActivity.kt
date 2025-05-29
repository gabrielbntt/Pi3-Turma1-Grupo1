package br.edu.puc.pi3_time1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

class SignUpActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                SignUp(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    activity = this@SignUpActivity
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpPreview() {
    Pi3_time1Theme {
        SignUp(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            activity = SignUpActivity()
        )
    }
}

@Composable
fun SignUp(modifier: Modifier = Modifier, activity: SignUpActivity) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmpassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var imei by remember { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        imei = getImei(context)
        if (imei != null) {
            Log.d("SignUpActivity", "Identificador obtido com sucesso: $imei")
        } else {
            Log.w("SignUpActivity", "Falha ao obter identificador")
            message = "Erro ao obter identificador do dispositivo. Tente novamente."
        }
    }

    val isNameValid = name.isNotEmpty()
    val isEmailValid = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).{8,}$"))
    val isFormValid = isNameValid && isEmailValid && isPasswordValid && imei != null && termsAccepted

    Scaffold(
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SignUp",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                modifier = Modifier.padding(10.dp),
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                isError = name.isNotEmpty() && !isNameValid,
                supportingText = {
                    if (name.isNotEmpty() && !isNameValid) {
                        Text("O nome não pode estar vazio")
                    }
                }
            )
            OutlinedTextField(
                modifier = Modifier.padding(10.dp),
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                isError = email.isNotEmpty() && !isEmailValid,
                supportingText = {
                    if (email.isNotEmpty() && !isEmailValid) {
                        Text("Email inválido")
                    }
                }
            )
            OutlinedTextField(
                modifier = Modifier.padding(10.dp),
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Esconder senha" else "Mostrar senha"
                        )
                    }
                },
                isError = password.isNotEmpty() && !isPasswordValid,
                supportingText = {
                    if (password.isNotEmpty() && !isPasswordValid) {
                        Text("A senha não se encaixa no formato pedido ")
                    } else {
                        Text("A senha deve ter no mínimo uma minúscula, uma maiúscula e um número. ")
                    }
                }
            )
            OutlinedTextField(
                modifier = Modifier.padding(10.dp),
                value = confirmpassword,
                onValueChange = { confirmpassword = it },
                label = { Text("Confirmar Senha") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Esconder senha" else "Mostrar senha"
                        )
                    }
                },
                isError = confirmpassword.isNotEmpty() && confirmpassword != password,
                supportingText = {
                    if (confirmpassword.isNotEmpty() && confirmpassword != password) {
                        Text("As senhas não são iguais")
                    }
                }
            )

            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it }
                )
                Text(
                    text = "Eu aceito os ",
                    fontSize = 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showTermsDialog = true }
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = "Termos de Uso",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Ver termos",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(20.dp)
                    )
                }
            }

            if (showTermsDialog) {
                AlertDialog(
                    onDismissRequest = { showTermsDialog = false },
                    title = { Text("Resumo dos Termos de Uso") },
                    text = {
                        Text(
                            "• O SuperID protege seus dados com criptografia.\n" +
                                    "• Não compartilhamos suas informações sem consentimento.\n" +
                                    "• Você é responsável por manter sua senha mestre segura."
                        )
                    },
                    confirmButton = {
                        Button(onClick = { showTermsDialog = false }) {
                            Text("Entendido")
                        }
                    }
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text(
                    text = "Criando sua conta...",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    if (!isFormValid) {
                        message = if (!termsAccepted) {
                            "Você deve aceitar os Termos de Uso."
                        } else {
                            "Corrija os erros nos campos ou aguarde a obtenção do identificador."
                        }
                    } else {
                        isLoading = true
                        createNewAccount(
                            activity = activity,
                            email = email,
                            password = password,
                            name = name,
                            imei = imei!!,
                            onSuccess = {
                                isLoading = false
                                message = "Conta criada! Verifique seu email."
                                name = ""
                                email = ""
                                password = ""
                                confirmpassword = ""
                                // Pass Snackbar message to MainActivity
                                val intent = Intent(activity, MainActivity::class.java).apply {
                                    putExtra("SNACKBAR_MESSAGE", "Enviamos um e-mail de verificação para $email. Abra o link no seu aplicativo de e-mail para ativar sua conta. Verifique também a pasta de spam.")
                                }
                                activity.startActivity(intent)
                            },
                            onFailure = { e ->
                                isLoading = false
                                message = "Erro ao criar conta: ${e.message}"
                                Log.e("SignUpActivity", "Erro ao criar conta", e)
                            }
                        )
                    }
                },
                enabled = isFormValid && !isLoading
            ) {
                Text(text = "Criar Minha Conta")
            }
            Button(
                onClick = {
                    activity.startActivity(Intent(activity, WelcomeActivity::class.java))
                }
            ) {
                Text("Voltar ao menu")
            }
            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

// All functions remain unchanged
fun createNewAccount(
    activity: SignUpActivity,
    email: String,
    password: String,
    name: String,
    imei: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val auth = Firebase.auth
    val obfuscatedPassword = obfuscatePassword(password, imei)

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()
                            user.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        val userId = user.uid
                                        saveAccountToFirebase(
                                            userId = userId,
                                            name = name,
                                            email = email,
                                            obfuscatedPassword = obfuscatedPassword,
                                            imei = imei,
                                            onSuccess = onSuccess,
                                            onFailure = onFailure
                                        )
                                    } else {
                                        onFailure(Exception("Falha ao atualizar o nome do usuário. Tente novamente."))
                                        Log.e("SignUpActivity", "Erro ao atualizar perfil", profileTask.exception)
                                    }
                                }
                        } else {
                            onFailure(Exception("Não foi possível enviar o e-mail de verificação. Verifique sua conexão Wi-Fi ou dados móveis."))
                            Log.e("SignUpActivity", "Erro ao enviar email de verificação", emailTask.exception)
                        }
                    }
            } else {
                val errorMessage = when {
                    task.exception?.message?.contains("email address is already in use") == true ->
                        "Este e-mail já está registrado. Tente outro ou faça login."
                    task.exception?.message?.contains("network error") == true ->
                        "Erro de conexão. Verifique sua conexão Wi-Fi ou dados móveis e tente novamente."
                    else -> "Erro ao criar conta. Tente novamente mais tarde."
                }
                onFailure(Exception(errorMessage))
                Log.e("SignUpActivity", "Erro ao criar conta no Firebase Auth", task.exception)
            }
        }
}

fun getImei(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

fun obfuscatePassword(password: String, salt: String): String {
    val shift = salt.last().code % 26
    return password.map { char ->
        if (char.isLetter()) {
            val base = if (char.isUpperCase()) 'A' else 'a'
            val shifted = ((char.code - base.code + shift) % 26 + base.code).toChar()
            shifted
        } else {
            char
        }
    }.joinToString("")
}

fun saveAccountToFirebase(
    userId: String,
    name: String,
    email: String,
    obfuscatedPassword: String,
    imei: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = Firebase.firestore
    val newAccount = hashMapOf(
        "name" to name,
        "email" to email,
        "uid" to userId,
        "password" to obfuscatedPassword,
        "imei" to imei
    )

    db.collection("Users")
        .document(userId)
        .set(newAccount)
        .addOnSuccessListener {
            Log.d("SignUpActivity", "Dados salvos no Firestore com sucesso")
            onSuccess()
            createCategory(userId, listOf("SitesWeb", "Aplicativos", "TecladosDeAcessoFísico"))
        }
        .addOnFailureListener { e ->
            val errorMessage = if (e.message?.contains("network") == true) {
                "Erro de conexão. Verifique sua conexão Wi-Fi ou dados móveis e tente novamente."
            } else {
                "Erro ao salvar os dados. Tente novamente."
            }
            Log.e("SignUpActivity", "Erro ao salvar no Firestore", e)
            onFailure(Exception(errorMessage))
        }
}

