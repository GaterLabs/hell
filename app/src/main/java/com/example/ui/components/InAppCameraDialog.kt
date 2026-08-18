package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.util.LocalAppLanguage
import com.example.ui.util.localizedLabel
import java.io.File
import java.util.UUID

@Composable
fun InAppCameraDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val language = LocalAppLanguage.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) errorMessage = localizedLabel(language, "Izin kamera diperlukan untuk mengambil foto.", "Camera permission is required to take a photo.")
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(hasPermission, lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        if (hasPermission) {
            val future = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            val listener = Runnable {
                try {
                    provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder().build()
                    provider?.unbindAll()
                    provider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                    imageCapture = capture
                } catch (error: Exception) {
                    errorMessage = error.message ?: localizedLabel(language, "Kamera tidak tersedia.", "Camera is unavailable.")
                }
            }
            future.addListener(listener, executor)
        }
        onDispose {
            provider?.unbindAll()
            imageCapture = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        localizedLabel(language, "Ambil Foto Warung", "Take Store Photo"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, localizedLabel(language, "Tutup", "Close")) }
                }
                if (hasPermission) {
                    AndroidViewPreview(previewView)
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            val file = File(context.cacheDir, "store_${UUID.randomUUID()}.jpg")
                            val output = ImageCapture.OutputFileOptions.Builder(file).build()
                            isCapturing = true
                            capture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    errorMessage = exception.message ?: localizedLabel(language, "Gagal mengambil foto.", "Failed to capture photo.")
                                }

                                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    onPhotoCaptured(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                                }
                            })
                        },
                        enabled = imageCapture != null && !isCapturing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCapturing) localizedLabel(language, "Menyimpan...", "Saving...") else localizedLabel(language, "Ambil Foto", "Capture Photo"))
                    }
                } else {
                    Text(
                        localizedLabel(language, "Aktifkan izin kamera untuk melanjutkan.", "Enable camera permission to continue."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) {
                        Text(localizedLabel(language, "Izinkan Kamera", "Allow Camera"))
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidViewPreview(previewView: PreviewView) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxWidth().height(320.dp)
    )
}
