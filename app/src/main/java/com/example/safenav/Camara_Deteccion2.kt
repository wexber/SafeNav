package com.example.safenav

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.Toast
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camara_Deteccion2 : AppCompatActivity(), Detector.DetectorListener,TextToSpeech.OnInitListener,Detector.InterpreterListener {

    private lateinit var binding: ActivityCamaraDeteccionBinding
    private val isFrontCamera = false
    private var interpreterCompleted = false
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

        /*

        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenu)
        btnVolverMenu.setOnClickListener {
            // Iniciar una coroutine para ejecutar la función apagarCamara() de manera asíncrona
            CoroutineScope(Dispatchers.Main).launch {
                // Liberar los recursos de TensorFlow Lite
                detector.clear()
                // Esperar a que se complete la ejecución de la función apagarCamara()
                apagarCamara()
                // Llamar a las otras funciones después de que apagarCamara() haya terminado
                releaseResources()
                navigateToMenu()
            }
        }

         */

        detector.registerInterpreterListener(object : Detector.InterpreterListener {
            override fun onInterpreterError(error: String) {
                Log.e("Camera_Deteccion2", "Error del intérprete: $error")
                navigateToMenu()
            }

            override fun onInterpreterCompleted() {
                // Manejar cuando el intérprete completa su ejecución
            }

            override fun onInterpreterRunning() {
                // Manejar cuando el intérprete está en ejecución
            }

            override fun onInterpreterStart() {
                // Manejar cuando el intérprete inicia su ejecución
            }
        })

        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenu)
        btnVolverMenu.setOnClickListener {
            // Detener el intérprete si está en curso
            if (detector.isDetectionInProgress()) {
                detector.stopInterpreter()
            }
            // Iniciar una coroutine para ejecutar la función apagarCamara() de manera asíncrona
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Liberar los recursos de TensorFlow Lite
                    detector.clear()
                    // Esperar a que se complete la ejecución de la función apagarCamara()
                    apagarCamara()
                    // Llamar a las otras funciones después de que apagarCamara() haya terminado
                    releaseResources()
                    navigateToMenu()
                } catch (e: Exception) {
                    Log.e("Camera_Deteccion2", "Error al detener la detección: ${e.message}", e)
                    navigateToMenu()
                }
            }
        }



        // Configurar el botón para apagar la cámara
        val btnApagarCamara = findViewById<Button>(R.id.btnApagarCamara)
        btnApagarCamara.setOnClickListener {
            apagarCamara()
        }


    }


    // Configura un listener para manejar los errores de la detección



    override fun onInterpreterCompleted() {
        // Realizar acciones necesarias cuando el intérprete ha completado su ejecución
        interpreterCompleted = true
    }

    // Implementación de los métodos de InterpreterListener
    override fun onInterpreterRunning() {
        // Realizar acciones necesarias cuando el intérprete está en ejecución
    }


    override fun onInterpreterStart() {
        // Realizar acciones necesarias cuando el intérprete se inicia
    }

    override fun onInterpreterError(error: String) {
        // Realizar acciones necesarias en caso de error en el intérprete
    }



    private fun apagarCamara() {
        try {
            Log.d(TAG, "Iniciando apagado de cámara...")
            isAnalyzing = false // Desactivar el análisis de imágenes

            // Detener el análisis de imágenes
            imageAnalyzer?.let {
                Log.d(TAG, "Deteniendo el análisis de imágenes...")
                it.clearAnalyzer()
                imageAnalyzer = null
            }

            // Detener TextToSpeech
            if (::tts.isInitialized) {
                try {
                    Log.d(TAG, "Deteniendo TextToSpeech...")
                    tts.stop()
                    tts.shutdown()
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "TextToSpeech no está vinculado: ${e.message}")
                }
            }

            // Detener y liberar la cámara
            cameraProvider?.let {
                Log.d(TAG, "Deteniendo y liberando la cámara...")
                it.unbindAll()
            }

            // Apagar el executor de la cámara
            if (!cameraExecutor.isShutdown) {
                Log.d(TAG, "Apagando el executor de la cámara...")
                cameraExecutor.shutdown()
            }

            // Limpiar el detector
            detector.clear()

            Log.d(TAG, "Cámara apagada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al apagar la cámara: ${e.message}", e)
        }
    }

    private fun stopImageAnalysis() {
        isAnalyzing = false // Detener el análisis de imágenes
        imageAnalyzer?.clearAnalyzer() // Limpiar el analizador de imágenes
    }
    private fun releaseResources() {
        apagarCamara()
        detector.clear()
        cameraExecutor.shutdown()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        cameraProvider?.unbindAll() // Liberar todos los casos de uso de la cámara
    }

    private fun navigateToMenu() {
        try {
            val intent = Intent(this, MenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish() // Finaliza la actividad actual
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar la actividad del menú", e)
        }
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

    override fun onDestroy() {
        stopImageAnalysis() // Detener el análisis de imágenes
        releaseResources() // Liberar los recursos
        super.onDestroy()
        detector.clear()
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
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
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