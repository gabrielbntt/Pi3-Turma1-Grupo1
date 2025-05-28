package br.edu.puc.pi3_time1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import br.edu.puc.pi3_time1.ui.theme.InterFontFamily

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
                    activity = this@SignUpActivity,
                    onNavigateToSignIn = {
                        startActivity(Intent(this@SignUpActivity, SignInActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun SignUp(modifier: Modifier = Modifier,
           activity: SignUpActivity,
           onNavigateToSignIn: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmpassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var imei by remember { mutableStateOf<String?>(null) }
    var showErrors by remember { mutableStateOf(false) }

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val DarkBlue = Color(0xFF253475)
    val Gray = Color(0xFF666666)
    val LightGray = Color(0xFFDDDDDD)
    val SuccessGreen = Color(0xFF07AE33)
    val ErrorRed = Color(0xFFFF1717)

    val context = LocalContext.current

    // Obter o identificador do dispositivo quando o Composable é inicializado
    LaunchedEffect(key1 = true) {
        imei = getImei(context)
        if (imei != null) {
            Log.d("SignUpActivity", "Identificador obtido com sucesso! imei: $imei")
        } else {
            Log.w("SignUpActivity", "Falha ao obter identificador.")
        }
    }

    val isNameValid = name.isNotBlank()
    val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.isNotBlank() && password.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@\$!%*?&^#()\\[\\]{}<>.,;:_+=|~`\\-]).{8,}\$"))
    val isConfirmPassValid = confirmpassword.isNotBlank() && confirmpassword == password
    val isFormValid = isNameValid && isEmailValid && isPasswordValid && isConfirmPassValid && imei != null


    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Seja bem-vindo\nao SuperID",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(60.dp))

        OutlinedTextField(
            modifier = Modifier.width(258.dp),
            value = name,
            onValueChange = { name = it },
            label = { Text(text = "Nome") },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBlue,
                unfocusedBorderColor = DarkBlue,
                errorBorderColor = Color.Red, // cor da borda em erro
                errorLabelColor = Color.Red
            ),
            singleLine = true,
            supportingText = {
                if (showErrors && name.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = Color.Red
                    )
                }
            }
        )

        OutlinedTextField(
            modifier = Modifier.width(258.dp),
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Email") },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBlue,
                unfocusedBorderColor = DarkBlue,
                errorBorderColor = Color.Red, // cor da borda em erro
                errorLabelColor = Color.Red
            ),
            singleLine = true,
            supportingText = {
                if (showErrors && email.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = Color.Red
                    )
                } else if (showErrors && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Text(
                        text = "Email inválido.",
                        color = Color.Red
                    )
                }
            }
        )

        OutlinedTextField(
            modifier = Modifier.width(258.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Senha") },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBlue,
                unfocusedBorderColor = DarkBlue,
                errorBorderColor = Color.Red, // cor da borda em erro
                errorLabelColor = Color.Red
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
            singleLine = true,
            supportingText = {
                if (showErrors && password.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = Color.Red
                    )
                } else if (showErrors && password.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@\$!%*?&^#()\\[\\]{}<>.,;:_+=|~`\\-]).{8,}\$"))) {
                    Text(
                        text = "A senha deve conter ao menos 8 caracteres.\nA senha deve ter uma letra maiúscula e uma minúscula.\nA senha deve ter um número.\nA senha deve ter um caractere especial.",
                        color = Color.Red
                    )
                }
            }
        )

        Text(text = "Sua senha deve conter:\n" + "• Ao menos 8 caracteres\n" + "• Uma letra maiúscula e uma minúscula\n" + "• Um número\n" + "• Um caractere especial (ex: !@#\$%)",
            fontSize = 12.sp,
            textAlign = TextAlign.Left,
            color = Gray)

        OutlinedTextField(
            modifier = Modifier.width(258.dp),
            value = confirmpassword,
            onValueChange = { confirmpassword = it },
            label = { Text(text = "Confirmar Senha") },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBlue,
                unfocusedBorderColor = DarkBlue,
                errorBorderColor = Color.Red, // cor da borda em erro
                errorLabelColor = Color.Red
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
            singleLine = true,
            supportingText = {
                if (showErrors && confirmpassword.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = Color.Red
                    )
                } else if (showErrors && confirmpassword == password) {
                    Text(
                        text = "As senhas não coincidem",
                        color = Color.Red
                    )
                }
            }
        )
        Text(text = "Li e concordo com os termos de uso", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(60.dp))
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
                showErrors = true // Ativa a exibição dos erros

                if (isFormValid) {
                    createNewAccount(
                        activity = activity,
                        email = email,
                        password = password,
                        name = name,
                        imei = imei!!, // imei já foi validado em isFormValid
                        onSuccess = {
                            message = "Conta criada! Verifique seu email."
                            name = ""
                            email = ""
                            password = ""
                            Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                            activity.startActivity(Intent(activity, MainActivity::class.java))
                        },
                        onFailure = { e ->
                            message = "Erro ao criar conta: ${e.message}"
                            Log.e("SignUpActivity", "Erro ao criar conta", e)
                        }
                    )
                }
            }
        ) {
            Text(text = "Cadastrar", fontWeight = FontWeight.ExtraBold)
        }
        TextButton(onClick = onNavigateToSignIn) {
            Text(text = "Já tenho uma conta", fontWeight = FontWeight.ExtraBold, color = DarkBlue, style = TextStyle(textDecoration = TextDecoration.Underline))
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
            activity = SignUpActivity(),
            onNavigateToSignIn = { }
        )
    }
}

fun getImei(context: Context): String {
    // Retorna o ANDROID_ID como identificador do dispositivo
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

fun hashPassword(password: String): String {
    val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
    val saltedPassword = password + Base64.getEncoder().encodeToString(salt)
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(saltedPassword.toByteArray())
    return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash)
}

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
    val hashedPassword = hashPassword(password)

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
                                            hashedPassword = hashedPassword,
                                            imei = imei,
                                            onSuccess = onSuccess,
                                            onFailure = onFailure
                                        )
                                    } else {
                                        onFailure(profileTask.exception ?: Exception("Erro ao atualizar o nome"))
                                        Log.e("SignUpActivity", "Erro ao atualizar perfil", profileTask.exception)
                                    }
                                }
                        } else {
                            onFailure(emailTask.exception ?: Exception("Erro ao enviar email de verificação"))
                            Log.e("SignUpActivity", "Erro ao enviar email de verificação", emailTask.exception)
                        }
                    }
            } else {
                onFailure(task.exception ?: Exception("Erro desconhecido ao criar conta"))
                Log.e("SignUpActivity", "Erro ao criar conta no Firebase Auth", task.exception)
            }
        }
}

fun saveAccountToFirebase(
    userId: String,
    name: String,
    email: String,
    hashedPassword: String,
    imei: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = Firebase.firestore
    val newAccount = hashMapOf(
        "name" to name,
        "email" to email,
        "uid" to userId,
        "password" to hashedPassword,
        "imei" to imei
    )

    db.collection("Users")
        .document(userId)
        .set(newAccount)
        .addOnSuccessListener {
            Log.d("SignUpActivity", "Dados salvos no Firestore com sucesso")
            onSuccess()
            createCategory(userId, "SitesWeb")
            createCategory(userId, "Aplicativos")
            createCategory(userId, "TecladosDeAcessoFísico")
        }
        .addOnFailureListener { e ->
            Log.e("SignUpActivity", "Erro ao salvar no Firestore", e)
            onFailure(e)
        }
}
