package ua.goworkout

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ua.goworkout.databinding.ActivityEditarPasswordBinding

class EditarPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditarPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar ação do botão de voltar
        binding.backArrow.setOnClickListener {
            finish() // Fecha a Activity e retorna para a anterior
        }

        binding.saveButton.setOnClickListener {
            val currentPassword = binding.currentPassword.text.toString()
            val newPassword = binding.newPassword.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            // Validar os campos
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "A nova palavra-passe e a confirmação não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
            val id_user = sharedPref.getInt("id_user", 0)

            // Criar o JSON com os parâmetros
            val jsonParams = JSONObject().apply {
                put("current_password", currentPassword)
                put("new_password", newPassword)
                put("confirm_password", confirmPassword)
                put("id_user", id_user)
            }

            // Log do JSON enviado
            Log.d("EditarPasswordActivity", "Enviando os seguintes dados: $jsonParams")

            // Enviar o pedido para o servidor
            val queue = Volley.newRequestQueue(this)
            val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/updatepassword.php"

            val jsonObjectRequest = object : JsonObjectRequest(
                Request.Method.POST, url, jsonParams,
                Response.Listener { response ->
                    // Log da resposta recebida para inspecionar o conteúdo
                    Log.d("EditarPasswordActivity", "Resposta da API: $response")

                    // Tentar interpretar a resposta como JSON
                    try {
                        // Obter o status da resposta
                        val status = response.optString("status", "error")
                        val message = response.optString("message", "Erro desconhecido")

                        // Verificar o status e exibir a mensagem
                        if (status == "success") {
                            // Mensagem de sucesso
                            Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_LONG).show()
                            finish() // Fechar a Activity após alteração de senha bem-sucedida
                        } else {
                            // Mensagem de erro
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }

                    } catch (e: Exception) {
                        // Caso ocorra um erro ao interpretar a resposta, logar o erro
                        Log.e("EditarPasswordActivity", "Erro ao interpretar a resposta JSON", e)
                        Toast.makeText(this, "Erro na resposta da API", Toast.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error ->
                    // Logar erro da rede
                    Log.e("Erro na API", "Erro: ${error.localizedMessage}")
                    Log.e("Erro na API", "Detalhes do erro: ${error.networkResponse}")

                    // Exibir a mensagem de erro na UI
                    Toast.makeText(this, "Algo correu mal. Tente novamente mais tarde.", Toast.LENGTH_SHORT).show()
                }
            ) {
                // Adicionar cabeçalhos, se necessário (por exemplo, para autenticação)
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = mutableMapOf<String, String>()
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }

            // Adicionar o pedido à fila do Volley
            queue.add(jsonObjectRequest)
        }

        // Configurar ação do botão de Cancelar
        binding.cancelButton.setOnClickListener {
            finish() // Voltar para a Activity anterior
        }
    }
}
