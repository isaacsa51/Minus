package com.serranoie.app.minus.presentation.ui.editor.receipt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.googlecode.tesseract.android.TessBaseAPI
import com.serranoie.app.minus.domain.receipt.ReceiptParser
import com.serranoie.app.minus.domain.receipt.ReceiptScan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import logcat.asLog
import logcat.logcat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val TAG = "ISAAC:ReceiptScanner"

class ReceiptScannerState(
    val isOpen: Boolean,
    val isScanning: Boolean,
    val camera: ReceiptCameraUiState,
    val launch: () -> Unit,
    val close: () -> Unit,
    val capture: () -> Unit,
    val preview: @Composable (Modifier) -> Unit,
)

@Composable
fun rememberReceiptScanner(
    onScanned: (ReceiptScan) -> Unit,
    onFailed: () -> Unit,
): ReceiptScannerState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnFailed by rememberUpdatedState(onFailed)
    var isOpen by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(ReceiptCameraUiState()) }
    var session by remember { mutableStateOf<ReceiptCameraSession?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isScanning = true
        scope.launch {
            val scan = runCatching { ReceiptParser.parse(ReceiptOcr.read(context, uri)) }
                .onFailure { logcat(TAG) { "Receipt scan failed\n${it.asLog()}" } }
                .getOrNull()
            isScanning = false
            if (scan != null) currentOnScanned(scan) else currentOnFailed()
        }
    }

    fun openCamera() {
        camera = ReceiptCameraUiState()
        isOpen = true
    }

    fun openPicker() {
        runCatching {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.onFailure {
            logcat(TAG) { "Could not open photo picker\n${it.asLog()}" }
            currentOnFailed()
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else openPicker()
    }

    return ReceiptScannerState(
        isOpen = isOpen,
        isScanning = isScanning,
        camera = camera,
        launch = {
            if (isOpen || isScanning) return@ReceiptScannerState
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) openCamera() else permission.launch(Manifest.permission.CAMERA)
        },
        close = { isOpen = false },
        capture = capture@{
            val active = session ?: return@capture
            if (camera.isCapturing) return@capture
            camera = camera.capturing()
            scope.launch {
                val (next, result) = camera.captured(active.capture())
                camera = next
                if (result != null && isOpen) {
                    isOpen = false
                    currentOnScanned(result)
                }
            }
        },
        preview = { modifier ->
            ReceiptCameraPreview(
                modifier = modifier,
                onSession = { session = it },
                onFrame = { scan -> camera = camera.withFrame(scan) },
                onUnavailable = {
                    isOpen = false
                    currentOnFailed()
                },
            )
        },
    )
}

@Composable
private fun ReceiptCameraPreview(
    modifier: Modifier,
    onSession: (ReceiptCameraSession?) -> Unit,
    onFrame: (ReceiptScan?) -> Unit,
    onUnavailable: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val session = remember {
        ReceiptCameraSession(context) { scan ->
            ContextCompat.getMainExecutor(context).execute { currentOnFrame(scan) }
        }
    }

    DisposableEffect(session) {
        onSession(session)
        onDispose {
            onSession(null)
            session.close()
        }
    }

    LaunchedEffect(session) {
        runCatching {
            val provider = suspendCancellableCoroutine { continuation ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener(
                    { continuation.resumeWith(runCatching { future.get() }) },
                    ContextCompat.getMainExecutor(context),
                )
            }
            val preview = Preview.Builder()
                .setResolutionSelector(session.previewResolution)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                session.analysis,
                session.imageCapture,
            )
            session.provider = provider
        }.onFailure {
            logcat(TAG) { "Could not start camera\n${it.asLog()}" }
            onUnavailable()
        }
    }

    Box(
        modifier = modifier.onSizeChanged { size ->
            session.viewWidth = size.width
            session.viewHeight = size.height
        },
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        CameraGestureLayer(
            camera = camera,
            previewView = previewView,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CameraGestureLayer(
    camera: Camera?,
    previewView: PreviewView,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusRing = remember { Animatable(0f) }
    var zoomRatio by remember(camera) {
        mutableFloatStateOf(camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f)
    }
    val ringColor = Color.White

    Canvas(
        modifier = modifier
            .pointerInput(camera) {
                val active = camera ?: return@pointerInput
                detectTapGestures { offset ->
                    val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                    active.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                            point,
                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                        ).setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS).build(),
                    )
                    focusPoint = offset
                    scope.launch {
                        focusRing.snapTo(0f)
                        focusRing.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
                        focusPoint = null
                    }
                }
            }
            .pointerInput(camera) {
                val active = camera ?: return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    val state = active.cameraInfo.zoomState.value ?: return@detectTransformGestures
                    zoomRatio = (zoomRatio * zoom).coerceIn(state.minZoomRatio, state.maxZoomRatio)
                    active.cameraControl.setZoomRatio(zoomRatio)
                }
            },
    ) {
        val point = focusPoint ?: return@Canvas
        val progress = focusRing.value
        val radius = FocusRingRadius.toPx() * (1.35f - 0.35f * progress.coerceAtMost(0.4f) / 0.4f)
        val alpha = if (progress < 0.7f) 1f else 1f - (progress - 0.7f) / 0.3f
        drawCircle(
            color = ringColor.copy(alpha = alpha),
            radius = radius,
            center = point,
            style = Stroke(width = FocusRingStroke.toPx()),
        )
    }
}

