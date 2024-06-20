package com.example.safenav

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


class RegistroActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etRepPassword: EditText
    private lateinit var btnRegistrar: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        etNombre = findViewById(R.id.et_nombre)
        etApellido = findViewById(R.id.et_apellido)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etRepPassword = findViewById(R.id.et_rep_password)
        btnRegistrar = findViewById(R.id.buttonRegister)

        val tvGoLogin = findViewById<TextView>(R.id.tv_go_to_login)
        tvGoLogin.setOnClickListener{
            goToLogin()
        }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val repPassword = etRepPassword.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || password.isEmpty() || repPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != repPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Llamar a la función para registrar al usuario
            registrarUsuario(nombre, apellido, email, password)
        }
    }
    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Esto evita que el usuario pueda regresar a la actividad de registro con el botón "Atrás"
    }

    private fun registrarUsuario(nombre: String, apellido: String, email: String, password: String) {
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
                    val query = "INSERT INTO Usuarios (nombre, apellido, correo_electronico, contrasena) VALUES ('$nombre', '$apellido', '$email', '$password')"
                    statement.executeUpdate(query)

                    runOnUiThread {
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        // Puedes redirigir al usuario a otra actividad después del registro exitoso
                        goToLogin()
                    }
                }
            } catch (e: SQLException) {
                runOnUiThread {
                    Toast.makeText(this, "Error al registrar el usuario", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                runOnUiThread {
                    Toast.makeText(this, "Error: Driver JDBC no encontrado", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            } finally {
                try {
                    connection?.close()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

}