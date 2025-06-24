package com.example.sup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ClienteMenu : AppCompatActivity() {
    private lateinit var solicitarAtendimentoBT: Button
    private lateinit var logoutBT: Button
    private lateinit var cabecarioIV: ImageView
    private lateinit var ticketsListView: ListView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ticketsAdapter: ClienteTicketAdapter
    private val ticketList = mutableListOf<SupportItem>()

    private val TAG = "ClienteMenu"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cliente_menu)

        auth = Firebase.auth
        db = Firebase.firestore

        solicitarAtendimentoBT = findViewById(R.id.SolicitarA)
        logoutBT = findViewById(R.id.buttonLogout)
        cabecarioIV = findViewById(R.id.cabecario)
        ticketsListView = findViewById(R.id.listViewTickets)

        ticketsAdapter = ClienteTicketAdapter(this, ticketList)
        ticketsListView.adapter = ticketsAdapter

        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cabecarioIV.setOnClickListener {
            startActivity(Intent(this, Dados_da_conta::class.java))
        }

        solicitarAtendimentoBT.setOnClickListener {
            startActivity(Intent(this, GeradorTicket::class.java))
        }

        logoutBT.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        ticketsListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedTicket = ticketList[position]
            if (selectedTicket.id.isNotEmpty()) {
                val intent = Intent(this, Chat::class.java)
                intent.putExtra("TICKET_ID", selectedTicket.id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "ID do chamado inválido.", Toast.LENGTH_SHORT).show()
            }
        }

        loadUserTickets()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega os chamados sempre que a tela se torna visível
        loadUserTickets()
    }

    private fun loadUserTickets() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Se não houver utilizador, limpa a lista e não faz nada
            ticketList.clear()
            ticketsAdapter.notifyDataSetChanged()
            return
        }

        val userId = currentUser.uid
        db.collection("supportTickets")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING) // Mostra os mais recentes primeiro
            .get()
            .addOnSuccessListener { documents ->
                ticketList.clear() // Limpa a lista antes de adicionar novos itens
                if (documents.isEmpty) {
                    Log.d(TAG, "Nenhum chamado encontrado para este utilizador.")
                } else {
                    for (document in documents) {
                        val item = document.toObject(SupportItem::class.java).copy(id = document.id)
                        ticketList.add(item)
                    }
                }
                ticketsAdapter.notifyDataSetChanged() // Notifica o adapter que os dados mudaram
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao buscar chamados: ", exception)
                Toast.makeText(this, "Erro ao carregar chamados: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}