private const val FOCUS_AUTO_CANCEL_SECONDS = 3L
private val FocusRingRadius = 36.dp
private val FocusRingStroke = 2.dp

private class ReceiptCameraSession(
    private val context: Context,
    private val onFrame: (ReceiptScan?) -> Unit,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val engine = ReceiptOcrEngine(context)
    private var lastFrameAt = 0L
    var provider: ProcessCameraProvider? = null

    @Volatile
    var viewWidth = 0

    @Volatile
    var viewHeight = 0

    val analysis: ImageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setResolutionSelector(resolution(Size(1280, 960)))
        .build()
        .also { it.setAnalyzer(executor, ::analyze) }

    val previewResolution: ResolutionSelector = resolution(Size(1920, 1440))

    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(resolution(Size(2560, 1920)))
        .build()

    private fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastFrameAt < FRAME_INTERVAL_MS) {
            image.close()
            return
        }
        val bitmap = image.use { it.toUprightBitmap() }
        val scan = runCatching { ReceiptParser.parse(engine.read(bitmap)) }
            .onFailure { logcat(TAG) { "Frame OCR failed\n${it.asLog()}" } }
            .getOrNull()
        lastFrameAt = System.currentTimeMillis()
        onFrame(scan)
    }

    suspend fun capture(): ReceiptScan? = suspendCancellableCoroutine { continuation ->
        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.use { it.toUprightBitmap() }
                    val scan = runCatching { ReceiptParser.parse(engine.read(bitmap)) }
                        .onFailure { logcat(TAG) { "Capture OCR failed\n${it.asLog()}" } }
                        .getOrNull()
                    continuation.resume(scan)
                }

                override fun onError(exception: ImageCaptureException) {
                    logcat(TAG) { "Capture failed\n${exception.asLog()}" }
                    continuation.resume(null)
                }
            },
        )
    }

    fun close() {
        analysis.clearAnalyzer()
        provider?.unbindAll()
        executor.execute { engine.close() }
        executor.shutdown()
    }

    private fun resolution(size: Size) = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(size, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
        )
        .build()

    private fun ImageProxy.toUprightBitmap(): Bitmap {
        val upright = toBitmap().rotated(imageInfo.rotationDegrees)
        val crop = receiptFrameCrop(upright.width, upright.height, viewWidth, viewHeight)
            ?: return ReceiptOcr.downscale(upright)
        val framed = Bitmap.createBitmap(upright, crop.left, crop.top, crop.width, crop.height)
        if (framed != upright) upright.recycle()
        return ReceiptOcr.downscale(framed)
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 1500L
    }
}

private class ReceiptOcrEngine(private val context: Context) {
    private var tess: TessBaseAPI? = null

    fun read(bitmap: Bitmap): String {
        val api = tess ?: TessBaseAPI().also { api ->
            check(api.init(ReceiptOcr.installTessdata(context), ReceiptOcr.LANGUAGES)) { "Tesseract init failed" }
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN
            tess = api
        }
        return try {
            api.setImage(bitmap)
            api.getUTF8Text().orEmpty().also { text ->
                logcat(TAG) { "OCR ${bitmap.width}x${bitmap.height} read:\n$text\n-> ${ReceiptParser.parse(text)}" }
            }
        } finally {
            api.clear()
            bitmap.recycle()
        }
    }

    fun close() {
        tess?.recycle()
        tess = null
    }
}

object ReceiptOcr {
    const val LANGUAGES = "spa+eng"
    private const val MAX_SIDE_PX = 2400
    private val TESSDATA_FILES = listOf("eng.traineddata", "spa.traineddata")

    suspend fun read(context: Context, uri: Uri): String = withContext(Dispatchers.Default) {
        val bitmap = decodeUpright(context, uri) ?: return@withContext ""
        val engine = ReceiptOcrEngine(context)
        try {
            engine.read(bitmap)
        } finally {
            engine.close()
        }
    }

    fun installTessdata(context: Context): String {
        val root = File(context.filesDir, "tesseract")
        val tessdata = File(root, "tessdata").apply { mkdirs() }
        TESSDATA_FILES.forEach { name ->
            val target = File(tessdata, name)
            if (target.exists()) return@forEach
            val partial = File(tessdata, "$name.part")
            context.assets.open("tessdata/$name").use { input ->
                partial.outputStream().use { input.copyTo(it) }
            }
            check(partial.renameTo(target)) { "Could not install $name" }
        }
        return root.absolutePath
    }

    fun downscale(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_SIDE_PX) return bitmap
        val scale = MAX_SIDE_PX.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true,
        )
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun decodeUpright(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val degrees = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        return bitmap.rotated(degrees)
    }

    private val ExifInterface.rotationDegrees: Int
        get() = when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val rotated = Bitmap.createBitmap(
        this, 0, 0, width, height,
        Matrix().apply { postRotate(degrees.toFloat()) }, true,
    )
    if (rotated != this) recycle()
    return rotated
}
