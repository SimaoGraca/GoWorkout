package ua.goworkout.fragments

import Aula
import AulasAdapter
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import ua.goworkout.NotificationReceiver
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
            Toast.makeText(context, "User ou clube inválido", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateCurrentDate() {
        // Verificar o idioma atual do dispositivo
        val locale = Locale.getDefault()

        // Definir o formato da data com base no idioma
        val sdf = if (locale.language == "en") {
            // Se o idioma for inglês
            SimpleDateFormat("EEEE, dd 'of' MMMM", Locale("en", "US"))
        } else {
            // Se o idioma for português (ou outro)
            SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "PT"))
        }

        // Formatando a data
        val formattedDate = sdf.format(currentCalendar.time)

        // Dividir o nome do dia da data para capitalizar a primeira letra
        val formattedDay = formattedDate.split(",")[0].trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }

        // Combinar o nome do dia com o resto da data
        val formattedFinalDate = formattedDay + formattedDate.substring(formattedDay.length)

        // Exibir a data formatada no TextView
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
                        val corcategoria = aula.getString("cor_categoria")
                        aulasList.add(Aula(id_aula, id_user, nomeCategoria, horario, instrutor_nome, duracao,corcategoria, false))
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

            // Recuperar o ID do user armazenado
            val storedUserId = jsonMarcacoes.optInt("id_user", -1)

            // Recuperar o ID do user atualmente logado do SharedPreferences "pmLogin"
            val currentUserId = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)?.getInt("id_user", -1)

            // Verificar se o ID do user armazenado é igual ao ID do usuário logado
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

        // Adicionar o ID do user ao JSON
        jsonMarcacoes.put("id_user", userId)

        editor.putString("marcacoes", jsonMarcacoes.toString())
        editor.apply()
    }


    override fun onAulaCheckClick(aulaId: Int, userId: Int, isMarked: Boolean) {
        // Encontrar a aula correspondente
        val aula = todasAulas.find { it.id_aula == aulaId }
        if (aula != null) {
            try {
                // Formatar o horário da aula e o horário atual
                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val aulaDateTime = dateTimeFormat.parse(aula.horario) // Horário da aula
                val currentDateTime = Date() // Data e hora atual

                Log.d("MarcacaoFragment", "Horário da aula: $aulaDateTime")
                Log.d("MarcacaoFragment", "Horário atual: $currentDateTime")

                // Verificar se a aula já passou
                if (aulaDateTime != null && aulaDateTime.before(currentDateTime)) {
                    // A aula já passou, então não pode ser marcada ou desmarcada
                    if (isMarked) {
                        Log.d("MarcacaoFragment", "A aula já passou. Não pode ser marcada.")
                        Toast.makeText(context, "Esta aula já não pode ser marcada", Toast.LENGTH_SHORT).show()
                        aula.isMarked = false
                    } else {
                        Log.d("MarcacaoFragment", "A aula já passou. Não pode ser desmarcada.")
                        Toast.makeText(context, "Esta aula já não pode ser desmarcada", Toast.LENGTH_SHORT).show()
                        aula.isMarked = true
                    }
                    return
                } else {
                    Log.d("MarcacaoFragment", "A aula está disponível para marcação/desmarcação.")
                }
            } catch (e: Exception) {
                Log.e("MarcacaoFragment", "Erro ao processar a data e hora da aula: ${e.message}", e)
                Toast.makeText(context, "Erro ao verificar a data e hora da aula", Toast.LENGTH_SHORT).show()
                return
            }

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
                    val idMarcacao = marcacoesMap[aulaId]
                    if (idMarcacao != null) {
                        put("id_marcacao", idMarcacao)
                        Log.d("MarcacaoFragment", "Enviando id_marcacao ao desmarcar: $idMarcacao")
                    } else {
                        Log.d("MarcacaoFragment", "id_marcacao não encontrado para a aula $aulaId")
                    }
                }
            }

            Log.d("MarcacaoFragment", "Enviando JSON para API: $jsonParams")

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
                        checkAndRequestNotificationPermission()

                        val id = response.optInt("id_marcacao", -1)
                        if (id != -1) {
                            aula.isMarked = true
                            marcacoesMap[aulaId] = id
                            Log.d("MarcacaoFragment", "ID da marcação recebida: $id")
                            saveMarcacoesToPreferences(userId)

                            val position = todasAulas.indexOf(aula)
                            if (position >= 0) {
                                aulasAdapter.notifyItemChanged(position)
                            }

                            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            dateTimeFormat.timeZone = TimeZone.getDefault()
                            val aulaDateTime = dateTimeFormat.parse(aula.horario)
                            val notificationTime = aulaDateTime.time - 3600000

                            val notificationTimeFormatted = dateTimeFormat.format(Date(notificationTime))
                            Log.d("MarcacaoFragment", "Notificação agendada para 1 hora antes: $notificationTimeFormatted")

                            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            val intent = Intent(context, NotificationReceiver::class.java).apply {
                                putExtra("nome_aula", aula.nomeCategoria)
                                putExtra("horario_aula", aula.horario)
                            }

                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                aulaId,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                context?.startActivity(intent)
                                Toast.makeText(context, "Permissão necessária para agendar alarmes exatos", Toast.LENGTH_SHORT).show()
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        notificationTime,
                                        pendingIntent
                                    )
                                } else {
                                    alarmManager.setExact(
                                        AlarmManager.RTC_WAKEUP,
                                        notificationTime,
                                        pendingIntent
                                    )
                                }
                            }
                        } else {
                            Log.d("MarcacaoFragment", "ID da marcação não recebido ou inválido")
                        }
                    } else {
                        val removedId = marcacoesMap.remove(aulaId)
                        aula.isMarked = false
                        Log.d("MarcacaoFragment", "Aula desmarcada. ID da marcação removido: $removedId")
                        saveMarcacoesToPreferences(userId)

                        val position = todasAulas.indexOf(aula)
                        if (position >= 0) {
                            aulasAdapter.notifyItemChanged(position)
                        }

                        val intent = Intent(context, NotificationReceiver::class.java)
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            aulaId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        alarmManager.cancel(pendingIntent)
                        Log.d("MarcacaoFragment", "Notificação cancelada para a aula: $aulaId")
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
            Log.d("MarcacaoFragment", "Aula não encontrada para o ID: $aulaId")
            Toast.makeText(context, "Aula não encontrada", Toast.LENGTH_SHORT).show()
        }
    }


    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.areNotificationsEnabled()) {
                // Se a permissão não foi concedida, solicitar
                showPermissionRequestDialog()
            } else {
                Log.d("MarcacaoFragment", "Permissão para notificações já concedida.")
            }
        } else {
            // No Android < 13, a permissão é concedida automaticamente
            Log.d("MarcacaoFragment", "Permissão para notificações não é necessária.")
        }
    }

    fun showPermissionRequestDialog() {
        AlertDialog.Builder(context)
            .setTitle("Permissão para Notificações")
            .setMessage("Gostaria de receber notificações para a sua aula?")
            .setPositiveButton("Sim") { _, _ ->
                // Leva o usuário para as configurações de permissão
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context?.packageName)
                startActivity(intent)
            }
            .setNegativeButton("Não") { dialog, _ ->
                dialog.dismiss()
                Log.d("MarcacaoFragment", "User recusou a permissão para notificações.")
            }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
