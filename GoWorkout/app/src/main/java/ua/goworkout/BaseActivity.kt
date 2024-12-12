package ua.goworkout

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import org.json.JSONException
import org.json.JSONObject
import ua.goworkout.databinding.ActivityBaseBinding
import ua.goworkout.fragments.MarcacaoFragment
import ua.goworkout.fragments.PerfilFragment
import ua.goworkout.fragments.UserFragment

open class BaseActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityBaseBinding.inflate(layoutInflater)
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var requestQueue: RequestQueue
    private var userId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Configuração da barra superior
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val cor = sharedPref.getString("cor", "#000000")
        userId = sharedPref.getInt("id_user", 1)
        val nome = sharedPref.getString("nome", "User")
        findViewById<View>(R.id.top_bar).setBackgroundColor(Color.parseColor(cor))

        // Configuração do Volley
        requestQueue = Volley.newRequestQueue(this)

        // Configuração do DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Configuração do Toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.top_bar)
        setSupportActionBar(toolbar)

        // Esconder o título da aplicação no Toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configuração do ActionBarDrawerToggle
        actionBarDrawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.drawer_open, R.string.drawer_close
        )

        // Usando o ícone personalizado
        actionBarDrawerToggle.drawerArrowDrawable.color = resources.getColor(R.color.white)
        actionBarDrawerToggle.isDrawerIndicatorEnabled = true

        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        // Recuperar a cor da barra superior nos SharedPreferences
        val iconColor = try {
            Color.parseColor(cor) // A cor da barra superior
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#000000") // Se a cor for inválida, usa preto como fallback
        }

        // Criar uma lista de estados para os ícones: selecionado e não selecionado
        val stateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected), // Selecionado
                intArrayOf() // Não selecionado
            ),
            intArrayOf(iconColor, Color.GRAY) // Selecionado (cor da barra superior) e não selecionado (cinza)
        )

        // Configuração do BottomNavigationView
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)

        // Aplicar a cor aos ícones do BottomNavigationView
        bottomNav.itemIconTintList = stateList

        // Aplicar a cor branca ao texto dos itens (sempre branco)
        bottomNav.setItemTextColor(ColorStateList.valueOf(Color.WHITE))

        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            // Limpar todas as views anteriores no layout
            binding.contentFrame.removeAllViews()

            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.content_frame, UserFragment())
                        .commit()
                    true
                }
                R.id.nav_marcar -> {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.content_frame, MarcacaoFragment())
                        .commit()
                    true
                }
                R.id.nav_perfil -> {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.content_frame, PerfilFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Configuração do NavigationView
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_item_one -> {
                    // Handle navigation item one click
                }
                R.id.nav_perfil -> {
                    // Substitui o fragmento no content_frame com o PerfilFragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content_frame, PerfilFragment())
                        .commit()

                    // Atualiza o item selecionado do BottomNavigationView para 'perfil'
                    bottomNav.selectedItemId = R.id.nav_perfil

                    true
                }
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        // Configuração do cabeçalho do NavigationView
        val headerView = navView.getHeaderView(0)
        val backArrow = headerView.findViewById<ImageView>(R.id.back_arrow)
        val languageFlag = headerView.findViewById<ImageView>(R.id.language_flag)

        backArrow.setOnClickListener {
            drawerLayout.closeDrawers()
        }

        languageFlag.setOnClickListener {
            // Implementar mudança de idioma
        }

        // Carregar o fragmento inicial
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home // Vai carregar o fragmento de 'Home' por padrão
        }

        // Verificar feedback do usuário
        userId?.let {
            checkUserFeedback(it.toString())
        }
    }


    private fun checkUserFeedback(userId: String) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/checkFeedback.php"

        // Criando um JSON com o userId
        val requestBody = JSONObject()
        try {
            requestBody.put("userId", userId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, requestBody,
            { response ->
                // Log da resposta do servidor
                Log.d("FeedbackResponse", "Resposta do servidor: $response")

                try {
                    val hasRated = response.getBoolean("hasRated")
                    Log.d("FeedbackResponse", "hasRated: $hasRated")  // Logando o valor de hasRated

                    // Verifica se o usuário já avaliou
                    if (!hasRated) {
                        Log.d("FeedbackResponse", "Usuário não avaliou, mostrando o rating card.")
                        Handler().postDelayed({
                            Log.d("RatingCard", "Atraso de 5 segundos passado, chamando showRatingCard()")
                            showRatingCard()  // Mostrar o rating card após 5 segundos
                        }, 5000) // 5 segundos
                    }
                } catch (e: JSONException) {
                    Log.e("FeedbackResponse", "Erro ao processar resposta JSON", e)
                    e.printStackTrace()
                }
            },
            { error ->
                // Log de erro
                Log.e("FeedbackResponse", "Erro na requisição: ${error.message}")
                Toast.makeText(this, "Erro ao verificar feedback", Toast.LENGTH_SHORT).show()
            })

        // Adiciona a requisição na fila do Volley
        requestQueue.add(jsonObjectRequest)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showRatingCard() {
        Log.d("RatingCard", "Exibindo o rating card...")

        // Infla o layout do rating card
        val inflater = LayoutInflater.from(this)
        val ratingCard = inflater.inflate(R.layout.rating_card, null)

        // Verifique se o container existe
        val ratingCardContainer = findViewById<FrameLayout>(R.id.rating_card_container)

        // Certifique-se de tornar o container visível
        if (ratingCardContainer.visibility != View.VISIBLE) {
            Log.d("RatingCard", "Tornando o container visível...")
            ratingCardContainer.visibility = View.VISIBLE // Torna o container visível
        }

        // Adiciona o rating card ao container
        ratingCardContainer.removeAllViews() // Remove qualquer visualização anterior
        ratingCardContainer.addView(ratingCard)

        // Ocultar a BottomNavigationView
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNav.visibility = View.GONE

        // Configura o RatingBar, EditText e Button
        val ratingBar = ratingCard.findViewById<RatingBar>(R.id.ratingBar)
        val feedbackText = ratingCard.findViewById<EditText>(R.id.etFeedback)
        val submitButton = ratingCard.findViewById<Button>(R.id.btnSubmitFeedback)

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            val feedback = feedbackText.text.toString()
            submitFeedback(userId!!.toString(), rating, feedback)
        }

        // Adicionar evento de clique fora do card para fechá-lo e mostrar o BottomNavigationView novamente
        ratingCardContainer.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                hideRatingCard()
                bottomNav.visibility = View.VISIBLE // Mostrar a BottomNavigationView novamente
            }
            true
        }

        // Configura a ação de clique para o ícone de fechar (cruz)
        val closeButton = ratingCard.findViewById<ImageView>(R.id.btnClose)
        closeButton.setOnClickListener {
            hideRatingCard()
            bottomNav.visibility = View.VISIBLE // Mostrar a BottomNavigationView novamente
        }
    }

    private fun hideRatingCard() {
        // Esconder o card de avaliação
        val ratingCardContainer = findViewById<FrameLayout>(R.id.rating_card_container)
        ratingCardContainer.visibility = View.GONE
    }




    private fun submitFeedback(userId: String, rating: Float, feedback: String) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/submitFeedback.php"
        val params = HashMap<String, String>()
        params["userId"] = userId
        params["rating"] = rating.toString()
        params["feedback"] = feedback

        val jsonObject = JSONObject(params as Map<*, *>)

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            { response ->
                Toast.makeText(this, "Obrigado pelo seu feedback!", Toast.LENGTH_SHORT).show()
                findViewById<FrameLayout>(R.id.rating_card_container).removeAllViews()

                // Mostrar novamente a BottomNavigationView após o feedback
                val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
                bottomNav.visibility = View.VISIBLE
            },
            { error ->
                Toast.makeText(this, "Erro ao enviar feedback", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(jsonObjectRequest)
    }

    // Função para mostrar o BottomSheetDialog de confirmação de logout
    @SuppressLint("MissingInflatedId")
    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_confirmation, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogView)

        val btnConfirmLogout = dialogView.findViewById<View>(R.id.btnYes)
        val btnCancelLogout = dialogView.findViewById<View>(R.id.btnNo)

        btnConfirmLogout.setOnClickListener {
            logOut()
            dialog.dismiss()
        }

        btnCancelLogout.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Função de logout
    fun logOut() {
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        sharedPref?.edit()?.apply {
            putBoolean("login", false)
            remove("id_user")
            remove("nome")
            remove("clube_nome")
            remove("cidade")
            remove("endereco")
            remove("dias_uteis")
            remove("horario_sabado")
            remove("horario_domingo")
            remove("horario_feriado")
            remove("cor")
            remove("foto_perfil")
            remove("frase_indice")
            apply()
        }

        // Redirecionar para a tela de login
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Finaliza a atividade atual
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment, fragment.javaClass.simpleName)
            .addToBackStack(fragment.javaClass.simpleName) // Adiciona à pilha de fragmentos
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
