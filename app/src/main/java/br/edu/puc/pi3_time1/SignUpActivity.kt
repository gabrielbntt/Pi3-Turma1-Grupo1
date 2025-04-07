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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.WelcomeActivity
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
                Pi3_time1Theme{
                SignUp(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        activity = this@SignUpActivity // Passe a Activity para o Composable
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
            activity = SignUpActivity() // Apenas para preview, não funcional
        )
    }
}
@Composable
    fun SignUp(modifier: Modifier = Modifier, activity: SignUpActivity){
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rg by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SignUp",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") }
        )
        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") }
        )
        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            value = rg,
            onValueChange = { rg = it },
            label = { Text("RG") }
        )
        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            value = cpf,
            onValueChange = { cpf = it },
            label = { Text("CPF") }
        )
        Button(
            onClick = {
                if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && rg.isNotEmpty() && cpf.isNotEmpty()) {
                    createNewAccount(
                        activity = activity,
                        email = email,
                        password = password,
                        name = name,
                        rg = rg,
                        cpf = cpf,
                        onSuccess = {
                            message = "Conta criada com sucesso!"
                            name = ""
                            email = ""
                            password = ""
                            rg = ""
                            cpf = ""
                            activity.startActivity(Intent(activity, MainActivity::class.java))
                        },
                        onFailure = { e ->
                            message = "Erro ao criar conta: ${e.message}"
                        }
                    )
                } else {
                    message = "Preencha todos os campos."
                }
            }
        ) {
            Text(text = "Criar Minha Conta")
        }
        Button(
            onClick = {
                activity.startActivity(Intent(activity, WelcomeActivity::class.java))
            }){
            Text("Voltar ao menu")
        }
    } }
    fun createNewAccount(
        activity: SignUpActivity,
        email: String,
        password: String,
        name: String,
        rg: String,
        cpf: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val auth = Firebase.auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveAccountToFirebase(
                            userId = userId,
                            name = name,
                            email = email,
                            rg = rg,
                            cpf = cpf,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    } else {
                        onFailure(Exception("UID do usuário não encontrado"))
                    }
                } else {
                    onFailure(task.exception ?: Exception("Erro desconhecido ao criar conta"))
                }
            }
    }
    fun saveAccountToFirebase(
        userId: String,
        name: String,
        email: String,

        rg: String,
        cpf: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = Firebase.firestore
        val newAccount = hashMapOf(
            "name" to name,
            "email" to email,
            "rg" to rg,
            "cpf" to cpf,
            "uid" to userId
        )

        db.collection("Users")
            .document(userId) // Usa o UID como ID do documento
            .set(newAccount)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }