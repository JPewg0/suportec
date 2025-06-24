package com.example.sup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns // For email validation
import android.view.View // For ProgressBar visibility
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar // Add to your layout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth // KTX extension
import com.google.firebase.firestore.ktx.firestore // Firestore KTX
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var usuarioET: EditText
    private lateinit var senhaET: EditText
    private lateinit var recuperarSenhaTV: TextView
    private lateinit var cadastraseBT: Button
    private lateinit var loginBT: Button
    private lateinit var progressBarLogin: ProgressBar

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private val TAG = "MainActivity"


    object UserType {
        const val CLIENTE = "cliente"
        const val SUPORTE = "suporte"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        usuarioET = findViewById(R.id.usuario)
        senhaET = findViewById(R.id.senha)
        recuperarSenhaTV = findViewById(R.id.recuperar_senha)
        cadastraseBT = findViewById(R.id.cadastra_se)
        loginBT = findViewById(R.id.login)
        progressBarLogin = findViewById(R.id.progressBarLogin)


         if (auth.currentUser != null) {
            Log.d(TAG, "User already signed in on create: ${auth.currentUser?.uid}")
             fetchUserTypeAndNavigate(auth.currentUser!!) }

        loginBT.setOnClickListener {
            performLogin()
        }

        cadastraseBT.setOnClickListener {
            startActivity(Intent(this, Cadastras_se::class.java))
        }

        recuperarSenhaTV.setOnClickListener {
            Toast.makeText(this, "Forgot Password Clicked - Implement me!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val email = usuarioET.text.toString().trim()
        val password = senhaET.text.toString().trim()

        if (email.isEmpty()) {
            usuarioET.error = "Email é obrigatório"
            usuarioET.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usuarioET.error = "Insira um email válido"
            usuarioET.requestFocus()
            return
        }
        if (password.isEmpty()) {
            senhaET.error = "Senha é obrigatória"
            senhaET.requestFocus()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        fetchUserTypeAndNavigate(user) // Fetch type THEN navigate
                    } else {
                        setLoading(false)
                        Toast.makeText(baseContext, "Falha ao obter dados do usuário.", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                } else {
                    setLoading(false)
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Falha na autenticação: ${task.exception?.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun fetchUserTypeAndNavigate(firebaseUser: FirebaseUser) {
        val userId = firebaseUser.uid
        db.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                setLoading(false)
                if (documentSnapshot.exists()) {
                    val userType = documentSnapshot.getString("userType")
                    Log.d(TAG, "User type fetched: $userType for user $userId")


                    when (userType) {
                        UserType.CLIENTE -> {
                            val intent = Intent(this, ClienteMenu::class.java) // Activity for Cliente
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        UserType.SUPORTE -> {
                            // TODO: Replace SuporteDashboardActivity::class.java with your actual support dashboard
                            val intent = Intent(this, SuporteMenu::class.java) // Activity for Suporte
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        else -> {

                            Log.w(TAG, "User type not found or unknown in Firestore: $userType")
                            Toast.makeText(this, "Tipo de usuário desconhecido. Contate o suporte.", Toast.LENGTH_LONG).show()
                             auth.signOut()
                             updateUI(null)
                        }
                    }
                } else {
                    Log.w(TAG, "User document does not exist in Firestore for user $userId")
                    Toast.makeText(this, "Dados do usuário não encontrados. Contate o suporte.", Toast.LENGTH_LONG).show()
                     auth.signOut()
                     updateUI(null)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Log.e(TAG, "Error fetching user type from Firestore", e)
                Toast.makeText(this, "Erro ao buscar dados do usuário: ${e.message}", Toast.LENGTH_LONG).show()
                 auth.signOut()
                 updateUI(null)
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.i(TAG, "UpdateUI: User ${user.uid} is signed in.")
        } else {
            Log.i(TAG, "UpdateUI: User is signed out.")
            usuarioET.text.clear()
            senhaET.text.clear()
            usuarioET.error = null // Clear previous errors
            senhaET.error = null
        }
    }


    private fun setLoading(isLoading: Boolean) {
        progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginBT.isEnabled = !isLoading
        cadastraseBT.isEnabled = !isLoading
        usuarioET.isEnabled = !isLoading
        senhaET.isEnabled = !isLoading
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "onStart: User ${currentUser.uid} already logged in. Fetching type...")
            setLoading(true)
            fetchUserTypeAndNavigate(currentUser)
        }
    }
}