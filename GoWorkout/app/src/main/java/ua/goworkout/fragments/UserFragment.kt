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
import com.google.gson.Gson
import ua.goworkout.databinding.FragmentUserBinding
import kotlin.random.Random

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val frasesMotivacionais = arrayOf(
        "Acredite em si mesmo e em todo o seu potencial.",
        "Cada dia é uma nova oportunidade para ser melhor.",
        "O sucesso é a soma de pequenos esforços repetidos diariamente.",
        "Nunca desista dos seus sonhos, acredite que são possíveis.",
        "Grandes conquistas começam com pequenas ações."
    )

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
            binding.welcomeMessage.text = "Bem-Vindo(a), $nome"
        } else {
            binding.welcomeMessage.text = "User não encontrado"
        }

        // Informações do ginásio
        binding.gymInformation.text = "Clube: $clubeNome\nMorada: $endereco\nCidade: $cidade"

        // LOG - Verificando os dados recuperados de SharedPreferences
        Log.d("UserFragment", "idUser: $idUser, Nome: $nome, Clube: $clubeNome,Clube_id: $id_clube, Cidade: $cidade")

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

        // Carregar a imagem de perfil usando Glide
        if (fotoPerfil != null) {
            val imageUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem/$fotoPerfil"
            Glide.with(this)
                .load(imageUrl)
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
                return "${splitHora[0]}:${splitHora[1]}"  // Retorna a hora e minutos
            } else {
                // Caso a string contenha o ":", mas não tenha pelo menos 2 partes (como "07:" ou ":00")
                return "Formato de hora incompleto"
            }
        } else {
            // Caso a hora seja null ou não contenha ":"
            return "Formato de hora inválido"
        }
    }

    // Função para escolher uma frase motivacional aleatória
    private fun escolherFraseAleatoria(sharedPref: SharedPreferences?): String {
        val indiceFrase = sharedPref?.getInt("frase_indice", -1)
        return if (indiceFrase != -1) {
            frasesMotivacionais[indiceFrase!!]
        } else {
            val novoIndice = Random.nextInt(frasesMotivacionais.size)
            sharedPref?.edit()?.putInt("frase_indice", novoIndice)?.apply()
            frasesMotivacionais[novoIndice]
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
