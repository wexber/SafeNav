package com.example.safenav

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import android.Manifest
import android.content.Context
import android.location.Location
import android.widget.Toast
import android.location.Criteria
import android.location.LocationManager
import android.util.Log
import java.io.IOException
import android.location.Geocoder
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.libraries.places.api.Places
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import java.util.Timer
import java.util.TimerTask
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.Executors

class Mapa : FragmentActivity(),OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,GoogleMap.OnMyLocationClickListener {

    private lateinit var map: GoogleMap
    private val TAG = "MainActivity"
    private var timer: Timer? = null
    private lateinit var tts: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        const val Request_Code_location = 0
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val INTERVAL_TIME = 10000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapa)

        // Inicializa el TextToSpeech
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                // Configura el idioma para la voz
                tts.language = Locale.getDefault()
            }
        }

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBOad-LsjPBURFajGqGkReCQIoy9Y9aBMU")
        }
        // Solicitar permiso de ubicación si aún no se ha concedido
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si los permisos no están concedidos, solicitarlos al usuario
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            val userId = obtenerIdUsuarioEnSesion()
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider: String? = locationManager.getBestProvider(Criteria(), true)
            val location: Location? = provider?.let { locationManager.getLastKnownLocation(it) }
            //obtenerLugaresCercanos()
            //createFragment()// Asegúrate de crear el fragmento del mapa si no se ha creado.
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val street = addresses?.get(0)?.thoroughfare // Nombre de la calle
                if (userId != -1 && street != null) {
                    saveLocationToDatabase(latitude, longitude, street, userId)
                    obtenerLugaresCercanos()
                    createFragment()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo obtener el ID del usuario o la dirección",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }




    }

    // Método para inicializar y utilizar la API de Places
    private fun inicializarPlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBOad-LsjPBURFajGqGkReCQIoy9Y9aBMU")
        }
        // Solicitar la ubicación actual y lugares cercanos
        obtenerLugaresCercanos()
    }


    private fun startLocationUpdates() {
        // Crea un nuevo Timer solo si no hay uno activo
        if (timer == null) {
            timer = Timer()
            timer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    // Lógica del Timer...
                }
            }, 0, INTERVAL_TIME)
        }
    }

    // Método para detener la actualización periódica de la dirección
    private fun stopLocationUpdates() {
        // Detiene el Timer solo si está en uso
        timer?.cancel()
        // Establece el Timer a null para indicar que ha sido detenido
        timer = null
    }

    override fun onResume() {
        super.onResume()
        // Inicia el Timer solo si es nulo o ha sido cancelado previamente
        if (timer == null) {
            startLocationUpdates()
            startLocationSpeech()
        }
    }

    private fun startLocationSpeech() {
        // Programa la lectura de la ubicación cada cierto tiempo
        handler.postDelayed(locationSpeechRunnable, INTERVAL_TIME)
    }


    private val locationSpeechRunnable = object : Runnable {
        override fun run() {
            // Obtiene la ubicación actual y la lee con TextToSpeech
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider: String? = locationManager.getBestProvider(Criteria(), true)
            val location: Location? = provider?.let { locationManager.getLastKnownLocation(it) }
            location?.let {
                getAddressFromLocation(it.latitude, it.longitude)
            }

            // Programa la próxima lectura de la ubicación después de un cierto intervalo de tiempo
            handler.postDelayed(this, INTERVAL_TIME)
        }
    }

    private fun obtenerIdUsuarioEnSesion(): Int {
        val sharedPreferences = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1)
    }
    // Dentro de la función saveLocationToDatabase, modifica el parámetro userId para que sea opcional y predeterminado al valor guardado al iniciar sesión
    private fun saveLocationToDatabase(latitude: Double, longitude: Double, street: String?, userId: Int) {
        executor.execute {
            var connection: Connection? = null
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")

                val azureUrl = "jdbc:jtds:sqlserver://safenav2.database.windows.net:1433;" +
                        "databaseName=safenav;" +
                        "user=adminsql@safenav2;" +
                        "password=Al3xander#;" +
                        "encrypt=true;" +
                        "trustServerCertificate=false;" +
                        "hostNameInCertificate=*.database.windows.net;" +
                        "loginTimeout=30;" +
                        "ssl=TLSv1.2;"

                connection = DriverManager.getConnection(azureUrl)

                synchronized(this) {
                    val statement = connection.createStatement()
                    val query = "INSERT INTO Ubicacion (Id_Usuario, Latitud, Longitud, Fecha_Hora, Nombre_Calle) VALUES ($userId, $latitude, $longitude, GETDATE(), '$street')"
                    statement.executeUpdate(query)

                    // Limpiar registros antiguos
                    val cleanQuery = "DELETE FROM Ubicacion WHERE Fecha_Hora < DATEADD(day, -7, GETDATE())"
                    statement.executeUpdate(cleanQuery)

                    runOnUiThread {
                        Toast.makeText(this, "Ubicación guardada", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SQLException) {
                Log.e(TAG, "Error al guardar la ubicación en la base de datos", e)
                runOnUiThread {
                    Toast.makeText(this, "Error al guardar la ubicación en la base de datos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "Controlador JDBC no encontrado", e)
                runOnUiThread {
                    Toast.makeText(this, "Error: Controlador JDBC no encontrado", Toast.LENGTH_SHORT).show()
                }
            } finally {
                try {
                    connection?.close()
                } catch (e: SQLException) {
                    Log.e(TAG, "Error closing connection", e)
                }
            }
        }
    }


    override fun onPause() {
        super.onPause()
        // Detiene el Timer si está en uso
        stopLocationUpdates()
        stopLocationSpeech() // Detiene la lectura de la ubicación
    }

    private fun stopLocationSpeech() {
        // Detiene la lectura de la ubicación
        handler.removeCallbacks(locationSpeechRunnable)
    }

    // Método para obtener la dirección a partir de las coordenadas de ubicación
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare // Nombre de la calle

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak("Estás en: $street", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    @Suppress("DEPRECATION")
                    tts.speak("Estás en: $street", TextToSpeech.QUEUE_FLUSH, null)
                }

                // Obtener el ID del usuario en sesión
                val userId = obtenerIdUsuarioEnSesion()

                // Guardar la ubicación y el nombre de la calle en la base de datos
                saveLocationToDatabase(latitude, longitude, street, userId)
            } else {
                Toast.makeText(this, "No se pudo obtener la dirección.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error al obtener la dirección: ${e.message}")
            Toast.makeText(this, "Error al obtener la dirección.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerLugaresCercanos() {
        // Crear el cliente de Places
        val placesClient = Places.createClient(this)
        // Definir los campos de lugar que deseas obtener
        val placeFields = listOf(Place.Field.NAME)
        // Obtener la ubicación actual y lugares cercanos
        placesClient.findCurrentPlace(FindCurrentPlaceRequest.newInstance(placeFields))
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val places = task.result?.placeLikelihoods
                    if (places != null) {
                        for (placeLikelihood in places) {
                            val place = placeLikelihood.place
                            Log.i(
                                TAG,
                                "Nombre del lugar: ${place.name}, Probabilidad: ${placeLikelihood.likelihood}"
                            )
                        }
                    } else {
                        Log.e(TAG, "Error al obtener la ubicación actual.")
                    }
                }
            }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el usuario concede los permisos, inicializar y utilizar la API de Places
                inicializarPlaces()
                if (::map.isInitialized) {
                    createMarker()
                } else {
                    createFragment()
                }
            } else {
                // Si el usuario niega los permisos, mostrar un mensaje o tomar otra acción
                Log.e(TAG, "Permiso de ubicación denegado.")
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createFragment() {
        val mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        createMarker()
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        Enablelocation()
    }


    //Metodo para obtener la ubicacion en el mapa En tiempo real
    private fun createMarker() {
        if (isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val criteria = Criteria()
            val provider: String? = locationManager.getBestProvider(criteria, true)
            val location: Location? = provider?.let { locationManager.getLastKnownLocation(it) }
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f), 3000, null)
                // Llamar a la función para obtener la dirección desde la ubicación
                getAddressFromLocation(it.latitude, it.longitude)
            }
        } else {
            requestlocationPermission()
        }
    }

    private fun isLocationPermissionGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED


    private fun Enablelocation() {
        if (!::map.isInitialized) return
        if (isLocationPermissionGranted()) {
            //permiso activado
            map.isMyLocationEnabled = true
            map.setOnMyLocationButtonClickListener(this)
            map.setOnMyLocationClickListener(this)
        } else {
            //no activo los permisos
            requestlocationPermission()

        }
    }


    private fun requestlocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(this, "ve ajustes y activa los permisos", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Request_Code_location
            )
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return
        if (isLocationPermissionGranted()) {
            map.isMyLocationEnabled = false

            Toast.makeText(
                this,
                "Para activar la localización ve ajuste y acepta los permisos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onMyLocationButtonClick(): Boolean {
        return false
    }


    override fun onMyLocationClick(p0: Location) {
        val latitude = p0.latitude
        val longitude = p0.longitude
        getAddressFromLocation(latitude, longitude)
        Toast.makeText(
            this,
            "Ubicación guardada en la base de datos: ($latitude, $longitude)",
            Toast.LENGTH_SHORT
        ).show()
    }


    override fun onDestroy() {
        super.onDestroy()
        // Detener la actualización de la ubicación cuando la actividad se destruye
        stopLocationUpdates()
        executor.shutdown()

    }
}