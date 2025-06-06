package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import androidx.compose.ui.text.TextStyle
import br.edu.puc.pi3_time1.ui.theme.Black
import br.edu.puc.pi3_time1.ui.theme.DarkBlue
import br.edu.puc.pi3_time1.ui.theme.LightGray
import br.edu.puc.pi3_time1.ui.theme.White
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

fun renameCategory(
    userId: String,
    nomeAtual: String,
    novoNome: String,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("Collections").document(userId)

    val colecaoAntiga = docRef.collection(nomeAtual)
    val colecaoNova = docRef.collection(novoNome)

    colecaoAntiga.get()
        .addOnSuccessListener { snapshot ->
            val batch = db.batch()

            for (document in snapshot.documents) {
                val novoDocRef = colecaoNova.document(document.id)
                batch.set(novoDocRef, document.data ?: mapOf<String, Any>())
                batch.delete(document.reference)
            }

            docRef.get().addOnSuccessListener { userDoc ->
                val categoriesList = userDoc.get("categoriesList") as? MutableList<String> ?: mutableListOf()

                if (categoriesList.contains(nomeAtual)) {
                    categoriesList.remove(nomeAtual)
                    categoriesList.add(novoNome)
                    docRef.update("categoriesList", categoriesList)
                }

                val categoriasRef = docRef.collection("categories")
                categoriasRef.document(nomeAtual).get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val dados = doc.data
                        categoriasRef.document(novoNome).set(dados ?: mapOf<String, Any>())
                        categoriasRef.document(nomeAtual).delete()
                    }
                }
            }

            batch.commit().addOnSuccessListener {
                Log.d("Firestore", "Categoria renomeada com sucesso.")
                onSuccess()
            }.addOnFailureListener { e ->
                Log.e("Firestore", "Erro ao renomear categoria", e)
                onFailure(e)
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Erro ao acessar documentos da categoria antiga", e)
            onFailure(e)
        }
}

fun deleteCategory(
    userId: String,
    categoria: String,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("Collections").document(userId)
    val colecaoRef = docRef.collection(categoria)

    colecaoRef.get()
        .addOnSuccessListener { snapshot ->
            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }

            docRef.get().addOnSuccessListener { userDoc ->
                val categoriesList = userDoc.get("categoriesList") as? MutableList<String> ?: mutableListOf()
                if (categoriesList.contains(categoria)) {
                    categoriesList.remove(categoria)
                    docRef.update("categoriesList", categoriesList)
                }

                val categoriasRef = docRef.collection("categories")
                categoriasRef.document(categoria).delete()
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d("Firestore", "Categoria excluída com sucesso.")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Erro ao excluir categoria", e)
                    onFailure(e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Erro ao acessar documentos da categoria", e)
            onFailure(e)
        }
}

class CategoriesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()){
            Pi3_time1Theme {
                Categories(
                    onNavigateToMainActivity = {
                        startActivity(Intent(this@CategoriesActivity, MainActivity::class.java))
                    },
                    snackbarMessage = intent.getStringExtra("SNACKBAR_MESSAGE")
                )
            } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Categories(
    modifier: Modifier = Modifier,
    onNavigateToMainActivity: () -> Unit,
    snackbarMessage: String? = null
) {
    val uid = Firebase.auth.currentUser?.uid ?: "default_uid"
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid, refreshTrigger) {
        try {
            isLoading = true
            categories = fetchCategories(uid)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Erro ao carregar categorias"
        } finally {
            isLoading = false
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF253475)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFF253475),
                content = {

                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Suas Categorias",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 16.dp)

            )

