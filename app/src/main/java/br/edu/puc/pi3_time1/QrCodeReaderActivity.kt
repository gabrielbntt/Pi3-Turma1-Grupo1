package br.edu.puc.pi3_time1

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button

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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class QrCodeReaderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pi3_time1Theme {
                QrCodeScannerScreen(
                    onQrCodeDetected = { scannedQrCode ->
                        Log.d("QRCode", "QR Code detectado: $scannedQrCode")
                    },
                    onNavigateBack = {
                        startActivity(Intent(this@QrCodeReaderActivity, MainActivity::class.java))

                    }
                )
            }
        }
    }

}

fun verificarToken(loginToken: String, firestore: FirebaseFirestore, activity: Activity) {
    val loginRef = firestore.collection("login").document(loginToken)

    loginRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                Log.d("QRCode", "Token válido encontrado no Firestore")
                val siteUrl = document.getString("partnerUrl")
                if (siteUrl != null) {
                    Log.d("Firestore", "URL do site: $siteUrl")
                    CoroutineScope(Dispatchers.IO).launch {
                        validateLogin(siteUrl, loginToken)
                    }
                } else {
                    Log.d("Firestore", "Campo partnerUrl não encontrado no documento")
                }

            } else {
                Log.d("QRCode", "Token não encontrado no Firestore")
                // Mostrar erro, feedback, etc.
            }
        }
        .addOnFailureListener { e ->
            Log.e("QRCode", "Erro ao acessar Firestore", e)
        }
}

@Composable
fun QrCodeScannerScreen(
    onQrCodeDetected: (String) -> Unit,
    onNavigateBack:() -> Unit

) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var detectedQrCode by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
            hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            Box(modifier = Modifier.weight(1f)) {
                CameraPreviewScreen(
                    onQrCodeDetected = { scannedQrCode ->
                        if (detectedQrCode == null) {
                            detectedQrCode = scannedQrCode
                            onQrCodeDetected(scannedQrCode)


                        }
                    }
                )
            }

            // Exibe o QR Code detectado na parte inferior da tela
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                detectedQrCode?.let {
                    Text("QR Code Detectado:")
                    Text(it)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateBack) {
                    Text("Retornar")
                }
            }

            LaunchedEffect(detectedQrCode) {
                if (detectedQrCode != null) {
                    delay(2000)
                    detectedQrCode = null
                }
            }

        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("A permissão da câmera é necessária para ler QR Codes.")
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Solicitar Permissão de Câmera")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewScreen(onQrCodeDetected: (String) -> Unit) {
    val firestore = Firebase.firestore
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var qrCodeContent by remember { mutableStateOf("") }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraExecutor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(barcodeScanner, imageProxy) { barcode ->
                                // Chama o callback quando um QR Code é detectado
                                onQrCodeDetected(barcode)
                                if (barcode.isNotBlank()) {
                                    verificarToken(barcode, firestore, activity)
                                }
                            }
                        }
                    }

                cameraProvider.unbindAll()
                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
//                    Log.e("CameraPreviewScreen", "Erro ao vincular a câmera", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onQrCodeDetected: (String) -> Unit
) {
    val inputImage =
        InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

    barcodeScanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            barcodes.forEach { barcode ->
                barcode.rawValue?.let {
                    // Passa o valor do QR Code detectado para o callback
                    onQrCodeDetected(it)
                }
            }
        }
        .addOnFailureListener {
//            Log.e("CameraPreviewScreen", "Falha na análise da imagem", it)
        }
        .addOnCompleteListener {
            // É crucial fechar a ImageProxy para liberar o buffer de imagem
            imageProxy.close()
        }
}
