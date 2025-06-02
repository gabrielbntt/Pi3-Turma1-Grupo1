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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Black
import br.edu.puc.pi3_time1.ui.theme.DarkBlue
import br.edu.puc.pi3_time1.ui.theme.ErrorRed
import br.edu.puc.pi3_time1.ui.theme.Gray
import br.edu.puc.pi3_time1.ui.theme.LightGray
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import br.edu.puc.pi3_time1.ui.theme.White
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
            Pi3_time1Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PasswordManagerScreen(
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
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFFFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color(0xFFFFFFFF),

                            )
                        }
                    },
                            colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF253475)
                ))
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(16.dp)
                        .border(width = 2.dp, color = Color(0xFF000000), shape = CircleShape)
                        .background(color = Color(0xFFFFFFFF), shape = CircleShape)
                        .clickable { showChooseActionDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Adicionar",
                        modifier = Modifier
                            .size(25.dp)
                            .align(Alignment.Center),
                        tint = Color.Black
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                BottomAppBar(
                    containerColor = Color(0xFF253475),
                    tonalElevation = 2.dp,
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
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
                )
            }) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color(0xFF253475),
                shape = RoundedCornerShape(8.dp)
            ),

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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight(800)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    tint = Color(0xFF253475),
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
        title = { Text("Adicionar") },
        text = {
            Column {
                Button(
                    onClick = onAddPassword,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nova Senha")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddCategory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nova Categoria")
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
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        containerColor = LightGray,
        title = { Box(
                modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Adicionar Senha",
                color = Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold
            )
        }
        },
        text = {
            Column {
                Box(
                        modifier = Modifier
                        .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = DarkBlue,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategory.isEmpty()) "Selecionar Categoria" else selectedCategory,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = if (expanded) "Recolher" else "Expandir",
                            tint = DarkBlue
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .width(250.dp)
                            .heightIn(max = 200.dp)
                            .background(
                                color = LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = category,
                                        fontWeight = FontWeight.Medium,
                                        color = Black
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título", color = Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Black,
                        unfocusedTextColor = Black,
                        cursorColor = Black,
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = DarkBlue,
                        errorBorderColor = ErrorRed,
                        errorLabelColor = ErrorRed
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuário (Opcional)", color = Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Black,
                        unfocusedTextColor = Black,
                        cursorColor = Black,
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = DarkBlue,
                        errorBorderColor = ErrorRed,
                        errorLabelColor = ErrorRed
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "Senha", color = Gray) },
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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Black,
                        unfocusedTextColor = Black,
                        cursorColor = Black,
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = DarkBlue,
                        errorBorderColor = ErrorRed,
                        errorLabelColor = ErrorRed
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (Opcional)", color = Gray) },
                    shape = RoundedCornerShape(8.dp),
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
                enabled = title.isNotEmpty() && password.isNotEmpty() && selectedCategory.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue,
                    contentColor = White,
                    disabledContainerColor = DarkBlue.copy(alpha = 0.3f),
                    disabledContentColor = White.copy(alpha = 0.6f))

            ) {
                Text("Salvar", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            Button(onClick = {
                onDismiss()
                 },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = White
                )
                ) {
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
                snackbarMessage = null
            )
        }
    }
}