            if (isLoading) {
                Text("Carregando categorias...", fontSize = 18.sp)
            } else if (errorMessage != null) {
                Text(errorMessage ?: "", color = Color.Red)
            } else {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = category,
                            onValueChange = {},
                            modifier = Modifier
                                .weight(1f)
                                .border(2.dp, Color(0xFF253475))
                                .background(Color.White)
                                .height(59.dp),
                            enabled = false,
                            textStyle = TextStyle(
                                textAlign = TextAlign.Left,
                                fontSize = 20.sp,
                                color = Color.Black,
                                fontWeight = FontWeight(800)
                            ),
                            trailingIcon = {
                                if (category != "SitesWeb") {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                categoryToEdit = category
                                                showEditDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar categoria",
                                                tint = Color(0xFF1A2C71)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                categoryToDelete = category
                                                showDeleteDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir categoria",
                                                tint = Color(0xFF1A2C71)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (showDeleteDialog && categoryToDelete != null) {
                DeleteCategoryDialog(
                    uid = uid,
                    categoryName = categoryToDelete!!,

                    onDismiss = {
                        showDeleteDialog = false
                        categoryToDelete = null
                    },
                    onConfirm = {
                        deleteCategory(
                            userId = uid,
                            categoria = categoryToDelete!!,
                            onSuccess = {
                                refreshTrigger = !refreshTrigger
                                showDeleteDialog = false
                                categoryToDelete = null
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Categoria excluída com sucesso!",
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onFailure = { e ->
                                Log.e("Categories", "Erro ao excluir: $e")
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Erro ao excluir categoria: ${e.message}",
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                                showDeleteDialog = false
                                categoryToDelete = null
                            }
                        )
                    }
                )
            }

            if (showEditDialog && categoryToEdit != null) {
                EditCategoryDialog(
                    nomeAtual = categoryToEdit!!,
                    onDismiss = {
                        showEditDialog = false
                        categoryToEdit = null
                    },
                    onSave = { newCategoryName ->
                        renameCategory(
                            userId = uid,
                            nomeAtual = categoryToEdit!!,
                            novoNome = newCategoryName,
                            onSuccess = {
                                refreshTrigger = !refreshTrigger
                                showEditDialog = false
                                categoryToEdit = null
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Categoria editada com sucesso!",
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onFailure = { e ->
                                Log.e("Categories", "Erro ao renomear: $e")
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Erro ao editar categoria: ${e.message}",
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                                showEditDialog = false
                                categoryToEdit = null
                            }
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                //botao de retornar a pagina principal
                onClick = { onNavigateToMainActivity() },
                modifier = Modifier
                    .border(width = 2.dp, Color(0xFF000000), shape = RoundedCornerShape(size = 100.dp))
                    .width(160.dp)
                    .height(55.dp)
                    .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(size = 100.dp))

            ) {
                Text(
                    text = "Voltar",
                    fontSize =16.sp,
                    fontWeight = FontWeight(700),
                    modifier = Modifier
                        .width(48.dp)
                        .height(20.dp)
                )
            }
        }
    }
}

@Composable
fun EditCategoryDialog(
    //Pop-up de Editar Categoria
    nomeAtual: String,
    onDismiss: () -> Unit,
    onSave: (novoNome: String) -> Unit
) {
    var categoryName by remember { mutableStateOf(nomeAtual) }

    AlertDialog(
        containerColor = LightGray,
        onDismissRequest = { onDismiss() },
        title = { Text("Editar Categoria",
            color = Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold) },
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
                //botao que salva as alteracoes
                onClick = {
                    if (categoryName.isNotEmpty()) {
                        onSave(categoryName)
                    }
                },
                enabled = categoryName.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = White,
                    disabledContainerColor = DarkBlue.copy(alpha = 0.3f),
                    disabledContentColor = White.copy(alpha = 0.6f))
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            //botao de cancelar operacao
            Button(onClick = { onDismiss() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = White,
                    disabledContainerColor = DarkBlue.copy(alpha = 0.3f),
                    disabledContentColor = White.copy(alpha = 0.6f))) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteCategoryDialog(
    //Pop-up de exclusao de senha
    categoryName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    uid: String
) {
    AlertDialog(
        containerColor = LightGray,
        onDismissRequest = { onDismiss() },
        title = { Text("Tem Certeza que deseja excluir a Categoria?") },
        text = {
            Column {
                Text(
                    text = "Atenção: Ao excluir a categoria ${categoryName}, " +
                            "as senhas dessa categoria serão excluídas."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    //botao de confirmar a exclusao
                    onConfirm()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = White)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            //botao de cancelar a exclusao
            Button(onClick = { onDismiss() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = White)) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CategoriesPreview() {
    Pi3_time1Theme {
        Categories(
            onNavigateToMainActivity = {}
        )
    }
}
