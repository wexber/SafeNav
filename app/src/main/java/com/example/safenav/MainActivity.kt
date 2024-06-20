package com.example.safenav

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.widget.Toast
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    // Variable global para almacenar el nombre de usuario
    private var nombreUsuario: String = ""
    private var userId: Int = -1
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userId = verificarSesionIniciada()
        if (userId != -1) {
            // El usuario ya ha iniciado sesión, redirigir a la actividad principal
            startActivity(Intent(this, MenuActivity::class.java).apply {
                // También puedes pasar cualquier otro dato relevante a la actividad principal aquí
                putExtra("userId", userId)
            })
            finish() // Finalizar la actividad actual para que no se pueda volver atrás
        }
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.tv_go_to_menu)
        tvGoToRegister = findViewById(R.id.tv_go_to_register)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        val tvGoRegister = findViewById<TextView>(R.id.tv_go_to_register)
        tvGoRegister.setOnClickListener{
            goToRegister()
        }



    }
    private fun goToRegister() {
        val intent = Intent(this, RegistroActivity::class.java)
        startActivity(intent)
    }

    private fun loginUser(email: String, password: String) {
        Executors.newSingleThreadExecutor().execute {
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
                    val query = "SELECT * FROM Usuarios WHERE correo_electronico = '$email' AND contrasena = '$password'"
                    val resultSet: ResultSet = statement.executeQuery(query)

                    if (resultSet.next()) {
                        // Usuario autenticado, iniciar la actividad del menú principal
                        val nombreUsuario = resultSet.getString("nombre") // Asigna el nombre de usuario obtenido de la base de datos
                        val userId = obtenerIdUsuarioDesdeBaseDeDatos(email) // Obtener el ID del usuario de la base de datos
                        guardarIdUsuarioEnSesion(userId)
                        runOnUiThread {
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MenuActivity::class.java).apply {
                                putExtra("nombreUsuario", nombreUsuario)
                                putExtra("userId", userId)
                            })
                            finish() // Finalizar la actividad actual (MainActivity)
                        }
                    } else {
                        // Credenciales incorrectas
                        runOnUiThread {
                            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: Driver JDBC no encontrado", Toast.LENGTH_SHORT).show()
                }
            } finally {
                try {
                    connection?.close()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun verificarSesionIniciada(): Int {
        val sharedPreferences = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1) // Devuelve -1 si no se encuentra ningún ID de usuario guardado
    }

    private fun guardarIdUsuarioEnSesion(userId: Int) {
        val sharedPreferences = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("userId", userId)
        editor.apply()
    }


    private fun obtenerIdUsuarioDesdeBaseDeDatos(email: String): Int {
        var userId = -1 // Valor predeterminado en caso de que no se encuentre el usuario

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
                val query = "SELECT id_usuario FROM Usuarios WHERE correo_electronico = '$email'"
                val resultSet: ResultSet = statement.executeQuery(query)

                if (resultSet.next()) {
                    userId = resultSet.getInt("id_usuario")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // Manejo de errores: aquí podrías lanzar una excepción o manejar el error de otra manera según tu aplicación
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            // Manejo de errores: aquí podrías lanzar una excepción o manejar el error de otra manera según tu aplicación
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        return userId
    }
}


