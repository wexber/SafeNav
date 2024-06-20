package com.example.safenav

import android.content.Context
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.safenav.ui.theme.RecentLocation
import com.example.safenav.ui.theme.RecentLocationsAdapter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

class RecentLocationsActivity : AppCompatActivity() {


    private val TAG = "RecentLocationsActivity"
    private lateinit var recentLocationsListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent_locations)

        recentLocationsListView = findViewById(R.id.recent_locations_list)
        val userId = obtenerIdUsuarioEnSesion()
        if (userId != -1) {
            fetchRecentLocations(userId)
        } else {
            Toast.makeText(this, "No se pudo obtener el ID del usuario", Toast.LENGTH_SHORT).show()
        }

    }
    private fun fetchRecentLocations(userId: Int) {
        Thread {
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
                    val query =
                        "SELECT TOP 7 Latitud, Longitud, Nombre_Calle, Fecha_Hora FROM Ubicacion WHERE id_usuario = $userId ORDER BY Fecha_Hora DESC"
                    val resultSet: ResultSet = statement.executeQuery(query)

                    val recentLocations = mutableListOf<RecentLocation>()

                    while (resultSet.next()) {
                        val latitude = resultSet.getDouble("Latitud")
                        val longitude = resultSet.getDouble("Longitud")
                        val street = resultSet.getString("Nombre_Calle")
                        val timestamp = resultSet.getTimestamp("Fecha_Hora")
                        val timestampString = timestamp.toString() // Convertir Timestamp a String
                        recentLocations.add(
                            RecentLocation(
                                latitude,
                                longitude,
                                street,
                                timestampString
                            )
                        )
                    }

                    runOnUiThread {
                        val adapter =
                            RecentLocationsAdapter(this@RecentLocationsActivity, recentLocations)
                        recentLocationsListView.adapter = adapter
                    }
                }
            } catch (e: SQLException) {
                Log.e(TAG, "Error fetching recent locations from database", e)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error fetching recent locations from database",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "JDBC Driver not found", e)
                runOnUiThread {
                    Toast.makeText(this, "Error: JDBC Driver not found", Toast.LENGTH_SHORT)
                        .show()
                }
            } finally {
                try {
                    connection?.close()
                } catch (e: SQLException) {
                    Log.e(TAG, "Error closing connection", e)
                }
            }
        }.start()
    }
    private fun obtenerIdUsuarioEnSesion(): Int {
        val sharedPreferences = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1)
    }
}

