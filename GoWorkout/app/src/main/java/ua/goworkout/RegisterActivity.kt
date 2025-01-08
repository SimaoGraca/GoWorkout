
package ua.goworkout

import VolleyMultipartRequest
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.json.JSONObject
import ua.goworkout.databinding.ActivityRegisterBinding
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegisterBinding.inflate(layoutInflater)
    }
    private var clubesMap = mutableMapOf<String, Int>()

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val generos = arrayOf(
            getString(R.string.text_choosegender), // Obtém o texto do recurso de strings
            "Masculino",
            "Feminino",
            "Outro"
        )
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, generos) {
            // Alterando a cor do texto do item selecionado, tamanho da fonte e padding
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.parseColor("#000000")) // Cor preta para o texto
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 10sp

                return view
            }

            // Alterando a cor do texto, tamanho da fonte e padding na lista suspensa
            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.parseColor("#000000")) // Cor preta para o texto
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 10sp

                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGenero.adapter = adapter

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGenero.adapter = adapter


        // Configurando o adapter para o Spinner
        binding.spinnerGenero.adapter = adapter


        // Configurar o clique na imagem de perfil
        binding.profileImageView.setOnClickListener {
            openImageChooser()
        }

        // Configurar o clique no campo de data de nascimento
        binding.editTextDataNascimento.setOnClickListener {
            showDatePickerDialog()
        }

        // Configuração do listener para o Spinner de Clubes
        binding.spinnerClube.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedClubeName = parentView.getItemAtPosition(position) as String
                val clubesMap = binding.spinnerClube.tag as Map<String, Int>
                val clubeId = clubesMap[selectedClubeName]

                // Verifica se o clube selecionado é válido (não é "Escolha o seu clube")
                if (clubeId != null && selectedClubeName != "Escolha o seu clube") {
                    // Chama a função para buscar os planos do clube selecionado
                    fetchPlanosByClube(clubeId)
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Nenhuma ação necessária se nada for selecionado
            }
        }

    // Quando a atividade for carregada, chame a função para configurar o Spinner de Planos
        setupPlanosSpinner()

    // Quando a atividade for carregada, chame a função para carregar os clubes
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
            // Carregar a imagem selecionada usando Glide com recorte circular
            Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.circleCropTransform()) // Aplica o recorte circular
                .into(binding.profileImageView)
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

                    // Adiciona o item de "placeholder" no começo da lista
                    // Adiciona o item de "placeholder" no começo da lista
                    clubesList.add(getString(R.string.text_chooseclub))

                    for (i in 0 until clubesArray.length()) {
                        val clube = clubesArray.getJSONObject(i)
                        val idClube = clube.getInt("id")
                        val nomeClube = clube.getString("nome")
                        clubesList.add(nomeClube)
                        clubesMap[nomeClube] = idClube
                    }

                    // Criando um adapter com personalização da cor do texto
                    val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, clubesList) {
                        // Alterando a cor do texto do item selecionado
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)
                            textView.setTextColor(Color.parseColor("#000000")) // Cor preta
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 10sp
                            return view
                        }

                        // Alterando a cor do texto no dropdown (itens visíveis quando o Spinner é clicado)
                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getDropDownView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)
                            textView.setTextColor(Color.parseColor("#000000")) // Cor preta
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 10sp
                            return view
                        }
                    }

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerClube.adapter = adapter

                    // Guardar os IDs dos clubes no Spinner usando uma tag
                    binding.spinnerClube.tag = clubesMap

                    // Configurar o listener para quando o usuário selecionar um clube
                    binding.spinnerClube.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val selectedClubeName = parentView.getItemAtPosition(position) as String
                            val clubeId = clubesMap[selectedClubeName]

                            // Verifique se um clube foi selecionado (não é o "Escolha o seu clube")
                            if (clubeId != null && selectedClubeName != "Escolha o seu clube") {
                                fetchPlanosByClube(clubeId)  // Chama a função que buscará os planos
                            }
                        }

                        override fun onNothingSelected(parentView: AdapterView<*>) {
                            // Nada selecionado, ou pode adicionar uma ação
                        }
                    }

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

    // Função chamada para configurar o Spinner de Planos
    private fun setupPlanosSpinner() {
        // Cria uma lista inicial com o "Escolha o seu Plano"
        val planosList = mutableListOf(getString(R.string.text_chooseplan))


        // Criando o adapter e personalizando a cor e o tamanho do texto
        val adapterPlanos = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, planosList) {
            // Alterando a cor do texto do item selecionado
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.parseColor("#000000")) // Cor preta
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 13sp
                return view
            }

            // Alterando a cor do texto no dropdown (itens visíveis quando o Spinner é clicado)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.parseColor("#000000")) // Cor preta
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 13sp
                return view
            }
        }

        // Atribui o adapter ao Spinner de Planos
        binding.spinnerplanos.adapter = adapterPlanos
    }

    private fun fetchPlanosByClube(clubeId: Int) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/getplanosclube.php?clube_id=$clubeId" // URL da API que retorna os planos para um clube específico

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val planosArray = response.getJSONArray("planos")
                    val planosList = mutableListOf<String>()
                    val planosMap = mutableMapOf<String, Int>()  // Mapa para armazenar os planos com seus IDs

                    // Adiciona o item de "placeholder" no começo da lista
                    planosList.add("Escolha o seu Plano")

                    // Preenche a lista com nome e preço dos planos
                    for (i in 0 until planosArray.length()) {
                        val plano = planosArray.getJSONObject(i)
                        val idPlano = plano.getInt("id")
                        val nomePlano = plano.getString("nome")
                        val precoPlano = plano.getString("preco") // A chave para o preço pode variar, se necessário, ajuste conforme a resposta da API

                        // Adiciona à lista o nome do plano seguido do preço
                        planosList.add("$idPlano - $nomePlano - $precoPlano")
                        planosMap[nomePlano] = idPlano
                    }

                    // Verificar se o mapa foi preenchido corretamente
                    Log.d("RegisterActivity", "Mapa de planos: $planosMap")

                    // Criando o adapter e personalizando a cor e o tamanho do texto
                    val adapterPlanos = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, planosList) {
                        // Alterando a cor do texto do item selecionado
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)
                            textView.setTextColor(Color.parseColor("#000000")) // Cor preta
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 13sp
                            return view
                        }

                        // Alterando a cor do texto no dropdown (itens visíveis quando o Spinner é clicado)
                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = super.getDropDownView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)
                            textView.setTextColor(Color.parseColor("#000000")) // Cor preta
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f) // Tamanho da letra para 13sp
                            return view
                        }
                    }

                    // Configurando o adapter no Spinner
                    adapterPlanos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerplanos.adapter = adapterPlanos

                    // Atribui o mapa de planos ao tag do Spinner
                    binding.spinnerplanos.tag = planosMap

                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Erro ao processar resposta da API para planos: ", e)
                    Toast.makeText(this, "Erro ao buscar planos", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("RegisterActivity", "Erro na requisição da API para planos: ", error)
                Toast.makeText(this, "Erro ao buscar planos: ${error.message}", Toast.LENGTH_SHORT).show()
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
        val planosMap = binding.spinnerplanos.tag as Map<String, Int>

        val planoNomeComPreco = binding.spinnerplanos.selectedItem.toString()
        // Extraímos apenas o nome do plano, sem o preço
        val planoNome = planoNomeComPreco.split(" - ")[1]  // Pega a segunda parte da string (nome do plano)

        // Verificar o valor do nome do plano
        Log.d("RegisterActivity", "Plano selecionado: $planoNome")

        val planoId = planosMap[planoNome] ?: 0  // Obtém o ID do plano selecionado
        val clubeId = clubesMap[clubeNome] ?: 0 // Obtém o ID do clube selecionado


        // Log: Verificar dados do formulário
        Log.d("RegisterActivity", "Dados do formulário: Nome=$nome, Email=$email, Telemovel=$telemovel, Data Nascimento=$dataNascimento, Genero=$genero, Altura=$altura, Peso=$peso, Clube=$clubeId, Plano=$planoId")

        if (nome.isBlank() || email.isBlank() || telemovel.isBlank() || dataNascimento.isBlank() ||
            genero.isBlank() || password.isBlank() || confirmPassword.isBlank() || altura.isBlank() || peso.isBlank() || clubeNome.isBlank() || planoNome.isBlank()) {
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
            "clube_id" to clubeId.toString(), // Adicionado o ID do clube
            "plano_id" to planoId.toString()  // Adicionado o ID do plano
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
                startActivity(Intent(this, LoginActivity::class.java))
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
