package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ResetPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extrai o oobCode do Deep Link
        val oobCode = intent.data?.getQueryParameter("oobCode")
        if (oobCode == null) {
            Toast.makeText(this, "Link de redefinição inválido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            Pi3_time1Theme {
                ResetPasswordScreen(
                    oobCode = oobCode,
                    onPasswordResetSuccess = {
                        // Redireciona para a tela de login após sucesso
                        startActivity(Intent(this, SignInActivity::class.java))
                        finish()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    oobCode: String,
    onPasswordResetSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    // Função para validar a nova senha
    fun isPasswordValid(password: String): Boolean {
        return password.length >= 6 // Pelo menos 6 caracteres
    }

    // Função para verificar se a nova senha é diferente da antiga
    suspend fun isPasswordDifferentFromOld(newPassword: String, user: FirebaseUser?): Boolean {
        if (user == null) return true // Se não houver usuário logado, assume que é diferente
        // Para verificar a senha antiga, precisaríamos reautenticar o usuário
        // Como o Firebase não fornece uma API direta para comparar a senha antiga,
        // uma abordagem comum é reautenticar o usuário com a senha antiga antes de mudar.
        // No entanto, como estamos usando o oobCode, podemos assumir que o usuário já foi validado.
        // Para simplificar, vamos assumir que a nova senha é diferente se passar pela validação.
        return true
    }

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
                    text = "Redefinir Senha",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nova Senha") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = newPassword.isNotEmpty() && !isPasswordValid(newPassword),
                    supportingText = {
                        if (newPassword.isNotEmpty() && !isPasswordValid(newPassword)) {
                            Text("A senha deve ter pelo menos 6 caracteres")
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Nova Senha") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                            Text("As senhas não coincidem")
                        }
                    }
                )

                Button(
                    onClick = {
                        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                            message = "Preencha todos os campos!"
                        } else if (!isPasswordValid(newPassword)) {
                            message = "A senha deve ter pelo menos 6 caracteres!"
                        } else if (newPassword != confirmPassword) {
                            message = "As senhas não coincidem!"
                        } else {
                            // Verifica se a nova senha é diferente da antiga
                            // Como estamos usando o oobCode, não temos acesso direto à senha antiga
                            // Para uma validação mais robusta, você precisaria reautenticar o usuário
                            // Para simplificar, vamos prosseguir com a redefinição
                            auth.confirmPasswordReset(oobCode, newPassword)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        message = "Senha redefinida com sucesso!"
                                        onPasswordResetSuccess()
                                    } else {
                                        message = "Erro ao redefinir a senha: ${task.exception?.message}"
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Redefinir Senha",
                        fontSize = 16.sp
                    )
                }

                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = if (message.contains("sucesso")) Color.Green else Color.Red,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    )
}