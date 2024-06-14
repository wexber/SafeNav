package com.example.safenav

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        val tvGoMenu = findViewById<TextView>(R.id.tv_go_to_menu)
        tvGoMenu.setOnClickListener{
            goToMenu()
        }
    }
    private fun goToMenu(){
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }
}