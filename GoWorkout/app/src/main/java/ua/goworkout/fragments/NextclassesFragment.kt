package ua.goworkout.fragments

import NextClassInfo
import NextclassesAdapter
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
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ua.goworkout.databinding.FragmentNextclassesBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NextclassesFragment : Fragment() {

    private var _binding: FragmentNextclassesBinding? = null
    private val binding get() = _binding!!
    private var currentCalendar: Calendar = Calendar.getInstance()

    private lateinit var nextClassesAdapter: NextclassesAdapter
    private val classList = mutableListOf<NextClassInfo>()

    private lateinit var requestQueue: RequestQueue

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNextclassesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar RequestQueue do Volley
        requestQueue = Volley.newRequestQueue(requireContext())

        // Configurar RecyclerView
        nextClassesAdapter = NextclassesAdapter { nextClassInfo ->
            openClassDetails(nextClassInfo)
        }

        binding.recyclerViewClasses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = nextClassesAdapter
        }

        // Inicializar a data atual no TextView
        updateCurrentDate()

        // Setar ação para avançar 1 dia
        binding.ivNextDay.setOnClickListener {
            // Avançar 1 dia
            currentCalendar.add(Calendar.DAY_OF_MONTH, 1)
            updateCurrentDate()
            filterAulasBySelectedDate()
        }

        // Setar ação para voltar 1 dia
        binding.ivPreviousDay.setOnClickListener {
            // Voltar 1 dia
            currentCalendar.add(Calendar.DAY_OF_MONTH, -1)
            updateCurrentDate()
            filterAulasBySelectedDate()
        }

        // Obter ID do utilizador do SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val idUser = sharedPref.getInt("id_user", 0)

        if (idUser > 0) {
            fetchNextClasses(idUser)
        } else {
            Toast.makeText(requireContext(), "Utilizador não autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCurrentDate() {
        val locale = Locale.getDefault()
        val sdf = SimpleDateFormat(
            if (locale.language == "en") "EEEE, dd 'of' MMMM" else "EEEE, dd 'de' MMMM",
            locale
        )

        val formattedDate = sdf.format(currentCalendar.time)
        val formattedDay = formattedDate.split(",")[0].replaceFirstChar { it.titlecase(locale) }
        val finalDate = formattedDay + formattedDate.substring(formattedDay.length)

        binding.tvDayDate.text = finalDate

        // Verificar se a data é o dia atual
        if (isToday(currentCalendar)) {
            binding.ivPreviousDay.visibility = View.INVISIBLE // Esconder seta para trás no dia atual
        } else {
            binding.ivPreviousDay.visibility = View.VISIBLE // Mostrar seta para trás em outros dias
        }
    }

    // Função para verificar se a data é o dia atual
    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
    }


    private fun filterAulasBySelectedDate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "PT"))
        val selectedDate = sdf.format(currentCalendar.time)

        // Filtrar as aulas que coincidem com a data selecionada
        val filteredList = classList.filter { it.date.startsWith(selectedDate) }

        // Atualizar o Adapter com as aulas filtradas
        nextClassesAdapter.updateData(filteredList)
    }


    private fun fetchNextClasses(idUser: Int) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/getnextclasses.php"

        val params = JSONObject().apply {
            put("id_user", idUser)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            { response ->
                try {
                    // Adicionando o log para visualizar a resposta
                    Log.d("NextclassesFragment", "Resposta da API: $response")

                    classList.clear()

                    val classesArray: JSONArray = response.getJSONArray("data")
                    for (i in 0 until classesArray.length()) {
                        val classObj = classesArray.getJSONObject(i)
                        val category = classObj.getString("categoria")
                        val date = classObj.getString("data_aula")
                        val cor = classObj.getString("cor_categoria")
                        val imagem = classObj.getString("imagem")

                        classList.add(NextClassInfo(category, date, cor, imagem))
                    }

                    // Filtrar as aulas para o dia atual logo após carregar os dados
                    filterAulasBySelectedDate()

                } catch (e: JSONException) {
                    Toast.makeText(requireContext(), "Erro ao processar os dados.", Toast.LENGTH_SHORT).show()
                    Log.e("NextclassesFragment", "Erro JSON: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Erro ao carregar as próximas aulas.", Toast.LENGTH_SHORT).show()
                Log.e("NextclassesFragment", "Erro Volley: ${error.message}")
            }
        )

        requestQueue.add(request)
    }



    private fun openClassDetails(nextClassInfo: NextClassInfo) {
        Toast.makeText(requireContext(), nextClassInfo.name, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
