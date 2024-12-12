package ua.goworkout

import VolleyMultipartRequest
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ua.goworkout.databinding.ActivityRegisterBinding
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegisterBinding.inflate(layoutInflater)
    }

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Configurar o Spinner com opções de género
        val generos = arrayOf("Masculino", "Feminino", "Outro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, generos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGenero.adapter = adapter

        // Configurar o clique na imagem de perfil
        binding.profileImageView.setOnClickListener {
            openImageChooser()
        }

        // Configurar o clique no campo de data de nascimento
        binding.editTextDataNascimento.setOnClickListener {
            showDatePickerDialog()
        }

        // Buscar clubes da API
        fetchClubes()

        // Configurar o OnClickListener para o TextView do registro
        binding.loginText.setOnClickListener {
            // Redirecionar para a RegisterActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Formatar a data no formato yyyy-MM-dd
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                binding.editTextDataNascimento.setText(formattedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            binding.profileImageView.setImageURI(imageUri)
        }
    }

    private fun fetchClubes() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/getclubes.php"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val clubesArray = response.getJSONArray("clubes")
                    val clubesList = mutableListOf<String>()
                    val clubesMap = mutableMapOf<String, Int>()
                    for (i in 0 until clubesArray.length()) {
                        val clube = clubesArray.getJSONObject(i)
                        val idClube = clube.getInt("id")
                        val nomeClube = clube.getString("nome")
                        clubesList.add(nomeClube)
                        clubesMap[nomeClube] = idClube
                    }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, clubesList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerClube.adapter = adapter

                    // Guardar os IDs dos clubes no Spinner usando uma tag
                    binding.spinnerClube.tag = clubesMap
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Erro ao processar resposta da API: ", e)
                    Toast.makeText(this, "Erro ao buscar clubes", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("RegisterActivity", "Erro na requisição da API: ", error)
                Toast.makeText(this, "Erro ao buscar clubes: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
    }


    fun doRegister(view: View) {
        val nome = binding.editTextNome.text.toString()
        val email = binding.editTextEmail.text.toString()
        val telemovel = binding.editTextTelemovel.text.toString()
        val dataNascimento = binding.editTextDataNascimento.text.toString()
        val genero = binding.spinnerGenero.selectedItem.toString()
        val password = binding.editTextPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()
        val altura = binding.editTextAltura.text.toString()
        val peso = binding.editTextPeso.text.toString()
        val clubeNome = binding.spinnerClube.selectedItem.toString()
        val clubesMap = binding.spinnerClube.tag as Map<String, Int>
        val clubeId = clubesMap[clubeNome] ?: 0 // Obtém o ID do clube selecionado

        // Log: Verificar dados do formulário
        Log.d("RegisterActivity", "Dados do formulário: Nome=$nome, Email=$email, Telemovel=$telemovel, Data Nascimento=$dataNascimento, Genero=$genero, Altura=$altura, Peso=$peso, Clube=$clubeId")

        if (nome.isBlank() || email.isBlank() || telemovel.isBlank() || dataNascimento.isBlank() ||
            genero.isBlank() || password.isBlank() || confirmPassword.isBlank() || altura.isBlank() || peso.isBlank() || clubeNome.isBlank()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
            return
        }

        // Criar o mapa de parâmetros com os dados do formulário
        val params = hashMapOf(
            "nome" to nome,
            "email" to email,
            "telemovel" to telemovel,
            "data_nascimento" to dataNascimento,
            "genero" to genero,
            "password" to password,
            "altura" to altura,
            "peso" to peso,
            "clube_id" to clubeId.toString() // Adicionado o ID do clube
        )

        // Log: Verificar conteúdo dos parâmetros
        Log.d("RegisterActivity", "Parâmetros a enviar para a API: $params")

        // Instanciar a fila de requisições
        val queue = Volley.newRequestQueue(this)
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/registo.php"

        // Criar a requisição multipart
        val multipartRequest = VolleyMultipartRequest(
            Request.Method.POST, url,
            { response ->
                // Log: Verificar a resposta da API
                Log.d("RegisterActivity", "Resposta da API: $response")
                Toast.makeText(this, "Registo realizado com sucesso", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this,LoginActivity::class.java))
                finish()
            },
            { error ->
                // Log: Verificar erro na requisição
                Log.e("RegisterActivity", "Erro na API: ", error)
                Toast.makeText(this, "Erro ao realizar registo: ${error.message}", Toast.LENGTH_SHORT).show()
            },
            params,
            imageUri,
            this // Passando o contexto
        )

        // Log: Verificar se a requisição foi criada corretamente
        Log.d("RegisterActivity", "Requisição criada. Enviando para a API.")

        // Adicionar a requisição à fila
        queue.add(multipartRequest)
    }
}
