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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
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
                    },
                    onNavigateWelcome = {
                        startActivity(Intent(this@SignUpActivity, WelcomeActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun SignUp(modifier: Modifier = Modifier,
           activity: SignUpActivity,
           onNavigateToSignIn: () -> Unit,
           onNavigateWelcome: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var confirmpassword by remember { mutableStateOf("") }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var imei by remember { mutableStateOf<String?>(null) }
    var showErrors by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

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
    val isFormChecked = termsAccepted
    val isFormValid = isNameValid && isEmailValid && isPasswordValid && isConfirmPassValid && isFormChecked && imei != null


    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.bot_o_voltar),
                contentDescription = "Botão Voltar",
                modifier = Modifier
                    .clickable{ onNavigateWelcome() }
            )
        }

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
            label = { Text(text = "Nome", color = Gray) },
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
            singleLine = true,
            supportingText = {
                if (showErrors && name.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = ErrorRed
                    )
                }
            }
        )

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
                } else if (showErrors && !(password.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@\$!%*?&^#()\\[\\]{}<>.,;:_+=|~`\\-]).{8,}\$")))) {
                    Text(
                        text = "A senha deve conter ao menos 8 caracteres.\nA senha deve ter uma letra maiúscula e uma minúscula.\nA senha deve ter um número.\nA senha deve ter um caractere especial.",
                        color = ErrorRed,
                    )
                }
            }
        )

        Text(text = "Sua senha deve conter:\n" + "• Ao menos 8 caracteres\n" + "• Uma letra maiúscula e uma minúscula\n" + "• Um número\n" + "• Um caractere especial (ex: !@#\$%)",
            fontSize = 12.sp,
            textAlign = TextAlign.Left,
            color = Gray,
            modifier = Modifier.padding(end = 20.dp))

        OutlinedTextField(
            modifier = Modifier.width(258.dp),
            value = confirmpassword,
            onValueChange = { confirmpassword = it },
            label = { Text(text = "Confirmar Senha", color = Gray) },
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
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        imageVector = if (showConfirmPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showConfirmPassword) "Esconder senha" else "Mostrar senha",
                        tint = DarkBlue
                    )
                }
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            supportingText = {
                if (showErrors && confirmpassword.isBlank()) {
                    Text(
                        text = "Preencha este campo.",
                        color = ErrorRed
                    )
                } else if (showErrors && confirmpassword != password) {
                    Text(
                        text = "As senhas não coincidem",
                        color = ErrorRed
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .height(20.dp)
                .padding(end = 25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                colors = CheckboxDefaults.colors(
                    checkedColor = DarkBlue,
                    uncheckedColor = DarkBlue,
                    checkmarkColor = White,
                ),
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it }
            )
            Text(
                text = "Eu aceito os ",
                fontSize = 14.sp
            )
            Text(
                text = "termos de uso",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DarkBlue,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { showTermsDialog = true }
            )
        }
        if (showErrors && !termsAccepted) {
            Text(
                text = "É necessário aceitar os termos.",
                color = ErrorRed,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Termos de Uso") },
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
                    isLoading = true // ativa a barra de loading do cadastro
                    createNewAccount(
                        activity = activity,
                        email = email,
                        password = password,
                        name = name,
                        imei = imei!!, // imei já foi validado em isFormValid
                        onSuccess = {
                            isLoading = false
                            message = "Conta criada! Verifique seu email."
                            name = ""
                            email = ""
                            password = ""
                            Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_LONG).show()
                            val intent = Intent(activity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "Cadastrar", fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Já tenho uma conta",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DarkBlue,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { onNavigateToSignIn() }
        )
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
            onNavigateToSignIn = { },
            onNavigateWelcome = { }
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
            createCategory(userId, listOf("SitesWeb", "Aplicativos", "TecladosDeAcessoFísico"))
        }
        .addOnFailureListener { e ->
            Log.e("SignUpActivity", "Erro ao salvar no Firestore", e)
            onFailure(e)
        }
}
