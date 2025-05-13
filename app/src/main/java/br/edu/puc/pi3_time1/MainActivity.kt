package br.edu.puc.pi3_time1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

data class PasswordEntry(
    val title: String,
    val username: String?,
    val password: String,
    val description: String?
)

fun createCategory(uid: String, categoryName: String) {
    val db = Firebase.firestore
    val vazio = hashMapOf("placeholder" to true)

    db.collection("Collections")
        .document(uid)
        .collection(categoryName)
        .add(vazio)
        .addOnSuccessListener { documentReference ->
            Log.d("Firestore", "Documento criado com sucesso com ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Erro ao criar documento: ${e.message}")
        }
}

fun deleteCategory(uid: String, categoryName: String) {
    if (categoryName == "SitesWeb") {
        Log.w("Firestore", "A categoria 'SitesWeb' não pode ser excluída.")
        return
    }

    val db = Firebase.firestore

    db.collection("Collections")
        .document(uid)
        .collection(categoryName)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val batch = db.batch()
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit()
                .addOnSuccessListener {
                    Log.d("Firestore", "Categoria '$categoryName' excluída com sucesso.")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Erro ao excluir documentos da categoria '$categoryName': ${e.message}")
                }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Erro ao acessar categoria '$categoryName': ${e.message}")
        }
}

fun savePasswordEntry(uid: String, categoryName: String, title: String, username: String?, password: String, description: String?) {
    val db = Firebase.firestore
    val entry = PasswordEntry(
        title,
        username,
        password,
        description
    )

    db.collection("Collections")
        .document(uid)
        .collection(categoryName)
        .add(entry)
        .addOnSuccessListener { documentReference ->
            Log.d("Firestore", "Entrada de senha salva com ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Erro ao salvar entrada de senha: ${e.message}")
        }
}

fun searchDocumentByTitle(uid: String, categoryName: String, title: String, action: (DocumentSnapshot) -> Unit) {
    val db = Firebase.firestore

    db.collection("Collections")
        .document(uid)
        .collection(categoryName)
        .whereEqualTo("title", title)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                Log.w("Firestore", "Nenhuma senha encontrada com título: $title")
                return@addOnSuccessListener
            }

            for (document in querySnapshot.documents) {
                action(document)
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Erro ao buscar senha: ${e.message}")
        }
}


fun updatePasswordByTitle(uid: String, categoryName: String, title: String, fieldToUpdate: String, newValue: Any) {
    searchDocumentByTitle(uid, categoryName, title) { document ->
        document.reference
            .update(fieldToUpdate, newValue)
            .addOnSuccessListener {
                Log.d("Firestore", "Campo '$fieldToUpdate' atualizado com sucesso!")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao atualizar campo: ${e.message}")
            }
    }
}

fun deletePasswordByTitle(uid: String, categoryName: String, title: String) {
    searchDocumentByTitle(uid, categoryName, title) { document ->
        document.reference
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Senha '$title' excluída com sucesso!")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao excluir senha: ${e.message}")
            }
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pi3_time1Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PasswordManagerScreenPreview()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen() {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    deletePasswordByTitle("ohY8N4QWWFhQazqP2D0SX8BBD2m1", "Jogos", "Netflix")



    val samplePasswords = List(5) { index ->
        PasswordEntry(
            title = "Serviço ${index + 1}",
            username = "usuario$index@exemplo.com",
            password = "senhaSegura$index",
            description = "bla bla bla bla"
        )
    }

    val filteredPasswords = samplePasswords.filter {
        it.title.contains(searchQuery.text, ignoreCase = true) ||
                it.username?.contains(searchQuery.text, ignoreCase = true) == true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Minhas Senhas") },
                    selected = false,
                    onClick = { /* ação futura */ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Configurações") },
                    selected = false,
                    onClick = { /* ação futura */ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    onClick = { /* ação futura */ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Pesquisar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredPasswords) { password ->
                    PasswordCard(password)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun PasswordCard(password: PasswordEntry) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = password.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Usuário: ${password.username}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Senha: ******", // Mascarado por padrão
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Descrição: ${password.description}", // Mascarado por padrão
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordManagerScreenPreview() {
    PasswordManagerScreen()
}
