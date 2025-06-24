package com.example.sup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar // Add to layout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog // For confirmation dialogs
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Dados_da_conta : AppCompatActivity() {
    private lateinit var usuarioET: EditText // Assuming this displays email
    private lateinit var nomeET: EditText
    private lateinit var mudarSenhaTV: TextView
    private lateinit var cancelarContaTV: TextView
    private lateinit var salvarBT: Button
    private lateinit var progressBarDados: ProgressBar // Add to your layout

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentUser: FirebaseUser? = null

    private val TAG = "DadosDaContaActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dados_da_conta) // Call this early

        // Initialize Firebase
        auth = Firebase.auth
        db = Firebase.firestore
        currentUser = auth.currentUser

        // Initialize Views AFTER setContentView
        usuarioET = findViewById(R.id.usuario)
        nomeET = findViewById(R.id.nomeET)
        mudarSenhaTV = findViewById(R.id.mudarSenha)
        cancelarContaTV = findViewById(R.id.cancelarConta)
        salvarBT = findViewById(R.id.Salvar)
        // progressBarDados = findViewById(R.id.progressBarDados) // Initialize from XML


        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_LONG).show()
            // Navigate to login
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        loadUserData()

        salvarBT.setOnClickListener {
            saveChanges()
        }

        mudarSenhaTV.setOnClickListener {
            currentUser?.email?.let { email ->
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Email de redefinição de senha enviado para $email", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Falha ao enviar email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } ?: Toast.makeText(this, "Email do usuário não encontrado.", Toast.LENGTH_SHORT).show()

            // Option 2: Navigate to a dedicated "Change Password" activity
            // (More complex, requires current password for re-authentication)
            // startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        cancelarContaTV.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
    }

    private fun loadUserData() {
        setLoading(true)
        currentUser?.let { user ->
            usuarioET.setText(user.email ?: "Email não disponível")
            usuarioET.isEnabled = false // Email usually not directly editable here for simplicity

            // Fetch name from Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    setLoading(false)
                    if (document != null && document.exists()) {
                        val name = document.getString("name")
                        nomeET.setText(name ?: "")
                    } else {
                        Log.d(TAG, "No such document for user details")
                        nomeET.setText("") // Or a placeholder
                    }
                }
                .addOnFailureListener { exception ->
                    setLoading(false)
                    Log.d(TAG, "get failed with ", exception)
                    Toast.makeText(this, "Falha ao carregar dados: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: setLoading(false)
    }

    private fun saveChanges() {
        val newName = nomeET.text.toString().trim()

        if (newName.isEmpty()) {
            nomeET.error = "Nome não pode ser vazio"
            nomeET.requestFocus()
            return
        }

        currentUser?.let { user ->
            setLoading(true)
            val userUpdates = hashMapOf<String, Any>(
                "name" to newName
                // Add any other fields you allow to be updated
            )

            db.collection("users").document(user.uid)
                .update(userUpdates)
                .addOnSuccessListener {
                    setLoading(false)
                    Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "User data updated in Firestore.")
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Falha ao salvar dados: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.w(TAG, "Error updating user data", e)
                }
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Conta")
            .setMessage("Tem certeza que deseja cancelar sua conta? Esta ação é irreversível e todos os seus dados serão perdidos. Você precisará digitar sua senha para confirmar.")
            .setView(R.layout.dialog_reauthenticate_password) // Create this layout with an EditText for password
            .setPositiveButton("Confirmar e Cancelar") { dialog, _ ->
                val passwordDialog = dialog as AlertDialog
                val passwordEditText = passwordDialog.findViewById<EditText>(R.id.editTextDialogPassword) // ID from your dialog_reauthenticate_password.xml
                val password = passwordEditText?.text.toString()

                if (password.isNotEmpty()) {
                    reauthenticateAndDeleteAccount(password)
                } else {
                    Toast.makeText(this, "Senha é obrigatória para cancelar a conta.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun reauthenticateAndDeleteAccount(password: String) {
        currentUser?.let { user ->
            user.email?.let { email ->
                setLoading(true)
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential)
                    .addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            Log.d(TAG, "User re-authenticated.")
                            // Now delete Firestore data
                            db.collection("users").document(user.uid).delete()
                                .addOnSuccessListener {
                                    Log.d(TAG, "User data deleted from Firestore.")
                                    // Now delete Auth user
                                    user.delete()
                                        .addOnCompleteListener { deleteTask ->
                                            setLoading(false)
                                            if (deleteTask.isSuccessful) {
                                                Log.d(TAG, "User account deleted from Firebase Auth.")
                                                Toast.makeText(this, "Conta cancelada com sucesso.", Toast.LENGTH_LONG).show()
                                                // Navigate to login screen
                                                val intent = Intent(this, MainActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Log.w(TAG, "Error deleting user account from Auth.", deleteTask.exception)
                                                Toast.makeText(this, "Falha ao cancelar conta (Auth): ${deleteTask.exception?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    setLoading(false)
                                    Log.w(TAG, "Error deleting user data from Firestore.", e)
                                    Toast.makeText(this, "Falha ao cancelar conta (Firestore): ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            setLoading(false)
                            Log.w(TAG, "User re-authentication failed.", reauthTask.exception)
                            Toast.makeText(this, "Reautenticação falhou. Senha incorreta?", Toast.LENGTH_LONG).show()
                        }
                    }
            } ?: run {
                setLoading(false)
                Toast.makeText(this, "Email do usuário não encontrado para reautenticação.", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun setLoading(isLoading: Boolean) {
        // if(::progressBarDados.isInitialized) {
        // progressBarDados.visibility = if (isLoading) View.VISIBLE else View.GONE
        // }
        salvarBT.isEnabled = !isLoading
        nomeET.isEnabled = !isLoading
        // Keep usuarioET (email) disabled if you don't intend for it to be changed here
        mudarSenhaTV.isEnabled = !isLoading
        cancelarContaTV.isEnabled = !isLoading
    }
}