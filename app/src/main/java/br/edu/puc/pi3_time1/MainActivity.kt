package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class PasswordEntry(
    val title: String,
    val username: String?,
    val password: String,
    val description: String?
)

data class Category(
    val name: String,
    val services: List<PasswordEntry>
)

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
    val obfuscatedPassword = obfuscatePassword(password, uid) // Usando obfuscatePassword
    val entry = PasswordEntry(
        title = title,
        username = username,
        password = obfuscatedPassword,
        description = description
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
        val valueToUpdate = if (fieldToUpdate == "password" && newValue is String) {
            obfuscatePassword(newValue, uid) // Usando obfuscatePassword
        } else {
            newValue
        }

        document.reference
            .update(fieldToUpdate, valueToUpdate)
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
/*suspend fun fetchCategories(uid: String): List<Category> = withContext(Dispatchers.IO) {
    val db = Firebase.firestore
    val categories = mutableListOf<Category>()

    try {
        // Verifica se o documento do usuário existe
        val userDocRef = db.collection("Collections").document(uid)
        val userDoc = userDocRef.get().await()
        if (!userDoc.exists()) {
            Log.w("fetchCategories", "Documento de usuário $uid não encontrado")
            return@withContext emptyList()
        }

        // Lista todas as subcoleções (categorias)
        val collections = userDocRef.listCollections().await()
        if (collections.isEmpty()) {
            Log.d("fetchCategories", "Nenhuma categoria encontrada para o usuário $uid")
            return@withContext emptyList()
        }

        // Log das subcoleções encontradas
        val collectionIds = collections.map { it.id }
        Log.d("fetchCategories", "Subcoleções encontradas para $uid: $collectionIds")

        // Busca serviços para cada categoria
        coroutineScope {
            val categoryTasks = collections.map { collectionRef ->
                async {
                    val categoryName = collectionRef.id
                    Log.d("fetchCategories", "Processando categoria: $categoryName")
                    val services = fetchServicesForCategory(uid, categoryName)
                    Log.d("fetchCategories", "Serviços carregados para $categoryName: ${services.map { it.title }}")
                    Category(categoryName, services)
                }
            }
            categories.addAll(categoryTasks.awaitAll())
        }

        Log.d("fetchCategories", "Categorias carregadas: ${categories.map { it.name }}")
        return@withContext categories.sortedBy { it.name }
    } catch (e: Exception) {
        Log.e("fetchCategories", "Erro ao buscar categorias para $uid: ${e.message}", e)
        emptyList()
    }
}

suspend fun fetchServicesForCategory(uid: String, categoryName: String): List<PasswordEntry> = withContext(Dispatchers.IO) {
    val db = Firebase.firestore

    try {
        val snapshot = db.collection("Collections")
            .document(uid)
            .collection(categoryName)
            .get()
            .await()

        val services = snapshot.documents
            .filter { doc ->
                val hasRequiredFields = doc.contains("title") && doc.contains("password")
                if (!hasRequiredFields) {
                    Log.w("fetchServicesForCategory", "Documento em $categoryName ignorado: falta title ou password")
                }
                hasRequiredFields
            }
            .mapNotNull { doc ->
                try {
                    PasswordEntry(
                        title = doc.getString("title") ?: return@mapNotNull null,
                        username = doc.getString("username"),
                        password = doc.getString("password") ?: return@mapNotNull null,
                        description = doc.getString("description")
                    )
                } catch (e: Exception) {
                    Log.e("fetchServicesForCategory", "Erro ao parsear documento em $categoryName: ${e.message}", e)
                    null
                }
            }
        Log.d("fetchServicesForCategory", "Serviços carregados para $categoryName: ${services.map { it.title }}")
        services
    } catch (e: Exception) {
        Log.e("fetchServicesForCategory", "Erro ao buscar serviços da categoria $categoryName: ${e.message}", e)
        emptyList()
    }
}*/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pi3_time1Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PasswordManagerScreen(
                        onNavigateToCategories ={
                            startActivity(Intent(this@MainActivity, CategoriesActivity::class.java))
                        },
                        onLogout = {
                            Firebase.auth.signOut()
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    WelcomeActivity::class.java
                                ).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                            finish()

                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(onLogout: () -> Unit,onNavigateToCategories : () -> Unit) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showChooseActionDialog by remember { mutableStateOf(false) }
    var showAddPasswordDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "default_uid"
    var categories by remember { mutableStateOf(listOf<Category>()) }
    // Lista estática de categorias para simulação
    val sampleCategories = listOf(
        Category(
            name = "SitesWeb",
            services = List(3) { index ->
                PasswordEntry(
                    title = "Site ${index + 1}",
                    username = "usuario$index@site.com",
                    password = "senhaSegura$index",
                    description = "Descrição do site $index"
                )
            }
        ),
        Category(
            name = "Aplicativos",
            services = List(2) { index ->
                PasswordEntry(
                    title = "App ${index + 1}",
                    username = "usuario$index@app.com",
                    password = "senhaApp$index",
                    description = "Descrição do app $index"
                )
            }
        ),
        Category(
            name = "TecladosDeAcessoFisico",
            services = List(1) { index ->
                PasswordEntry(
                    title = "Teclado ${index + 1}",
                    username = null,
                    password = "senhaTeclado$index",
                    description = "Descrição do teclado $index"
                )
            }
        )
    )

    val filteredCategories = sampleCategories.filter { category ->
        category.name.contains(searchQuery.text, ignoreCase = true) ||
                category.services.any {
                    it.title.contains(searchQuery.text, ignoreCase = true) ||
                            it.username?.contains(searchQuery.text, ignoreCase = true) == true
                }
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
                    label = { Text("Categorias") },
                    selected = false,
                    onClick = { onNavigateToCategories() },
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
                    onClick = {
                        onLogout()
                    },
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showChooseActionDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar"
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (filteredCategories.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhuma categoria encontrada",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(filteredCategories) { category ->
                        Log.d("PasswordManagerScreen", "Renderizando categoria: ${category.name}")
                        CategoryCard(category)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showChooseActionDialog) {
        ChooseActionDialog(
            onDismiss = { showChooseActionDialog = false },
            onAddPassword = {
                showChooseActionDialog = false
                showAddPasswordDialog = true
            },
            onAddCategory = {
                showChooseActionDialog = false
                showAddCategoryDialog = true
            }
        )
    }

    if (showAddPasswordDialog) {
        AddPasswordDialog(
            onDismiss = { showAddPasswordDialog = false },
            onSave = { title, username, password, description ->
                val obfuscatedPassword = obfuscatePassword(password, uid)
                savePasswordEntry(uid, "SitesWeb", title, username, obfuscatedPassword, description)
                scope.launch {
                    Log.d("PasswordManagerScreen", "Categorias atualizadas após adicionar senha: ${categories.map { it.name }}")
                }
                showAddPasswordDialog = false
            },
            uid = uid
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { categoryName ->
                createCategory(uid, categoryName)
                scope.launch {
                    Log.d("PasswordManagerScreen", "Categorias atualizadas após adicionar categoria: ${categories.map { it.name }}")
                }
                showAddCategoryDialog = false
            }
        )
    }
}
@Composable
fun CategoryCard(category: Category) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(category.name) {
        Log.d("CategoryCard", "Inicializando categoria: ${category.name}, expanded: $expanded")
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                        Log.d("CategoryCard", "Categoria ${category.name} clicada, novo estado expanded: $expanded")
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Recolher" else "Expandir"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (category.services.isEmpty()) {
                    Text(
                        text = "Nenhum serviço nesta categoria",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Log.d("CategoryCard", "Exibindo serviços para ${category.name}: ${category.services.map { it.title }}")
                    category.services.forEach { service ->
                        PasswordCard(service)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                Log.d("CategoryCard", "Categoria ${category.name} não expandida, serviços ocultos")
            }
        }
    }
}
@Composable
fun CategoryRow(category: Category) {
    var categoryInput by remember { mutableStateOf(TextFieldValue(category.name)) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedTextField(
            value = categoryInput,
            onValueChange = { categoryInput = it },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            singleLine = true,
            enabled = false // Campo apenas para exibição, não editável
        )
        IconButton(
            onClick = {
                expanded = !expanded
                Log.d("CategoryRow", "Botão clicado para ${category.name}, expanded: $expanded")
            }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Recolher" else "Expandir"
            )
        }
    }

    if (expanded) {
        Spacer(modifier = Modifier.height(8.dp))
        if (category.services.isEmpty()) {
            Text(
                text = "Nenhum serviço nesta categoria",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp)
            )
        } else {
            category.services.forEach { service ->
                PasswordCard(service)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PasswordCard(password: PasswordEntry) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
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
                text = "Usuário: ${password.username ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Senha: ******", // Mascarado por padrão
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Descrição: ${password.description ?: "N/A"}", // Exibe descrição ou "N/A"
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
fun ChooseActionDialog(
    onDismiss: () -> Unit,
    onAddPassword: () -> Unit,
    onAddCategory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Escolha uma Ação") },
        text = {
            Column {
                Button(
                    onClick = onAddPassword,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adicionar Nova Senha")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddCategory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adicionar Nova Categoria")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddPasswordDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, username: String?, password: String, description: String?) -> Unit,
    uid: String
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Adicionar Nova Senha") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuário (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && password.isNotEmpty()) {
                        onSave(title, if (username.isEmpty()) null else username, password, if (description.isEmpty()) null else description)
                    }
                },
                enabled = title.isNotEmpty() && password.isNotEmpty()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (categoryName: String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Adicionar Nova Categoria") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Nome da Categoria") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotEmpty()) {
                        onSave(categoryName)
                    }
                },
                enabled = categoryName.isNotEmpty()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}
@Preview(showBackground = true)
@Composable
fun PasswordManagerScreenPreview() {
    Pi3_time1Theme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PasswordManagerScreen(
                onLogout = {},
                onNavigateToCategories = {}
            )
        }
    }
}
