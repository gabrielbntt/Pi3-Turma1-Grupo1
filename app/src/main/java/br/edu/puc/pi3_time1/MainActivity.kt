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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PasswordEntry(
    val username: String?,
    val password: String,
    val description: String?
)

data class Category(
    val name: String,
    val services: List<PasswordEntry>
)

fun checkEmailVerification(onResult: (Boolean) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    if (user == null) {
        Log.w("CheckEmail", "Usuário não logado")
        onResult(false)
        return
    }

    user.reload()
        .addOnSuccessListener {
            val isVerified = user.isEmailVerified
            if (isVerified) {
                updateEmailVerificationStatus(user.uid)
            }
            onResult(isVerified) // Passa o resultado para o callback
        }
        .addOnFailureListener { e ->
            Log.e("CheckEmail", "Erro ao verificar email", e)
            onResult(false) // Define como false em caso de falha
        }
}

private fun updateEmailVerificationStatus(uid: String) {
    val db = FirebaseFirestore.getInstance()
    val userDocRef = db.collection("Users").document(uid)
    val data = mapOf("hasEmailVerified" to true)
    userDocRef
        .set(data)
        .addOnSuccessListener {
            Log.d("UpdateStatus", "Documento criado e verificação de email definida para o usuário $uid")
        }
        .addOnFailureListener { e ->
            Log.e("UpdateStatus", "Erro ao criar documento para o usuário $uid", e)
        }
}

fun createCategory(uid: String, categories: List<String>) {
    val db = Firebase.firestore
    val vazio = hashMapOf("placeholder" to true)

    categories.forEach { categoryName ->
        db.collection("Collections")
            .document(uid)
            .collection(categoryName)
            .add(vazio)
    }

    val docRef = db.collection("Collections").document(uid)
    docRef.update("categoriesList", FieldValue.arrayUnion(*categories.toTypedArray()))
        .addOnFailureListener {
            docRef.set(mapOf("categoriesList" to categories), SetOptions.merge())
        }
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
fun savePasswordEntry(uid: String, categoryName: String, username: String?, password: String, description: String?) {
    val db = Firebase.firestore
    val obfuscatedPassword = obfuscatePassword(password, uid)
    val entry = PasswordEntry(
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

suspend fun fetchCategories(uid: String): List<String> {
    val db = Firebase.firestore
    val docRef = db.collection("Collections").document(uid)
    val snapshot = docRef.get().await()
    return snapshot.get("categoriesList") as? List<String> ?: emptyList()
}

suspend fun fetchPasswordsForCategory(uid: String, category: String): List<PasswordEntry> {
    val db = Firebase.firestore
    val passwordsSnapshot = db.collection("Collections")
        .document(uid)
        .collection(category)
        .get()
        .await()

    return passwordsSnapshot.documents.map { doc ->
        PasswordEntry(
            username = doc.getString("username"),
            password = doc.getString("password") ?: "",
            description = doc.getString("description")
        )
    }
}

private lateinit var auth: FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pi3_time1Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PasswordManagerScreen(
                        onNavigateToCategories = {
                            startActivity(Intent(this@MainActivity, CategoriesActivity::class.java))
                        },
                        onNavigateToAccount ={
                            startActivity(Intent(this@MainActivity, AccountActivity::class.java))
                        },
                        onNavigateToQrCode = {
                            startActivity(Intent(this@MainActivity, QrCodeReaderActivity::class.java))
                        },
                        onLogout = {
                            Firebase.auth.signOut()
                            startActivity(
                                Intent(this@MainActivity, WelcomeActivity::class.java)
                                    .apply {
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                            )
                            finish()
                        },
                        snackbarMessage = intent.getStringExtra("SNACKBAR_MESSAGE")
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordManagerScreenPreview() {
    Pi3_time1Theme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PasswordManagerScreen(
                onLogout = {},
                onNavigateToCategories = {},
                onNavigateToAccount = {},
                onNavigateToQrCode = {},
                snackbarMessage = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(
    onLogout: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToQrCode: () -> Unit,
    snackbarMessage: String? = null
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showChooseActionDialog by remember { mutableStateOf(false) }
    var showAddPasswordDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    val uid = Firebase.auth.currentUser?.uid ?: "default_uid"
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val DarkBlue = Color(0xFF253475)
    val Gray = Color(0xFF666666)
    val LightGray = Color(0xFFDDDDDD)
    val SuccessGreen = Color(0xFF07AE33)
    val ErrorRed = Color(0xFFFF1717)

    // Show Snackbar if a message is provided
    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = snackbarMessage,
                    actionLabel = "Entendido",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    LaunchedEffect(uid) {
        val categoryNames = fetchCategories(uid)
        val loadedCategories = categoryNames.map { categoryName ->
            val passwords = fetchPasswordsForCategory(uid, categoryName)
            Category(name = categoryName, services = passwords)
        }
        categories = loadedCategories
    }

    val filteredCategories = categories.filter { category ->
        category.name.contains(searchQuery.text, ignoreCase = true) ||
                category.services.any {
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
                    label = { Text("Conta") },
                    selected = false,
                    onClick = { onNavigateToAccount()},
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    onClick = { onLogout() },
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar"
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
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
            onSave = { username, password, description, categoryName ->
                savePasswordEntry(uid, categoryName, username, password, description)
                showAddPasswordDialog = false
            },
            uid = uid,
            categories = categories.map { it.name }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { categoryName ->
                createCategory(uid, listOf(categoryName))
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
                    Log.d("CategoryCard", "Exibindo serviços para ${category.name}: ${category.services.size} serviços")
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
                text = "Usuário: ${password.username ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Senha: ******",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Descrição: ${password.description ?: "N/A"}",
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
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val DarkBlue = Color(0xFF253475)
    val Gray = Color(0xFF666666)
    val LightGray = Color(0xFFDDDDDD)
    val SuccessGreen = Color(0xFF07AE33)
    val ErrorRed = Color(0xFFFF1717)

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = LightGray,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ADICIONAR SENHA/CATEGORIA",
                    color = Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column {
                Button(
                    onClick = onAddCategory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue,
                        contentColor = White
                    )
                ) {
                    Text(text = "Nova Categoria", fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddPassword,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue,
                        contentColor = White
                    )
                ) {
                    Text(text = "Nova Senha", fontWeight = FontWeight.ExtraBold)
                }

            }
        },
        confirmButton = {},
        dismissButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = White
                )
            ) {
                Text(text = "Cancelar", fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordDialog(
    onDismiss: () -> Unit,
    onSave: (username: String?, password: String, description: String?, categoryName: String) -> Unit,
    uid: String,
    categories: List<String>
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Adicionar Nova Senha") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = if (selectedCategory.isEmpty()) "" else selectedCategory,
                        onValueChange = { selectedCategory = it },
                        label = { Text("Categoria") },
                        placeholder = { Text("Categoria") },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { expanded = true },
                        readOnly = true
                    )
                    IconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = if (expanded) "Recolher" else "Expandir"
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
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
                    if (password.isNotEmpty() && selectedCategory.isNotEmpty()) {
                        onSave(if (username.isEmpty()) null else username, password, if (description.isEmpty()) null else description, selectedCategory)
                    }
                },
                enabled = password.isNotEmpty() && selectedCategory.isNotEmpty()
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
        title = { Text("Adicionar Categoria") },
        text = {
            Column {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Nome da categoria") },
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
