package ua.goworkout.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
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
