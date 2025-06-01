package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

data class PasswordEntry(
    val title: String,
    val username: String?,
    val password: String,
    val description: String?,
    val acesstoken: String
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
            onResult(isVerified)
        }
        .addOnFailureListener { e ->
            Log.e("CheckEmail", "Erro ao verificar email", e)
            onResult(false)
        }
}

private fun updateEmailVerificationStatus(uid: String) {
    val db = FirebaseFirestore.getInstance()
    val userDocRef = db.collection("Users").document(uid)
    val data = mapOf("hasEmailVerified" to true)
    userDocRef
        .set(data, SetOptions.merge())
        .addOnSuccessListener {
            Log.d("UpdateStatus", "Campo hasEmailVerified atualizado para o usuário $uid")
        }
        .addOnFailureListener { e ->
            Log.e("UpdateStatus", "Erro ao atualizar hasEmailVerified para o usuário $uid", e)
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
                Log.w("Firestore", "Nenhuma senha com o título: $title")
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

fun updatePasswordByTitle(uid: String, categoryName: String, title: String, fieldToUpdate: String, newPassword: String) {
    searchDocumentByTitle(uid, categoryName, title) { document ->
        val currentValue = document.get(fieldToUpdate)

        val valueToUpdate = if (fieldToUpdate == "password") {
            val hashed = obfuscatePassword(newPassword, uid)
            if (hashed == currentValue) {
                Log.d("Firestore", "A nova senha é igual à atual. Nenhuma atualização feita.")
                return@searchDocumentByTitle
            }
            hashed
        } else {
            if (currentValue == newPassword) {
                Log.d("Firestore", "O valor de '$fieldToUpdate' já está atualizado. Nenhuma modificação feita.")
                return@searchDocumentByTitle
            }
            newPassword
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

fun generateAccessToken(): String {
    val randomBytes = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    return android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
}
fun updateAccessToken(
    uid: String,
    categoryName: String,
    title: String,
    newAccessToken: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = Firebase.firestore

    db.collection("Collections")
        .document(uid)
        .collection(categoryName)
        .whereEqualTo("title", title)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                onFailure(Exception("Entrada não encontrada para o título: $title"))
                return@addOnSuccessListener
            }
            val document = querySnapshot.documents.first()

            document.reference
                .update("acesstoken", newAccessToken)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }
        .addOnFailureListener { e ->
            onFailure(e)
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

fun savePasswordEntry(uid: String, categoryName: String, title: String, username: String?, password: String, description: String?, accessToken: String) {
    val db = Firebase.firestore
    val obfuscatedPassword = obfuscatePassword(password, uid)
    val entry = PasswordEntry(
        title = title,
        username = username,
        password = obfuscatedPassword,
        description = description,
        acesstoken = accessToken
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
            title = doc.getString("title") ?: "",
            username = doc.getString("username"),
            password = doc.getString("password") ?: "",
            description = doc.getString("description"),
            acesstoken = doc.getString("acesstoken") ?: ""
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            Pi3_time1Theme (darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PasswordManagerScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onNavigateToCategories = {
                            startActivity(Intent(this@MainActivity, CategoriesActivity::class.java))
                        },
                        onNavigateToAccount = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
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
    var showEditPasswordDialog by remember { mutableStateOf(false) }
    var showDeletePasswordDialog by remember { mutableStateOf(false) }
    var passwordToEdit by remember { mutableStateOf<PasswordEntry?>(null) }
    var passwordToDelete by remember { mutableStateOf<PasswordEntry?>(null) }
    val uid = Firebase.auth.currentUser?.uid ?: "default_uid"
    val accessToken = remember { generateAccessToken() }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isVerified by remember { mutableStateOf<Boolean?>(null) }
    var refreshTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        checkEmailVerification { result ->
            isVerified = result
        }
    }

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

    LaunchedEffect(uid,refreshTrigger) {
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
                    it.username?.contains(searchQuery.text, ignoreCase = true) == true ||
                            it.title.contains(searchQuery.text, ignoreCase = true)
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
                    onClick = { onNavigateToAccount() },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    onClick = { onLogout() },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (isDarkTheme) "Modo Claro" else "Modo Escuro")
                }
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
                    modifier = Modifier
                        .size(80.dp)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxSize()
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
                            CategoryCard(
                                category = category,
                                onEditPassword = { password ->
                                    passwordToEdit = password
                                    showEditPasswordDialog = true
                                },
                                onDeletePassword = { password ->
                                    passwordToDelete = password
                                    showDeletePasswordDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                Button(
                    onClick = {
                        when (isVerified) {
                            true -> onNavigateToQrCode()
                            false -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Por favor, verifique seu email antes de usar o leitor de QR code.",
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                            null -> {}
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(63.7.dp)
                        .height(63.7.dp)
                        .border(width = 2.dp, color = Color(0xFF000000), shape = CircleShape),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = Color(0xFFFFFFFF), shape = CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.qr_code_logo),
                                contentDescription = "Ícone QR Code",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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
            onSave = { title, username, password, description, categoryName ->
                savePasswordEntry(uid, categoryName, title, username, password, description, accessToken)
                showAddPasswordDialog = false
                scope.launch {
                    val categoryNames = fetchCategories(uid)
                    val loadedCategories = categoryNames.map { categoryName ->
                        val passwords = fetchPasswordsForCategory(uid, categoryName)
                        Category(name = categoryName, services = passwords)
                    }
                    categories = loadedCategories
                    refreshTrigger = !refreshTrigger
                    snackbarHostState.showSnackbar(
                        message = "Senha adicionada com sucesso!",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                }},
            uid = uid,
            categories = categories.map { it.name },
            accessToken = accessToken

        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { categoryName ->
                createCategory(uid, listOf(categoryName))
                showAddCategoryDialog = false
                scope.launch {
                    val categoryNames = fetchCategories(uid)
                    val loadedCategories = categoryNames.map { categoryName ->
                        val passwords = fetchPasswordsForCategory(uid, categoryName)
                        Category(name = categoryName, services = passwords)
                    }
                    categories = loadedCategories
                    refreshTrigger = !refreshTrigger
                    snackbarHostState.showSnackbar(
                        message = "Categoria adicionada com sucesso!",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short)
                }
            }
        )
    }

    if (showEditPasswordDialog && passwordToEdit != null) {
        EditPasswordDialog(
            currentPassword = passwordToEdit!!,
            onDismiss = {
                showEditPasswordDialog = false
                passwordToEdit = null
            },
            onSave = { newTitle, newUsername, newPassword, newDescription ->
                val category = categories.find { cat ->
                    cat.services.any { it.title == passwordToEdit!!.title }
                }
                if (category != null) {
                    if (newTitle != passwordToEdit!!.title) {
                        updatePasswordByTitle(uid, category.name, passwordToEdit!!.title, "title", newTitle)
                    }
                    if (newUsername != passwordToEdit!!.username) {
                        updatePasswordByTitle(uid, category.name, passwordToEdit!!.title, "username", newUsername ?: "")
                    }

                    if (newPassword != passwordToEdit!!.password) {
                        updatePasswordByTitle(uid, category.name, passwordToEdit!!.title, "password", newPassword)
                    }

                    if (newDescription != passwordToEdit!!.description) {
                        updatePasswordByTitle(uid, category.name, passwordToEdit!!.title, "description", newDescription ?: "")
                    }

                    scope.launch {
                        val categoryNames = fetchCategories(uid)
                        val loadedCategories = categoryNames.map { categoryName ->
                            val passwords = fetchPasswordsForCategory(uid, categoryName)
                            Category(name = categoryName, services = passwords)
                        }
                        categories = loadedCategories
                        refreshTrigger = !refreshTrigger
                        snackbarHostState.showSnackbar(
                            message = "Senha editada com sucesso!",
                            actionLabel = "OK",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                showEditPasswordDialog = false
                passwordToEdit = null
            }
        )
    }

    if (showDeletePasswordDialog && passwordToDelete != null) {
        DeletePasswordDialog(
            passwordTitle = passwordToDelete!!.title,
            onDismiss = {
                showDeletePasswordDialog = false
                passwordToDelete = null
            },
            onConfirm = {

                val category = categories.find { cat ->
                    cat.services.any { it.title == passwordToDelete!!.title }
                }
                if (category != null) {
                    deletePasswordByTitle(uid, category.name, passwordToDelete!!.title)

                    scope.launch {
                        val categoryNames = fetchCategories(uid)
                        val loadedCategories = categoryNames.map { categoryName ->
                            val passwords = fetchPasswordsForCategory(uid, categoryName)
                            Category(name = categoryName, services = passwords)
                        }
                        categories = loadedCategories
                        refreshTrigger = !refreshTrigger
                        snackbarHostState.showSnackbar(
                            message = "Senha excluída com sucesso!",
                            actionLabel = "OK",
                            duration = SnackbarDuration.Short)
                    }
                }
                showDeletePasswordDialog = false
                passwordToDelete = null

            }
        )
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onEditPassword: (PasswordEntry) -> Unit,
    onDeletePassword: (PasswordEntry) -> Unit
) {
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
                        PasswordCard(
                            password = service,
                            onEdit = { onEditPassword(service) },
                            onDelete = { onDeletePassword(service) }
                        )
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
fun PasswordCard(
    password: PasswordEntry,
    onEdit: (PasswordEntry) -> Unit,
    onDelete: (PasswordEntry) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = " ${password.title}",
                    style = MaterialTheme.typography.bodyMedium
                )
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
            Row {
                IconButton(onClick = { onEdit(password) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar senha",
                        tint = Color(0xFF1A2C71)
                    )
                }
                IconButton(onClick = { onDelete(password) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir senha",
                        tint = Color(0xFF1A2C71)
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, username: String?, password: String, description: String?, categoryName: String) -> Unit,
    uid: String,
    categories: List<String>,
    accessToken: String
) {
    var title by remember { mutableStateOf("") }
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
                    if (title.isNotEmpty() && password.isNotEmpty() && selectedCategory.isNotEmpty()) {
                        onSave(
                            title,
                            if (username.isEmpty()) null else username,
                            password,
                            if (description.isEmpty()) null else description,
                            selectedCategory
                        )
                    }
                },
                enabled = title.isNotEmpty() && password.isNotEmpty() && selectedCategory.isNotEmpty()
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
@Composable
fun EditPasswordDialog(
    currentPassword: PasswordEntry,
    onDismiss: () -> Unit,
    onSave: (title: String, username: String?, password: String, description: String?) -> Unit
) {
    var title by remember { mutableStateOf(currentPassword.title) }
    var username by remember { mutableStateOf(currentPassword.username ?: "") }
    var password by remember { mutableStateOf(currentPassword.password) }
    var description by remember { mutableStateOf(currentPassword.description ?: "") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Editar Senha") },
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
                        onSave(
                            title,
                            if (username.isEmpty()) null else username,
                            password,
                            if (description.isEmpty()) null else description
                        )
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
fun DeletePasswordDialog(
    passwordTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Tem Certeza que deseja excluir a Senha?") },
        text = {
            Column {
                Text(
                    text = "Atenção: Ao excluir a senha '${passwordTitle}', " +
                            "ela será permanentemente removida."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                }
            ) {
                Text("Confirmar")
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
                onNavigateToCategories = {},
                onNavigateToAccount = {},
                onNavigateToQrCode = {},
                snackbarMessage = null,
                isDarkTheme = false,
                onToggleTheme = {}
            )
        }
    }
}
