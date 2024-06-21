package com.example.safenav

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.safenav.ui.theme.Constants.LABELS_PATH
import com.example.safenav.ui.theme.Constants.MODEL_PATH
import com.example.safenav.R
import com.example.safenav.databinding.ActivityCamaraDeteccionBinding
import com.example.safenav.ui.theme.BoundingBox
import com.example.safenav.ui.theme.Detector
import com.example.safenav.ui.theme.OverlayView
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camara_Deteccion2 : AppCompatActivity(), Detector.DetectorListener,TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityCamaraDeteccionBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    private lateinit var tts: TextToSpeech
    private lateinit var cameraExecutor: ExecutorService
    private var lastSpeakTime = 0L
    private val speakInterval = 5000L // Tiempo mínimo entre pronunciaciones en milisegundos
    private var isAnalyzing = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCamaraDeteccionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)
        val overlayView = findViewById<OverlayView>(R.id.overlay)
        overlayView.setMainActivity(this)
        detector = Detector(
            context = this,
            modelPath = "model.tflite",
            labelPath = "labels.txt",
            detectorListener = this
        )
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Configurar el botón para volver al menú
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenu)
        btnVolverMenu.setOnClickListener {
            isAnalyzing = false // Desactivar el análisis de imágenes
            stopImageAnalysis()
            releaseResources()

            val intent = Intent(this, MenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

    }


    private fun stopImageAnalysis() {
        isAnalyzing = false // Detener el análisis de imágenes
        imageAnalyzer?.clearAnalyzer() // Limpiar el analizador de imágenes
    }
    private fun releaseResources() {
        detector.clear()
        cameraExecutor.shutdown()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        cameraProvider?.unbindAll() // Liberar todos los casos de uso de la cámara
    }
    override fun onInit(status: Int) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                // Verificar la disponibilidad del idioma
                val result = tts?.isLanguageAvailable(Locale.getDefault())
                if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                    // El idioma no está soportado o faltan datos
                    Log.e("TTS", "El idioma no está soportado o faltan datos")
                } else {
                    // TextToSpeech se ha inicializado correctamente y el idioma está disponible
                    // Configurar otras opciones si es necesario
                }
            } else {
                // La inicialización de TextToSpeech falló
                // Aquí puedes manejar el error, mostrar un mensaje de error, o intentar inicializarlo nuevamente, etc.
            }
        } catch (e: Exception) {
            // Si ocurre alguna excepción, manejarla aquí
            e.printStackTrace()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
    }
/*
    override fun onDestroy() {
        detector.clear()
        cameraExecutor.shutdown()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        cameraProvider?.unbindAll()// Liberar todos los casos de uso de la cámara
        super.onDestroy()
    }

 */
override fun onDestroy() {
    super.onDestroy()
    stopImageAnalysis() // Detener el análisis de imágenes
    releaseResources() // Liberar los recursos
}

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            // Limpiar la vista de superposición
            binding.overlay.clear()
            // Actualizar la vista de tiempo de inferencia
            binding.inferenceTime.text = "no se ha detectado semaforo"
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.overlay.clear()
            binding.overlay.setResults(boundingBoxes)

            binding.inferenceTime.text = "${inferenceTime}ms"
        }
    }


    fun speak(objectName: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSpeakTime >= speakInterval) {
            tts.speak("Semaforo: $objectName", TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpeakTime = currentTime
        }
    }

    override fun onBackPressed() {
        // No hacemos nada para desactivar el botón de retroceso
    }

}