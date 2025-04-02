package com.puccamp.pi3_time1

import android.R
import android.app.Activity
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.puccamp.pi3_time1.ui.theme.Pi3time1Theme



class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        enableEdgeToEdge()
        setContent {
            Pi3time1Theme {
                AuthenticatorApp(
                )
            }
        }
    }
}

fun saveNewAccount(
    name: String,
    email: String,
    senha: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit) {

    val db = Firebase.firestore
    val newAccount = hashMapOf(
        "name" to name,
        "email" to email,
        "senha" to senha,
        "id" to "seu_documento_id"

    )

    val documentRef = db.collection("Accounts").document()

    // Adiciona o ID gerado ao mapa de dados
    newAccount["id"] = documentRef.id

    // Salva o documento com o ID incluído
    documentRef
        .set(newAccount)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e) }
}



@Preview(showBackground = true)
@Composable
fun AuthenticatorApp(){
    FormAccounts(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center))
}


@Composable
fun FormAccounts(modifier: Modifier = Modifier){
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }


    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally){
        Text(
            text = "Criar Conta",
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
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") }
        )

        Button(
            onClick = {
                if (name.isNotEmpty() && email.isNotEmpty() && senha.isNotEmpty()) {
                    saveNewAccount(
                        name = name,
                        email = email,
                        senha = senha,

                        onSuccess = {
                            message = "Conta Criada com sucesso!"
                            name = ""
                            email = ""
                            senha = ""
                        },
                        onFailure = { e ->
                            message = "Erro ao Criar Conta: ${e.message}"
                        }
                    )
                } else {
                    message = "Preencha todos os campos."
                }
            }
        ) {
            Text(text = "Registrar-se")
        }

        if (message.isNotEmpty()) {
            Text(text = message, modifier = Modifier.padding(10.dp))
        }
    }
}}