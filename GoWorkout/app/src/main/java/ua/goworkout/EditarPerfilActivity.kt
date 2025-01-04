package ua.goworkout

import VolleyMultipartRequest
import android.Manifest
import android.R
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ua.goworkout.databinding.ActivityEditarPerfilBinding
import com.android.volley.toolbox.Volley
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private val CAMERA_REQUEST_CODE = 1001
    private val GALLERY_REQUEST_CODE = 1002
    private val CAMERA_PERMISSION_REQUEST_CODE = 1003
    private val STORAGE_PERMISSION_REQUEST_CODE = 1004

    private var imageUri: Uri? = null // Variável para armazenar a imagem selecionada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar ação do botão de voltar
        binding.backArrow.setOnClickListener {
            finish() // Fecha a Activity e retorna para a anterior
        }

        // Buscar dados das SharedPreferences
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val nome = sharedPref.getString("nome", "User")
        val email = sharedPref.getString("email", "example@gmail.com")
        val altura = sharedPref.getString("altura", "0")
        val peso = sharedPref.getString("peso", "0")
        val genero = sharedPref.getString("genero", "default")?.toLowerCase() // Lembrar de deixar em minúsculas
        val telefone = sharedPref.getString("telefone", "") // Adicionando telefone
        val fotoPerfil = sharedPref.getString("foto_perfil", null) // Caminho da imagem de perfil
        val dataNascimentoStr = sharedPref.getString("data_nascimento", "") // Adicionando data de nascimento

        // Definir os valores dos campos
        binding.editName.setText(nome)
        binding.editEmail.setText(email)
        binding.editHeight.setText(altura)
        binding.editWeight.setText(peso)
        binding.editPhone.setText(telefone)

        // Configurar o Spinner para o gênero
        val genderOptions = arrayOf("Masculino", "Feminino", "Outro")
        val genderSpinner = binding.editGender
        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = adapter

        val generoIndex = when (genero) {
            "masculino" -> 0
            "feminino" -> 1
            "outro" -> 2
            else -> -1
        }

        if (generoIndex != -1) {
            genderSpinner.setSelection(generoIndex)
        }

        // Formatar e exibir a data de nascimento
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dataNascimentoFormatada = if (dataNascimentoStr?.isNotEmpty() == true) {
            try {
                val dataNascimento = inputFormat.parse(dataNascimentoStr)
                dataNascimento?.let { outputFormat.format(it) } ?: "Data inválida"
            } catch (e: Exception) {
                "Data inválida"
            }
        } else {
            "Data não disponível"
        }
        binding.editDob.setText(dataNascimentoFormatada)

        // Carregar a imagem de perfil usando Glide
        if (fotoPerfil != null) {
            val imageUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem/$fotoPerfil"
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Ignora o cache no disco
                .skipMemoryCache(true) // Ignora o cache na memória
                .into(binding.profileImage)
        }

        // Configurar clique na imagem de perfil
        binding.profileImage.setOnClickListener {
            showImageSourceDialog() // Mostrar o diálogo para escolher entre tirar foto ou escolher da galeria
        }

        // Configurar ação do botão de Cancelar
        binding.cancelButton.setOnClickListener {
            finish() // Voltar para a Activity anterior
        }

        // Configurar ação do botão de Atualizar
        binding.saveButton.setOnClickListener {
            atualizar() // Chama a função de atualização quando o botão é clicado
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Tirar foto", "Escolher da galeria")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha uma opção")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission() // Verifica permissão para a câmera
                    1 -> checkStoragePermission() // Verifica permissão para a galeria
                }
            }
        builder.show()
    }

    // Verificar e solicitar permissão para a câmera
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            openCamera() // Se já tiver permissão, abre a câmera
        }
    }

    // Verificar e solicitar permissão para acessar a galeria
    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST_CODE)
        } else {
            openGallery() // Se já tiver permissão, abre a galeria
        }
    }

    // Abre a câmera
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Câmera não disponível", Toast.LENGTH_SHORT).show()
        }
    }

    // Abre a galeria
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    // Trata os resultados das permissões
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera() // Permissão concedida, abre a câmera
                } else {
                    Toast.makeText(this, "Permissão para a câmera negada", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery() // Permissão concedida, abre a galeria
                } else {
                    Toast.makeText(this, "Permissão para a galeria negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Lidar com o resultado da captura de imagem ou seleção da galeria
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    imageUri = data?.data
                    imageUri?.let {
                        Glide.with(this)
                            .load(it)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.profileImage)
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    imageUri = data?.data
                    imageUri?.let {
                        Glide.with(this)
                            .load(it)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.profileImage)
                    }
                }
            }
        }
    }

    private fun atualizar() {
        val nome = binding.editName.text.toString()
        val email = binding.editEmail.text.toString()
        val telefone = binding.editPhone.text.toString()
        val altura = binding.editHeight.text.toString()
        val peso = binding.editWeight.text.toString()
        val genero = binding.editGender.selectedItem.toString()
        var dataNascimento = binding.editDob.text.toString()
        val sharedPref = getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val id_user = sharedPref.getInt("id_user", 0)

        // Verificar se todos os campos estão preenchidos
        if (nome.isBlank() || email.isBlank() || telefone.isBlank() || altura.isBlank() || peso.isBlank() || genero.isBlank()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar se o id_user é válido
        if (id_user == 0) {
            Toast.makeText(this, "User não autenticado. Por favor, faça login novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // Tentar formatar a data de nascimento para o formato 'yyyy-MM-dd'
        try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Supondo que o formato da data seja 'dd/MM/yyyy'
            val parsedDate = dateFormat.parse(dataNascimento)
            if (parsedDate != null) {
                val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate)
                dataNascimento = formattedDate
            } else {
                // Caso a data não seja válida, mostrar erro
                Toast.makeText(this, "Data de nascimento inválida", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao formatar data de nascimento", Toast.LENGTH_SHORT).show()
            return
        }

        // Criar o mapa de parâmetros
        val params = hashMapOf<String, String>(
            "nome" to nome,
            "email" to email,
            "telefone" to telefone,
            "altura" to altura,
            "peso" to peso,
            "genero" to genero,
            "data_nascimento" to dataNascimento,
            "id_user" to id_user.toString()
        )

        // Log para ver os parâmetros enviados
        Log.d("AtualizarPerfil", "Parâmetros enviados: $params")

        // Verificar se a imagem foi selecionada
        val imageUri = this.imageUri

        // Definir a URL da API para atualizar os dados
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/updateuser.php"

        // Instanciar a fila de requisições
        val queue = Volley.newRequestQueue(this)

        // Criar a requisição multipart com os parâmetros
        val multipartRequest = VolleyMultipartRequest(
            Request.Method.POST,
            url,
            { response ->
                // Se a resposta for positiva, notifique o sucesso
                Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()

                // Log da resposta
                Log.d("AtualizarPerfil", "Resposta da API: $response")

                // Atualizar os campos da SharedPreferences
                val sharedPreferences = getSharedPreferences("pmLogin", MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putString("nome", nome)
                    putString("email", email)
                    putString("telefone", telefone)
                    putString("altura", altura)
                    putString("peso", peso)
                    putString("genero", genero)
                    putString("data_nascimento", dataNascimento)
                }.apply()

                // Finaliza a Activity
                finish()
            },
            { error ->
                // Se ocorrer erro, mostre a mensagem de erro
                Toast.makeText(this, "Erro ao atualizar perfil: ${error.message}", Toast.LENGTH_SHORT).show()

                // Log de erro
                Log.e("AtualizarPerfil", "Erro na requisição: ${error.message}")
            },
            params, // Parâmetros de texto
            imageUri, // Imagem de perfil (Uri)
            this // Contexto
        )

        // Adicionar a requisição na fila
        queue.add(multipartRequest)
    }




}
