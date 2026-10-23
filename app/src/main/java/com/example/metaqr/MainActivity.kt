package com.example.metaqr

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.meta.wearable.dat.core.types.PermissionStatus
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.display.*
import com.meta.wearable.dat.display.types.*
import com.meta.wearable.dat.display.views.*
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(Wearables.RequestPermissionContract()) { response ->
        status.text = "Autorisation caméra : ${response.getOrDefault(PermissionStatus.Denied)}"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var status: TextView
    private lateinit var result: TextView
    private var session: DeviceSession? = null
    private var camera: Camera? = null
    private var glassesDisplay: Display? = null
    private var displayJob: Job? = null
    private var showingResult = false
    private var displayReady = false
    private var streamJob: Job? = null
    private var stateJob: Job? = null
    private var busy = false
    private var lastCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(34, 50, 34, 30); setBackgroundColor(Color.rgb(247, 248, 250)) }
        val title = TextView(this).apply { text = "META QR SCANNER"; textSize = 25f; setTextColor(Color.BLACK) }
        status = TextView(this).apply { text = "Initialisation…"; textSize = 17f; setPadding(0, 28, 0, 28) }
        result = TextView(this).apply { text = "Aucun QR code détecté"; textSize = 19f; setTextIsSelectable(true); setPadding(0, 24, 0, 24) }
        layout.addView(title); layout.addView(status)
        fun action(label: String, block: () -> Unit) { layout.addView(Button(this).apply { text = label; setOnClickListener { block() } }) }
        action("1 · Connecter les lunettes") { Wearables.startRegistration(this) }
        action("2 · Autoriser la caméra") { permissionLauncher.launch(Permission.CAMERA) }
        action("3 · Ouvrir le scanner dans les lunettes") { startScanner() }
        action("Retour au scanner") { backToScanner() }
        action("Arrêter le scanner") { stopScanner() }
        action("Copier le dernier QR code") {
            lastCode?.let { (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("QR", it)) }
        }
        layout.addView(result)
        setContentView(ScrollView(this).apply { addView(layout) })
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 42)
        }
        Wearables.initialize(applicationContext).fold(onSuccess = { status.text = "SDK prêt. Connectez les lunettes." }, onFailure = { error, _ -> status.text = error.description })
        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); processIntent(intent) }
    private fun processIntent(intent: Intent?) { if (intent != null) Wearables.handleIntent(intent) { request -> request.continueRegistration(this) } }

    private fun startScanner() {
        stopScanner()
        showingResult = false
        lastCode = null
        Wearables.createSession(AutoDeviceSelector()).fold(
            onSuccess = { s ->
                session = s
                stateJob = scope.launch {
                    s.state.collect { state ->
                        status.text = "Session : $state"
                        if (state == DeviceSessionState.STARTED) {
                            if (glassesDisplay == null) attachDisplay(s)
                            if (camera == null) attachCamera(s)
                        }
                        if (state == DeviceSessionState.STOPPED) {
                            camera = null
                            glassesDisplay = null
                            displayReady = false
                        }
                    }
                }
                s.start()
            },
            onFailure = { error, _ -> status.text = "Session impossible : ${error.description}" }
        )
    }

    private fun attachDisplay(s: DeviceSession) {
        s.addDisplay(DisplayConfiguration()).fold(
            onSuccess = { display ->
                glassesDisplay = display
                displayJob?.cancel()
                displayJob = scope.launch {
                    display.state.collect { state ->
                        displayReady = state == DisplayState.STARTED
                        if (displayReady) renderGlasses()
                    }
                }
            },
            onFailure = { error, _ -> status.text = "Affichage lunettes : ${error.description}" }
        )
    }

    private fun renderGlasses() {
        scope.launch { renderGlassesContent() }
    }

    private suspend fun renderGlassesContent() {
        val display = glassesDisplay ?: return
        if (!displayReady) return
        val code = lastCode
        if (!showingResult || code == null) {
            display.sendContent {
                flexBox(direction = Direction.COLUMN, gap = 16, paddingTop = 20, paddingBottom = 20, paddingStart = 20, paddingEnd = 20) {
                    text("QR SCANNER", style = TextStyle.HEADING)
                    text("Regardez un QR code avec vos lunettes.", style = TextStyle.BODY)
                    text("Recherche en cours...", style = TextStyle.META)
                }
            }.onFailure { error, _ -> status.text = "Écran : ${error.description}" }
            return
        }
        val imageUrl = QrContentClassifier.directImageUrl(code)
        val isLink = QrContentClassifier.isWebUrl(code)
        display.sendContent {
            flexBox(direction = Direction.COLUMN, gap = 12, paddingTop = 20, paddingBottom = 20, paddingStart = 20, paddingEnd = 20) {
                text("QR DÉTECTÉ", style = TextStyle.HEADING)
                text(if (imageUrl != null) "Image" else if (isLink) "Lien web" else "Contenu", style = TextStyle.META)
                if (imageUrl != null) {
                    image(uri = imageUrl, sizePreset = ImageSize.FILL)
                }
                // All formats are preserved as text, including URLs and non-displayable payloads.
                text(code.take(1500), style = TextStyle.BODY)
                if (code.length > 1500) text("Contenu complet disponible sur le téléphone.", style = TextStyle.META)
                button(label = "Retour au scanner", style = ButtonStyle.PRIMARY, onClick = { runOnUiThread { backToScanner() } })
            }
        }.onFailure { error, _ -> status.text = "Écran : ${error.description}" }
    }

    private fun backToScanner() {
        showingResult = false
        lastCode = null
        result.text = "Aucun QR code détecté"
        status.text = "Recherche QR code..."
        renderGlasses()
    }

    private fun attachCamera(s: DeviceSession) {
        s.addCamera(StreamConfiguration(videoQuality = VideoQuality.MEDIUM, frameRate = 15, compressVideo = false)).fold(
            onSuccess = { c ->
                camera = c
                streamJob = scope.launch {
                    c.stream.videoStream.collect { frame ->
                        if (frame.isCompressed || busy || showingResult) return@collect
                        busy = true
                        try {
                            val decoded = withContext(Dispatchers.Default) { QrFrameDecoder.scan(frame.buffer, frame.width, frame.height) }
                            if (!decoded.isNullOrBlank() && !showingResult) {
                                showingResult = true
                                lastCode = decoded
                                result.text = decoded
                                status.text = "QR code détecté"
                                renderGlasses()
                            }
                        } catch (e: Exception) { status.text = "Décodage : ${e.message}" }
                        finally { busy = false }
                    }
                }
                c.stream.start().onFailure { error, _ -> status.text = "Caméra : ${error.description}" }
            },
            onFailure = { error, _ -> status.text = "Caméra inaccessible : ${error.description}" }
        )
    }

    private fun stopScanner() {
        displayJob?.cancel(); displayJob = null
        displayReady = false
        glassesDisplay?.stop(); glassesDisplay = null
        streamJob?.cancel(); streamJob = null
        stateJob?.cancel(); stateJob = null
        camera?.stop(); camera = null
        session?.stop(); session = null
        busy = false
    }
    override fun onDestroy() { stopScanner(); scope.cancel(); super.onDestroy() }
}
