package com.example.sup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar // Add to layout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Recuperar_senha : AppCompatActivity() {
    private lateinit var emailET: EditText // Renamed for clarity
    private lateinit var enviarEmailBT: Button // Renamed for clarity (was Salvar)
    private lateinit var progressBarRecuperar: ProgressBar // Add to your layout

    private lateinit var auth: FirebaseAuth
    private val TAG = "RecuperarSenhaActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_recuperar_senha)

        auth = Firebase.auth

        emailET = findViewById(R.id.emailETC)
        enviarEmailBT = findViewById(R.id.salvar)


        enviarEmailBT.setOnClickListener {
            sendPasswordResetEmail()
        }
    }

    private fun sendPasswordResetEmail() {
        val emailAddress = emailET.text.toString().trim()

        if (emailAddress.isEmpty()) {
            emailET.error = "Email é obrigatório"
            emailET.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            emailET.error = "Insira um email válido"
            emailET.requestFocus()
            return
        }

        setLoading(true)
        auth.sendPasswordResetEmail(emailAddress)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Log.d(TAG, "Email de redefinição de senha enviado.")
                    Toast.makeText(
                        this,
                        "Email de redefinição de senha enviado para $emailAddress. Verifique sua caixa de entrada.",
                        Toast.LENGTH_LONG
                    ).show()

                     startActivity(Intent(this, MainActivity::class.java))
                     finish()
                } else {
                    Log.w(TAG, "sendPasswordResetEmail:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Falha ao enviar email de redefinição: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        // if(::progressBarRecuperar.isInitialized) {
        //     progressBarRecuperar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // }
        enviarEmailBT.isEnabled = !isLoading
        emailET.isEnabled = !isLoading
    }
}