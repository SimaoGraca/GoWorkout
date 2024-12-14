package ua.goworkout.fragments

import Aula
import AulasAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ua.goworkout.databinding.FragmentMarcacoesBinding
import java.text.SimpleDateFormat
import java.util.*

class MarcacaoFragment : Fragment(), AulasAdapter.OnAulaCheckClickListener {

    private var _binding: FragmentMarcacoesBinding? = null
    private val binding get() = _binding!!

    private var currentCalendar: Calendar = Calendar.getInstance()
    private lateinit var aulasAdapter: AulasAdapter
    private var todasAulas: List<Aula> = listOf() // Lista para armazenar todas as aulas

    private val marcacoesMap = mutableMapOf<Int, Int?>() // Mapeia aulaId para idMarcacao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMarcacoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        aulasAdapter = AulasAdapter(listOf(), this)
        binding.recyclerViewAulas.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewAulas.adapter = aulasAdapter

        // Inicializar a data atual no TextView
        updateCurrentDate()

        // Configurar ação das setas
        binding.ivPreviousDay.setOnClickListener {
            // Retroceder 1 dia
            currentCalendar.add(Calendar.DAY_OF_MONTH, -1)
            updateCurrentDate()
            // Filtrar aulas para o novo dia
            filterAulasBySelectedDate()
        }

        binding.ivNextDay.setOnClickListener {
            // Avançar 1 dia
            currentCalendar.add(Calendar.DAY_OF_MONTH, 1)
            updateCurrentDate()
            // Filtrar aulas para o novo dia
            filterAulasBySelectedDate()
        }

        // Recuperar dados do usuário do SharedPreferences
        val sharedPref = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val idUser = sharedPref?.getInt("id_user", -1)
        val clubeId = sharedPref?.getString("clube_id", "")

