package com.example.safenav

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.safenav.MenuActivity
import com.example.safenav.MainActivity
import com.example.safenav.R

class ConfigurationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_configuration)

        // Agregar un botón para cerrar sesión
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            // Llama a la función para cerrar sesión
            MenuActivity.textViewUsername.text = ""
            logout()
        }

    }

    // Método para cerrar sesión
    private fun logout() {
        // Elimina el ID de usuario de las preferencias compartidas
        val sharedPreferences = getSharedPreferences("sesion", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("userId")
        editor.remove("tvNombreUsuario")
        editor.remove("nombreUsuario")
        // Actualiza el TextView con una cadena vacía
        editor.apply()

        // Inicia la actividad de inicio de sesión
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Finaliza la actividad actual
        finish()
    }
}