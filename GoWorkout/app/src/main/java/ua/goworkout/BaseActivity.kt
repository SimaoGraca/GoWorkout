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
import ua.goworkout.fragments.MarcacaoFragment
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

    private var isPortugueseFlag = true

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
        actionBarDrawerToggle.drawerArrowDrawable.color = resources.getColor(R.color.white)
        actionBarDrawerToggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        // Configuração do BottomNavigationView
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
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

        bottomNav.setOnItemSelectedListener { item: MenuItem ->
            binding.contentFrame.removeAllViews()
            when (item.itemId) {
                R.id.nav_home -> loadFragment(UserFragment())
                R.id.nav_marcar -> loadFragment(MarcacaoFragment())
                R.id.nav_perfil -> loadFragment(PerfilFragment())
                else -> false
            }
            true
        }

        // Configuração do NavigationView
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_perfil -> {
                    loadFragment(PerfilFragment())
                    bottomNav.selectedItemId = R.id.nav_perfil
                    true
                }
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
            drawerLayout.closeDrawers()
            true
        }

        // Cabeçalho do NavigationView
        val headerView = navView.getHeaderView(0)
        val backArrow = headerView.findViewById<ImageView>(R.id.back_arrow)
        val nameClientTextView = headerView.findViewById<TextView>(R.id.client_name)
        nameClientTextView.text = nome
        val languageFlag = headerView.findViewById<ImageView>(R.id.language_flag)

        backArrow.setOnClickListener {
            drawerLayout.closeDrawers()
        }

        languageFlag.setOnClickListener {
            toggleLanguageFlag(languageFlag)
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }

        userId?.let { checkUserFeedback(it.toString()) }
    }

    private fun setLocale(languageCode: String) {
        // Definir o novo idioma
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        // Aplicar o novo idioma à configuração
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Salvar a preferência de idioma no SharedPreferences
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        sharedPref.edit().putString("language", languageCode).apply()

        // Atualizar a interface de forma transparente
        invalidateOptionsMenu() // Invalida o menu para forçar a atualização de textos

        // Atualizar a interface de outras partes (por exemplo, recarregar texto)
        // Aqui você pode também forçar o reload de certos componentes ou UI que precisam ser atualizados
    }

    private fun toggleLanguageFlag(languageFlag: ImageView) {
        // Recuperar o idioma atual do SharedPreferences
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        var isPortugueseFlag = sharedPref.getString("language", "pt") == "pt"

        // Alterar o idioma e a bandeira
        if (isPortugueseFlag) {
            setLocale("en") // Definir para inglês
            languageFlag.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.english_flag, null))
        } else {
            setLocale("pt") // Definir para português
            languageFlag.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.portuguese_flag, null))
        }

        // Atualizar o idioma no SharedPreferences
        isPortugueseFlag = !isPortugueseFlag
        sharedPref.edit().putString("language", if (isPortugueseFlag) "pt" else "en").apply()

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