        // Verificar se os dados são válidos
        if (idUser != null && idUser != -1 && !clubeId.isNullOrEmpty()) {
            val clubeIdInt = clubeId.toIntOrNull()
            if (clubeIdInt != null) {
                getAulasDisponiveis(idUser, clubeIdInt)
            } else {
                Toast.makeText(context, "ID do clube inválido", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Usuário ou clube inválido", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "PT"))
        val formattedDate = sdf.format(currentCalendar.time)
        val formattedDay = formattedDate.split(",")[0].trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
        val formattedFinalDate = formattedDay + formattedDate.substring(formattedDay.length)
        binding.tvDayDate.text = formattedFinalDate
    }

    private fun getAulasDisponiveis(userId: Int, clubeId: Int) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/getaulas.php"

        // Criar o JSON para enviar o ID do usuário e outras informações
        val jsonParams = JSONObject().apply {
            put("id_user", userId)
            put("clube_id", clubeId)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonParams,
            { response ->
                try {
                    val aulasArray = response.getJSONArray("aulas")
                    val aulasList = mutableListOf<Aula>()

                    // Iterar sobre as aulas e adicioná-las à lista
                    for (i in 0 until aulasArray.length()) {
                        val aula = aulasArray.getJSONObject(i)
                        val id_user = userId
                        val id_aula = aula.getInt("ID_AULA")
                        val nomeCategoria = aula.getString("nome_categoria")
                        val horario = aula.getString("DATA_AULA")
                        val instrutor_nome = aula.getString("instrutor_nome")
                        val duracao = aula.getInt("DURACAO")
                        aulasList.add(Aula(id_aula, id_user, nomeCategoria, horario, instrutor_nome, duracao, false))
                    }

                    // Armazenar todas as aulas para uso posterior
                    todasAulas = aulasList

                    // Carregar as marcações do SharedPreferences
                    loadMarcacoesFromPreferences()

                    // Filtrar as aulas para o dia selecionado
                    filterAulasBySelectedDate()

                } catch (e: Exception) {
                    Log.e("MarcacaoFragment", "Erro ao processar a resposta da API", e)
                    Toast.makeText(context, "Erro ao processar dados", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("MarcacaoFragment", "Erro na requisição: $error")
                Toast.makeText(context, "Erro ao buscar aulas", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
    }

    private fun filterAulasBySelectedDate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "PT"))
        val selectedDate = sdf.format(currentCalendar.time)

        // Filtrar as aulas que coincidem com a data selecionada
        val aulasList = todasAulas.filter {
            val aulaDate = it.horario.split(" ")[0] // 'yyyy-MM-dd'
            aulaDate == selectedDate
        }

        // Atualizar o Adapter com as aulas filtradas
        aulasAdapter.updateData(aulasList)

        // Marcar as aulas que foram previamente marcadas
        aulasList.forEach {
            val isMarked = marcacoesMap.containsKey(it.id_aula)
            it.isMarked = isMarked
        }

        // Atualizar o Adapter novamente com o estado das aulas
        aulasAdapter.updateData(aulasList)
    }

    private fun loadMarcacoesFromPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("MarcacoesPrefs", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("marcacoes", null)

        if (!jsonString.isNullOrEmpty()) {
            val jsonMarcacoes = JSONObject(jsonString)

            // Recuperar o ID do usuário armazenado
            val storedUserId = jsonMarcacoes.optInt("id_user", -1)

            // Recuperar o ID do usuário atualmente logado do SharedPreferences "pmLogin"
            val currentUserId = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)?.getInt("id_user", -1)

            // Verificar se o ID do usuário armazenado é igual ao ID do usuário logado
            if (storedUserId == currentUserId) {
                for (key in jsonMarcacoes.keys()) {
                    if (key != "id_user") {
                        val aulaId = key.toInt()
                        val idMarcacao = jsonMarcacoes.getInt(key)
                        marcacoesMap[aulaId] = idMarcacao
                    }
                }
            }
        }
    }


    private fun saveMarcacoesToPreferences(userId: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("MarcacoesPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val jsonMarcacoes = JSONObject()
        for ((aulaId, idMarcacao) in marcacoesMap) {
            jsonMarcacoes.put(aulaId.toString(), idMarcacao)
        }

        // Adicionar o ID do usuário ao JSON
        jsonMarcacoes.put("id_user", userId)

        editor.putString("marcacoes", jsonMarcacoes.toString())
        editor.apply()
    }


    override fun onAulaCheckClick(aulaId: Int, userId: Int, isMarked: Boolean) {
        // Encontrar a aula correspondente
        val aula = todasAulas.find { it.id_aula == aulaId }
        if (aula != null) {
            // Obter a data da aula
            val aulaDateStr = aula.horario.split(" ")[0] // 'yyyy-MM-dd'

            // Formatar a data de hoje
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            // Verificar se a aula já passou
            if (aulaDateStr < currentDate) {
                // Exibir mensagem de erro se a aula já passou
                Toast.makeText(context, "Esta aula já não está disponível para marcação", Toast.LENGTH_SHORT).show()
                return // Não continua com a marcação
            }

            // Caso contrário, prosseguir com a lógica original de marcação
            val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val jsonParams = JSONObject().apply {
                put("id_user", userId)
                put("id_aula", aula.id_aula)
                put("nome_categoria", aula.nomeCategoria)
                put("horario", aula.horario)
                put("instrutor_nome", aula.instrutor_nome)
                put("duracao", aula.duracao)
                put("data_marcacao", currentDateTime)
                put("isMarked", isMarked)

                if (!isMarked) {
                    // Adiciona o id_marcacao ao JSON apenas quando desmarcando
                    val idMarcacao = marcacoesMap[aulaId]
                    if (idMarcacao != null) {
                        put("id_marcacao", idMarcacao)
                        Log.d("MarcacaoFragment", "Enviando id_marcacao ao desmarcar: $idMarcacao")
                    } else {
                        Log.d("MarcacaoFragment", "id_marcacao não encontrado para a aula $aulaId")
                    }
                }
            }

            Log.d("MarcacaoFragment", "Enviando JSON para API: $jsonParams") // Log dos parâmetros

            val url = if (isMarked) {
                "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/marcaraula.php"
            } else {
                "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/desmarcaraula.php"
            }

            val queue = Volley.newRequestQueue(requireContext())

            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, jsonParams,
                { response ->
                    Log.d("MarcacaoFragment", "Resposta recebida da API: $response")

                    if (isMarked) {
                        val id = response.optInt("id_marcacao", -1)
                        if (id != -1) {
                            marcacoesMap[aulaId] = id
                            Log.d("MarcacaoFragment", "ID da marcação recebida: $id") // Log do id da marcação
                            saveMarcacoesToPreferences(userId) // Salva no SharedPreferences
                        } else {
                            Log.d("MarcacaoFragment", "ID da marcação não recebido ou inválido")
                        }
                    } else {
                        val removedId = marcacoesMap.remove(aulaId)
                        Log.d("MarcacaoFragment", "Aula desmarcada. ID da marcação removido: $removedId") // Log da remoção
                        saveMarcacoesToPreferences(userId) // Salva no SharedPreferences
                    }

                    Toast.makeText(context, "Operação realizada com sucesso", Toast.LENGTH_SHORT).show()
                },
                { error ->
                    Log.e("MarcacaoFragment", "Erro ao realizar operação: $error")
                    Toast.makeText(context, "Erro ao realizar operação", Toast.LENGTH_SHORT).show()
                }
            )

            queue.add(jsonObjectRequest)
        } else {
            Log.d("MarcacaoFragment", "Aula não encontrada para o ID: $aulaId") // Log se a aula não for encontrada
            Toast.makeText(context, "Aula não encontrada", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
