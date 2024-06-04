package com.example.safenav

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
/**import androidx.activity.enableEdgeToEdge**/
import androidx.appcompat.app.AppCompatActivity
/**import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat**/

class RegistroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        val tvGoLogin = findViewById<TextView>(R.id.tv_go_to_login)
        tvGoLogin.setOnClickListener{
            goToLogin()
        }
    }
    private fun goToLogin(){
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }
//holaaaaaasdasd
}