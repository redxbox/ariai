package com.ariai.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.widget.FrameLayout
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.ariai.app.ui.theme.AriAccent
import com.ariai.app.ui.theme.AriCard
import com.ariai.app.ui.theme.AriInk
import com.ariai.app.ui.theme.AriMuted
import com.ariai.app.util.tx
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun LiveQrScannerDialog(
    onScan: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val currentOnScan by rememberUpdatedState(onScan)
    val currentOnError by rememberUpdatedState<(String) -> Unit>({ error -> cameraError = error })
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermissionGranted = granted
        if (!granted) cameraError = tx("Camera permission denied.")
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = AriCard,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AriAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(tx("Scan provider QR"), modifier = Modifier.weight(1f), color = AriInk, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tx("Close"), tint = AriMuted)
                    }
                }
                if (cameraPermissionGranted) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(360.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black)
                            .testTag("provider-qr-camera-preview")
                    ) {
                        AndroidView(
                            factory = { viewContext ->
                                QrPreviewView(viewContext).apply {
                                    onCodeFound = { code -> currentOnScan(code) }
                                    onCameraError = { error -> currentOnError(error) }
                                    start(lifecycleOwner)
                                }
                            },
                            update = { view ->
                                view.onCodeFound = { code -> currentOnScan(code) }
                                view.onCameraError = { error -> currentOnError(error) }
                            },
                            onRelease = { view -> view.disposeCamera() },
                            modifier = Modifier.fillMaxWidth().height(360.dp)
                        )
                    }
                    Text(
                        tx("Point the camera at a provider QR code."),
                        color = AriMuted,
                        fontSize = 13.sp
                    )
                    cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AriMuted, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(cameraError ?: tx("Allow camera access to scan a provider QR code."), color = AriMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = AriAccent),
                            modifier = Modifier.testTag("provider-qr-camera-permission")
                        ) { Text(tx("Try again")) }
                    }
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AriAccent),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(tx("Close")) }
            }
        }
    }
}

private class QrPreviewView(context: Context) : FrameLayout(context) {
    @Volatile var onCodeFound: ((String) -> Unit)? = null
    @Volatile var onCameraError: ((String) -> Unit)? = null
    private val previewView = PreviewView(context)
    private var controller: QrCameraController? = null

    init {
        addView(previewView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun start(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        if (controller != null) return
        controller = QrCameraController(context, lifecycleOwner, previewView).also { camera ->
            camera.onCodeFound = { onCodeFound?.invoke(it) }
            camera.onCameraError = { onCameraError?.invoke(it) }
            camera.start()
        }
    }

    fun disposeCamera() {
        controller?.close()
        controller = null
    }
}

private class QrCameraController(
    private val context: Context,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    private val previewView: PreviewView
) : AutoCloseable {
    @Volatile var onCodeFound: ((String) -> Unit)? = null
    @Volatile var onCameraError: ((String) -> Unit)? = null

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val delivered = AtomicBoolean(false)
    @Volatile private var cameraProvider: ProcessCameraProvider? = null
    @Volatile private var closed = false

    fun start() {
        try {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                if (closed) return@addListener
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(analysisExecutor) { proxy ->
                        val mediaImage = proxy.image
                        if (mediaImage == null || delivered.get() || closed) {
                            proxy.close()
                        } else {
                            val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                            scanner.process(input)
                                .addOnSuccessListener { codes ->
                                    val value = codes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                                    if (value != null && delivered.compareAndSet(false, true)) {
                                        mainHandler.post { if (!closed) onCodeFound?.invoke(value) }
                                    }
                                }
                                .addOnCompleteListener { proxy.close() }
                        }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (error: Exception) {
                    postError(error)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (error: Exception) {
            postError(error)
        }
    }

    private fun postError(error: Exception) {
        mainHandler.post {
            if (!closed) onCameraError?.invoke(error.message?.take(240) ?: tx("Camera is unavailable."))
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        cameraProvider?.unbindAll()
        runCatching { scanner.close() }
        analysisExecutor.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
