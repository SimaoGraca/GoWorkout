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
import androidx.core.content.res.ResourcesCompat
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
import ua.goworkout.fragments.HistoryFragment
import ua.goworkout.fragments.MarcacaoFragment
import ua.goworkout.fragments.NextclassesFragment
import ua.goworkout.fragments.PerfilFragment
import ua.goworkout.fragments.UserFragment
import java.util.Locale

@Suppress("DEPRECATION")
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
        val tipo_user = sharedPref.getString("tipo_user", "")
        val nome = sharedPref.getString("nome", "User")
        val clubeNome = sharedPref?.getString("clube_nome", "Clube não encontrado")

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
        actionBarDrawerToggle.drawerArrowDrawable.color = resources.getColor(R.color.white)
        actionBarDrawerToggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()



        Log.d("UserType", "Tipo de user: $tipo_user")

        // Verificar se o tipo de usuário é instrutor
        val isInstrutor = tipo_user.equals("instrutor", ignoreCase = true) // Comparação segura, ignorando maiúsculas/minúsculas

        // Configuração do BottomNavigationView
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        val menu = bottomNav.menu
        val menu1 = navView.menu

        // Ocultar o item "nav_nextclasses" se o user não for instrutor
        if (!isInstrutor) {
            menu.findItem(R.id.nav_nextclasses)?.isVisible = false
            menu1.findItem(R.id.nav_nextclass)?.isVisible = false
        }

        // Configuração das cores dos ícones
        val iconColor = try {
            Color.parseColor(cor)
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#000000")
        }

        val stateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(iconColor, Color.GRAY)
        )
        bottomNav.itemIconTintList = stateList
        bottomNav.setItemTextColor(ColorStateList.valueOf(Color.WHITE))

        // Listener para os itens do BottomNavigationView
        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            binding.contentFrame.removeAllViews()
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(UserFragment())
                    navView.setCheckedItem(R.id.nav_inicio)
                }
                R.id.nav_marcar -> {
                    loadFragment(MarcacaoFragment())
                    navView.setCheckedItem(R.id.nav_marcaraula)
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    navView.setCheckedItem(R.id.nav_historico)
                }
                R.id.nav_perfil -> {
                    loadFragment(PerfilFragment())
                    navView.setCheckedItem(R.id.nav_perfil)
                }
                R.id.nav_nextclasses -> {
                    if (isInstrutor) {
                        loadFragment(NextclassesFragment())
                        navView.setCheckedItem(R.id.nav_nextclass)
                    }
                }
                else -> false
            }
            return@setOnItemSelectedListener true
        }


        // Configuração do NavigationView
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    loadFragment(UserFragment())
                    bottomNav.selectedItemId = R.id.nav_home
                }
                R.id.nav_nextclass -> {
                    if (isInstrutor) {
                        loadFragment(NextclassesFragment())
                        bottomNav.selectedItemId = R.id.nav_nextclasses
                    }
                }
                R.id.nav_marcaraula -> {
                    loadFragment(MarcacaoFragment())
                    bottomNav.selectedItemId = R.id.nav_marcar
                }
                R.id.nav_historico -> {
                    loadFragment(HistoryFragment())
                    bottomNav.selectedItemId = R.id.nav_history
                }
                R.id.nav_perfil -> {
                    loadFragment(PerfilFragment())
                    bottomNav.selectedItemId = R.id.nav_perfil
                }
                R.id.nav_changepassword -> {
                    startActivity(Intent(this, EditarPasswordActivity::class.java))
                }
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                }
            }
            drawerLayout.closeDrawers()
            return@setNavigationItemSelectedListener true
        }


        // Cabeçalho do NavigationView
        val headerView = navView.getHeaderView(0)
        val backArrow = headerView.findViewById<ImageView>(R.id.back_arrow)
        val nameClientTextView = headerView.findViewById<TextView>(R.id.client_name)
        val nameClubTextView = headerView.findViewById<TextView>(R.id.gym_name)
        nameClientTextView.text = nome
        nameClubTextView.text = clubeNome

        backArrow.setOnClickListener {
            drawerLayout.closeDrawers()
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }

        userId?.let { checkUserFeedback(it.toString()) }
    }


    private fun checkUserFeedback(userId: String) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/checkFeedback.php"
        val requestBody = JSONObject().apply {
            put("userId", userId)
        }

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, requestBody,
            { response ->
                try {
                    val hasRated = response.getBoolean("hasRated")
                    if (!hasRated) {
                        Handler().postDelayed({ showRatingCard() }, 5000)
                    }
                } catch (e: JSONException) {
                    Log.e("FeedbackResponse", "Erro ao processar resposta JSON", e)
                }
            },
            { error ->
                Log.e("FeedbackResponse", "Erro na requisição: ${error.message}")
                Toast.makeText(this, "Erro ao verificar feedback", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(jsonObjectRequest)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showRatingCard() {
        val inflater = LayoutInflater.from(this)
        val ratingCard = inflater.inflate(R.layout.rating_card, null)
        val ratingCardContainer = findViewById<FrameLayout>(R.id.rating_card_container)

        if (ratingCardContainer.visibility != View.VISIBLE) {
            ratingCardContainer.visibility = View.VISIBLE
        }

        ratingCardContainer.removeAllViews()
        ratingCardContainer.addView(ratingCard)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNav.visibility = View.GONE

        val ratingBar = ratingCard.findViewById<RatingBar>(R.id.ratingBar)
        val feedbackText = ratingCard.findViewById<EditText>(R.id.etFeedback)
        val submitButton = ratingCard.findViewById<Button>(R.id.btnSubmitFeedback)

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            val feedback = feedbackText.text.toString()
            submitFeedback(userId!!.toString(), rating, feedback)
            hideRatingCard()
        }

        ratingCardContainer.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                hideRatingCard()
                bottomNav.visibility = View.VISIBLE
            }
            true
        }

        val closeButton = ratingCard.findViewById<ImageView>(R.id.btnClose)
        closeButton.setOnClickListener {
            hideRatingCard()
            bottomNav.visibility = View.VISIBLE
        }
    }

    private fun hideRatingCard() {
        val ratingCardContainer = findViewById<FrameLayout>(R.id.rating_card_container)
        ratingCardContainer.visibility = View.GONE
    }

    private fun submitFeedback(userId: String, rating: Float, feedback: String) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/submitFeedback.php"
        val params = hashMapOf<String, String>(
            "userId" to userId,
            "rating" to rating.toString(),
            "feedback" to feedback
        )
        val jsonObject = JSONObject(params as Map<*, *>?)

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            { response ->
                Toast.makeText(this, "Obrigado pelo seu feedback!", Toast.LENGTH_SHORT).show()
                findViewById<FrameLayout>(R.id.rating_card_container).removeAllViews()
                findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.VISIBLE
            },
            { error ->
                Toast.makeText(this, "Erro ao enviar feedback", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(jsonObjectRequest)
    }

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

    private fun logOut() {
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean("login", false)
            remove("id_user")
            remove("tipo_user")
            remove("nome")
            remove("genero")
            remove("email")
            remove("peso")
            remove("altura")
            remove("telefone")
            remove("data_nascimento")
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

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
