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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ClienteMenu : AppCompatActivity() {
    // Variáveis que agora correspondem aos IDs do XML
    private lateinit var novoChamadoButton: Button // Antigo: solicitarAtendimentoBT
    private lateinit var deslogarButton: Button // Antigo: logoutBT
    private lateinit var cabecalhoImageView: ImageView // Antigo: cabecarioIV (não existe no XML fornecido)
    private lateinit var listaChamadosListView: ListView // Antigo: ticketsListView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ticketsAdapter: ClienteTicketAdapter

    private val ticketList = mutableListOf<SupportItem>()

    private val TAG = "ClienteMenu"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cliente_menu)

        // Inicialização de instâncias Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Vinculação de elementos da interface
        novoChamadoButton = findViewById(R.id.solicitacao)
        deslogarButton = findViewById(R.id.buttonLogout)
        listaChamadosListView = findViewById(R.id.ticketsList)

        // Configuração do adaptador para a lista de chamados
        ticketsAdapter = ClienteTicketAdapter(this, ticketList)
        listaChamadosListView.adapter = ticketsAdapter

        // Ajuste de insets da janela para barras do sistema
        val mainContainer = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { view, insets ->
            val systemWindowBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemWindowBars.left, systemWindowBars.top, systemWindowBars.right, systemWindowBars.bottom)
            insets
        }

        // Definição dos ouvintes de clique
        novoChamadoButton.setOnClickListener {
            val launchIntent = Intent(this, GeradorTicket::class.java)
            startActivity(launchIntent)
        }

        deslogarButton.setOnClickListener {
            auth.signOut()
            val logoutIntent = Intent(this, MainActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logoutIntent)
            finish()
        }

        // Configuração do ouvinte de clique para itens da lista
        listaChamadosListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedTicket = ticketList[position]
            if (selectedTicket.id.isNotEmpty()) {
                val chatIntent = Intent(this, Chat::class.java)
                chatIntent.putExtra("TICKET_ID", selectedTicket.id)
                startActivity(chatIntent)
            } else {
                Toast.makeText(this, "ID do chamado inválido.", Toast.LENGTH_SHORT).show()
            }
        }

        // Carregamento inicial dos tickets do usuário
        loadTickets()
    }

    override fun onResume() {
        super.onResume()
        loadTickets()
    }

    private fun loadTickets() {
        val loggedInUser = auth.currentUser
        if (loggedInUser == null) {
            ticketList.clear()
            ticketsAdapter.notifyDataSetChanged()
            return
        }

        val currentUserId = loggedInUser.uid

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fetchedDocuments = db.collection("supportTickets")
                    .whereEqualTo("userId", currentUserId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                withContext(Dispatchers.Main) {
                    ticketList.clear()
                    if (fetchedDocuments.isEmpty) {
                        Log.d(TAG, "Não foram encontrados registros de suporte para o usuário atual.")
                    } else {
                        for (doc in fetchedDocuments) {
                            try {
                                val supportEntry = doc.toObject(SupportItem::class.java).copy(id = doc.id)
                                supportEntry?.let { ticketList.add(it) }
                            } catch (e: Exception) {
                                Log.e(TAG, "Falha ao converter documento para SupportItem: ${doc.id}", e)
                            }
                        }
                    }
                    ticketsAdapter.notifyDataSetChanged()
                }
            } catch (operationFailed: Exception) {
                Log.e(TAG, "Erro ao obter entradas de suporte: ", operationFailed)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ClienteMenu, "Falha ao carregar entradas: ${operationFailed.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}