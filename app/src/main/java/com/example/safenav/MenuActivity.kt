package com.example.safenav

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


class MenuActivity : AppCompatActivity() {

    companion object {
        lateinit var textViewUsername: TextView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        textViewUsername = findViewById(R.id.tv_nombre_usuario_dynamic)

        // Obtener el ID de usuario guardado después de iniciar sesión
        val userId = obtenerIdUsuarioEnSesion()

        // Iniciar AsyncTask para obtener el nombre de usuario
        ObtenerNombreUsuarioTask().execute(userId)




        val btnUbicacion = findViewById<Button>(R.id.Ubicacion)
        btnUbicacion.setOnClickListener {
            val intent = Intent(this, Mapa::class.java)
            startActivity(intent)
        }



        val btnCamera = findViewById<Button>(R.id.DeteccionSemaforos)
        btnCamera.setOnClickListener {
            val intent = Intent(this, Camara_Deteccion2::class.java)
            startActivity(intent)
        }

        val btnHistorialUbicacion = findViewById<Button>(R.id.HistorialCalles)
        btnHistorialUbicacion.setOnClickListener {
            val intent = Intent(this, RecentLocationsActivity::class.java)
            startActivity(intent)
        }

        val btnAjustes = findViewById<Button>(R.id.BtnAjustes)
        btnAjustes.setOnClickListener {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()
        val userId = obtenerIdUsuarioEnSesion()
        ObtenerNombreUsuarioTask().execute(userId)
    }


    // Método para obtener el ID de usuario guardado en la sesión
    private fun obtenerIdUsuarioEnSesion(): Int {
        val sharedPreferences = getSharedPreferences("sesion", MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1)
    }

    private fun goToMenu() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

    // AsyncTask para obtener el nombre de usuario de la base de datos
    private inner class ObtenerNombreUsuarioTask : AsyncTask<Int, Void, String>() {
        override fun doInBackground(vararg params: Int?): String {
            val userId = params[0] ?: -1
            var nombreUsuario = ""
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

                // Establecer la conexión a la base de datos
                connection = DriverManager.getConnection(azureUrl)

                // Preparar la consulta parametrizada
                val query = "SELECT nombre FROM Usuarios WHERE id_usuario = ?"
                val preparedStatement = connection.prepareStatement(query)
                preparedStatement.setInt(1, userId)

                // Ejecutar la consulta
                val resultSet = preparedStatement.executeQuery()

                // Procesar el resultado
                if (resultSet.next()) {
                    nombreUsuario = resultSet.getString("nombre")
                }
            } catch (e: SQLException) {
                // Manejar la excepción (por ejemplo, mostrar un mensaje de error)
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                // Manejar la excepción (por ejemplo, mostrar un mensaje de error)
                e.printStackTrace()
            } finally {
                // Cerrar la conexión de forma segura
                try {
                    connection?.close()
                } catch (e: SQLException) {
                    // Manejar la excepción (por ejemplo, mostrar un mensaje de error)
                    e.printStackTrace()
                }
            }

            return nombreUsuario
        }

        override fun onPostExecute(result: String) {
            // Actualizar el TextView con el nombre de usuario obtenido
            val tvNombreUsuario = findViewById<TextView>(R.id.tv_nombre_usuario_dynamic)
            tvNombreUsuario.text = result
        }
    }



}