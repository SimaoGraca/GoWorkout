package ua.goworkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ua.goworkout.databinding.ActivityLoginBinding
import ua.goworkout.fragments.UserFragment

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Verificar se o user está logado ao iniciar o aplicativo
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("login", false)

        if (isLoggedIn) {
            val idUser = sharedPref.getInt("id_user", -1)
            val nome = sharedPref.getString("nome", "")
            val clube_id = sharedPref.getString("clube_id", "")

            if (idUser != -1 && nome != null) {
                // O user está logado, redireciona para a UserFragment
                val intent = Intent(this, UserFragment::class.java).apply {
                    putExtra("id_user", idUser)
                    putExtra("nome", nome)
                }
                startActivity(intent)
                finish()
            }
        }

        // Configurar o OnClickListener para o TextView do registro
        binding.registerText.setOnClickListener {
            // Redirecionar para a RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    fun doLogin(view: View) {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString().trim()

        // Verifica se os campos estão vazios
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Instancia a fila de requisições.
        val queue = Volley.newRequestQueue(this)
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/login.php"

        // Cria o JSON com os parâmetros.
        val jsonParams = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        // Loga o JSON que está sendo enviado
        Log.d("JSONRequest", "Enviando JSON: $jsonParams")

        // Cria a requisição JSON.
        val jsonRequest = JsonObjectRequest(Request.Method.POST, url, jsonParams,
            { response ->
                try {
                    // Loga a resposta JSON
                    Log.d("JSONResponse", "Resposta JSON: $response")

                    if (response.has("status")) {
                        val status = response.getString("status")
                        val msg = response.optString("message", "Sem mensagem") // Usando optString para evitar erro
                        // Dentro da parte que trata a resposta JSON
                        if (status == "OK") {
                            val idUser = response.getInt("id_user")
                            val nome = response.getString("nome")
                            val genero = response.getString("genero")
                            val email = response.getString("email")
                            val peso = response.getString("peso")
                            val telefone = response.getString("telefone")
                            val altura = response.getString("altura")
                            val foto_perfil = response.getString("foto_perfil")
                            val clubenome = response.getString("clube_nome")
                            val clube_id = response.getString("clube_id")
                            val cor = response.getString("cor")
                            val cidade = response.getString("cidade")
                            val endereco = response.getString("endereco")
                            val data_nascimento = response.getString("data_nascimento")

                            // Obter horários de funcionamento, verificando a existência de cada campo
                            val horarios = response.getJSONObject("horarios")
                            val diasUteis = horarios.optJSONObject("dias_uteis")?.toString() ?: "Não disponível"
                            val sabado = horarios.optJSONObject("sabado")?.toString() ?: "Não disponível"
                            val domingo = horarios.optJSONObject("domingo")?.toString() ?: "Não disponível"
                            val feriado = horarios.optJSONObject("feriado")?.toString() ?: "Não disponível"

                            // Salva os dados do user em SharedPreferences
                            val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
                            sharedPref.edit().apply {
                                putBoolean("login", true)
                                putInt("id_user", idUser)
                                putString("nome", nome)
                                putString("email", email)
                                putString("genero", genero)
                                putString("peso", peso)
                                putString("telefone", telefone)
                                putString("data_nascimento", data_nascimento)
                                putString("altura", altura)
                                putString("foto_perfil", foto_perfil)
                                putString("clube_nome", clubenome)
                                putString("clube_id", clube_id)
                                putString("cor", cor)
                                putString("cidade", cidade)
                                putString("endereco", endereco)
                                putString("dias_uteis", diasUteis)
                                putString("horario_sabado", sabado)
                                putString("horario_domingo", domingo)
                                putString("horario_feriado", feriado)
                                apply()
                            }

                            // Exibe o Toast de sucesso
                            Toast.makeText(this, "Login bem sucedido!", Toast.LENGTH_SHORT).show()

                            // Redireciona para a BaseActivity
                            val intent = Intent(this, BaseActivity::class.java).apply {
                                putExtra("id_user", idUser)
                                putExtra("nome", nome)
                            }
                            startActivity(intent)
                            finish()
                        }
                        else {
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("JSONResponse", "Resposta JSON não contém 'status'")
                        Toast.makeText(this, "Erro: Status ausente na resposta", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    Log.e("JSONResponse", "Erro ao analisar resposta JSON", e)
                }
            },
            { error ->
                Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
                Log.e("JSONResponse", "Erro na requisição JSON", error)
            }
        )

        // Adiciona a requisição à fila de requisições.
        queue.add(jsonRequest)
    }
}


