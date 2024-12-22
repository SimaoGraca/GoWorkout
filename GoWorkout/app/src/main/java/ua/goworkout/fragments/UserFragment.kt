package ua.goworkout.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import org.json.JSONObject
import ua.goworkout.R
import ua.goworkout.databinding.FragmentUserBinding
import kotlin.random.Random

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var frasesMotivacionais: Array<String>

    data class Horario(
        val horario_abertura: String?,
        val horario_fecho: String?,
        val aberto: Int
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Acesso ao array de frases motivacionais no momento certo
        frasesMotivacionais = resources.getStringArray(R.array.frases_motivacionais)

        // Recuperar dados do usuário do SharedPreferences
        val sharedPref = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val idUser = sharedPref?.getInt("id_user", -1)
        val id_clube = sharedPref?.getString("clube_id", "")
        val nome = sharedPref?.getString("nome", "User")
        val fotoPerfil = sharedPref?.getString("foto_perfil", null)
        val clubeNome = sharedPref?.getString("clube_nome", "Clube não encontrado")
        val cidade = sharedPref?.getString("cidade", "Cidade não encontrada")
        val endereco = sharedPref?.getString("endereco", "Endereço não encontrado")
        val cor = sharedPref?.getString("cor", "#000000")

        if (id_clube != null) {
            if (idUser != null) {
                checkNotices(idUser, id_clube)
            }
        }

        // Configurar a cor da barra de status (igual na BaseActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity?.window?.statusBarColor = Color.parseColor(cor)
        }

        // Exibir mensagem de boas-vindas
        if (idUser != -1 && nome != null) {
            binding.welcomeMessage.text = resources.getString(R.string.text_welcome) + " " + nome
        } else {
            binding.welcomeMessage.text = "User não encontrado"
        }

        // Informações do ginásio
        binding.gymInformation.text = "Clube: $clubeNome\nMorada: $endereco\nCidade: $cidade"

        // LOG - Verificando os dados recuperados de SharedPreferences
        Log.d("UserFragment", "idUser: $idUser, Nome: $nome, Clube: $clubeNome, Clube_id: $id_clube, Cidade: $cidade")

        // Horários de funcionamento - Recuperando os dados JSON do SharedPreferences
        val diasUteisJson = sharedPref?.getString("dias_uteis", "Não disponível")
        val sabadoJson = sharedPref?.getString("horario_sabado", "Não disponível")
        val domingoJson = sharedPref?.getString("horario_domingo", "Não disponível")
        val feriadoJson = sharedPref?.getString("horario_feriado", "Não disponível")

        // Desserializar os JSONs para objetos Horario
        val gson = Gson()
        val horarioDiasUteis: Horario = gson.fromJson(diasUteisJson, Horario::class.java)
        val horarioSabado: Horario = gson.fromJson(sabadoJson, Horario::class.java)
        val horarioDomingo: Horario = gson.fromJson(domingoJson, Horario::class.java)
        val horarioFeriado: Horario = gson.fromJson(feriadoJson, Horario::class.java)

        // Formatar os horários para exibição
        val diasUteisFormatted = if (horarioDiasUteis.aberto == 1) {
            "Dias Úteis: ${formatHora(horarioDiasUteis.horario_abertura)} - ${formatHora(horarioDiasUteis.horario_fecho)}"
        } else {
            "Dias Úteis: Não disponível"
        }

        val sabadoFormatted = if (horarioSabado.aberto == 1) {
            "Sábado: ${formatHora(horarioSabado.horario_abertura)} - ${formatHora(horarioSabado.horario_fecho)}"
        } else {
            "Sábado: Não disponível"
        }

        val domingoFormatted = if (horarioDomingo.aberto == 1) {
            "Domingo: ${formatHora(horarioDomingo.horario_abertura)} - ${formatHora(horarioDomingo.horario_fecho)}"
        } else {
            "Domingo: Fechado"
        }

        val feriadoFormatted = if (horarioFeriado.aberto == 1) {
            "Feriado: ${formatHora(horarioFeriado.horario_abertura)} - ${formatHora(horarioFeriado.horario_fecho)}"
        } else {
            "Feriado: Não disponível"
        }

        // Atualizar a interface com os horários formatados
        binding.gymhorarioInformation.text = """
            $diasUteisFormatted
            $sabadoFormatted
            $domingoFormatted
            $feriadoFormatted
        """.trimIndent()

        // LOG - Verificando os horários recuperados
        Log.d("UserFragment", "$diasUteisFormatted, $sabadoFormatted, $domingoFormatted, $feriadoFormatted")

        // Carregar a imagem de perfil usando Glide com recorte circular
        if (fotoPerfil != null) {
            val imageUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem/$fotoPerfil"
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform()) // Aplica o recorte circular
                .into(binding.profileImage)
        }

        // Escolher uma frase motivacional aleatória
        val fraseAleatoria = escolherFraseAleatoria(sharedPref)
        binding.motivationalQuote.text = fraseAleatoria
    }

    private fun checkNotices(userId: Int, clubeId: String) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/checknotices.php"

        // Criar o JSON para enviar o ID do usuário e outras informações
        val jsonParams = JSONObject().apply {
            put("userId", userId)
            put("clube_id", clubeId)
        }

        // Criar a solicitação POST
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonParams,
            { response ->
                // Processar a resposta
                val hasNotices = response.getBoolean("hasNotices")
                if (hasNotices) {
                    val notices = response.getJSONArray("notices")
                    val noticesList = mutableListOf<Pair<String, String>>() // Para armazenar o título e descrição das notícias
                    for (i in 0 until notices.length()) {
                        val notice = notices.getJSONObject(i)
                        val title = notice.getString("TITULO")
                        val description = notice.getString("DESCRICAO")
                        noticesList.add(Pair(title, description)) // Adiciona título e descrição
                    }

                    // Exibir o card de notícias se houver notícias
                    binding.titleNews.visibility = View.VISIBLE
                    binding.noticiasCard.visibility = View.VISIBLE


                    // Atualizar o conteúdo do card com as notícias
                    // Vamos exibir o título e a descrição dentro do CardView
                    val formattedNotices = SpannableStringBuilder()

                    for (notice in noticesList) {
                        val title = notice.first
                        val description = notice.second

                        // Criar o texto formatado para a notícia
                        val noticeText = SpannableString("$title\n$description\n\n")

                        // Aplica o negrito e tamanho ao título (primeira parte do texto)
                        val titleStart = 0
                        val titleEnd = "$title\n".length
                        noticeText.setSpan(StyleSpan(Typeface.BOLD), titleStart, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        noticeText.setSpan(AbsoluteSizeSpan(16, true), titleStart, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                        // Aplica o tamanho normal à descrição (segunda parte do texto)
                        val descriptionStart = titleEnd
                        val descriptionEnd = noticeText.length
                        noticeText.setSpan(AbsoluteSizeSpan(14, true), descriptionStart, descriptionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                        // Adiciona o texto formatado ao StringBuilder
                        formattedNotices.append(noticeText)
                    }

                    // Exibir o texto formatado dentro do TextView
                    binding.newsInformation.text = formattedNotices


                } else {
                    // Ocultar o card de notícias se não houver notícias
                    binding.noticiasCard.visibility = View.GONE
                }
            },
            { error ->
                Log.e("UserFragment", "Erro ao verificar notícias: ${error.message}")
            }
        )

        // Adicionar a solicitação à fila
        Volley.newRequestQueue(requireContext()).add(jsonObjectRequest)
    }



    private fun formatHora(hora: String?): Any {
        return if (hora != null && hora.contains(":")) {
            val splitHora = hora.split(":")
            if (splitHora.size >= 2) {
                "${splitHora[0]}:${splitHora[1]}"  // Retorna a hora e minutos
            } else {
                "Formato de hora incompleto"
            }
        } else {
            "Formato de hora inválido"
        }
    }

    private fun escolherFraseAleatoria(sharedPref: SharedPreferences?): String {
        // Recupera o índice da frase armazenada nas SharedPreferences
        val indiceFrase = sharedPref?.getInt("frase_indice", -1)

        return if (indiceFrase != -1) {
            frasesMotivacionais[indiceFrase!!]
        } else {
            // Se não houver índice salvo, escolhe um índice aleatório
            val novoIndice = Random.nextInt(frasesMotivacionais.size)
            sharedPref?.edit()?.putInt("frase_indice", novoIndice)?.apply() // Salva o índice
            frasesMotivacionais[novoIndice]
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
