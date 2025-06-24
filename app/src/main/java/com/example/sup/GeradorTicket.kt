package com.example.sup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


data class SupportTicket(
    var id: String? = null,
    val motive: String = "",
    val company: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val status: String = "Open",
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)

class GeradorTicket : AppCompatActivity() {
    private lateinit var editTextMotive: EditText
    private lateinit var spinnerCompany: Spinner
    private lateinit var buttonCreateTicket: Button
    private lateinit var progressBarCreateTicket: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var selectedCompany: String? = null
    private val companiesList = mutableListOf<String>()

    companion object {
        private const val TAG = "GeradorTicket"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gerador_ticket)

        auth = Firebase.auth
        db = Firebase.firestore

        editTextMotive = findViewById(R.id.editTextMotive)
        spinnerCompany = findViewById(R.id.spinnerCompany)
        buttonCreateTicket = findViewById(R.id.buttonCreateTicket)
        progressBarCreateTicket = findViewById(R.id.progressBarCreateTicket)


        val mainLayout = findViewById<View>(R.id.mainCreateTicketLayout)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (auth.currentUser == null) {
            Toast.makeText(this, "Você precisa estar logado para criar um ticket.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupCompanySpinner()

        buttonCreateTicket.setOnClickListener {
            createAndSaveTicket()
        }
    }

    private fun setupCompanySpinner() {
        val staticCompanies = listOf("Selecione uma empresa", "Empresa Alpha", "Empresa Beta", "Tech Solutions Inc.")
        companiesList.addAll(staticCompanies)

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            companiesList
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCompany.adapter = spinnerAdapter

        spinnerCompany.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                selectedCompany = if (position == 0) {
                    null
                } else {
                    parent.getItemAtPosition(position).toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCompany = null
            }
        }

    }

    private fun createAndSaveTicket() {
        val motive = editTextMotive.text.toString().trim()
        val currentUser = auth.currentUser ?: return // Should be checked in onCreate, but good practice

        // --- Validations ---
        if (motive.isEmpty()) {
            editTextMotive.error = "O motivo/problema é obrigatório."
            editTextMotive.requestFocus()
            return
        }

        if (selectedCompany == null) {
            Toast.makeText(this, "Por favor, selecione uma empresa.", Toast.LENGTH_SHORT).show()
            return
        }
        // --- End Validations ---

        setLoading(true)

        val userId = currentUser.uid
        val userDisplayName = currentUser.displayName ?: "Usuário" // Fallback
        val userEmail = currentUser.email ?: "email.nao.disponivel@example.com"

        val newTicket = SupportTicket(
            motive = motive,
            company = selectedCompany!!,
            userId = userId,
            userName = userDisplayName,
            userEmail = userEmail,
            status = "Open"

        )

        db.collection("supportTickets")
            .add(newTicket)
            .addOnSuccessListener { documentReference ->
                val newTicketId = documentReference.id
                Log.d(TAG, "Ticket criado com sucesso. ID: $newTicketId")
                Toast.makeText(this, "Ticket criado com sucesso!", Toast.LENGTH_SHORT).show()
                setLoading(false)

                val intent = Intent(this, Chat::class.java)
                intent.putExtra("TICKET_ID", newTicketId)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("COMPANY_NAME", selectedCompany)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Log.w(TAG, "Erro ao criar ticket", e)
                Toast.makeText(this, "Falha ao criar ticket: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBarCreateTicket.visibility = if (isLoading) View.VISIBLE else View.GONE
        buttonCreateTicket.isEnabled = !isLoading
        editTextMotive.isEnabled = !isLoading
        spinnerCompany.isEnabled = !isLoading
    }
}