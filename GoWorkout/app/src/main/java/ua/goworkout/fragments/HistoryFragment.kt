package ua.goworkout.fragments

import ClassInfo
import HistoryAdapter
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
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ua.goworkout.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private val classList = mutableListOf<ClassInfo>()

    private lateinit var requestQueue: RequestQueue

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar RequestQueue do Volley
        requestQueue = Volley.newRequestQueue(requireContext())

        // Configurar RecyclerView
        historyAdapter = HistoryAdapter(classList) { classInfo ->
            openClassDetails(classInfo)
        }

        binding.recyclerViewClasses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        // Obter ID do utilizador do SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val idUser = sharedPref.getInt("id_user", 0) // Pega diretamente o valor inteiro

        if (idUser > 0) {  // Verifica se o id_user é maior que 0
            fetchClassHistory(idUser) // Passa o idUser diretamente para a função
        } else {
            Toast.makeText(requireContext(), "Utilizador não autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchClassHistory(idUser: Int) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/gethistory.php"

        val params = JSONObject()
        try {
            params.put("id_user", idUser)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // Usando JsonObjectRequest para obter um JSONObject como resposta
        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            { response ->
                try {
                    // Limpar a lista de aulas antes de adicionar os novos dados
                    classList.clear()

                    // Aqui obtemos o JSONArray diretamente do JSONObject
                    val classesArray: JSONArray = response.getJSONArray("data") // Supondo que os dados estão no campo "data"

                    for (i in 0 until classesArray.length()) {
                        val classObj = classesArray.getJSONObject(i)
                        val category = classObj.getString("categoria")
                        val date = classObj.getString("data_aula")
                        val cor = classObj.getString("cor_categoria")
                        val imagem = classObj.getString("imagem")

                        classList.add(ClassInfo(category, date,cor,imagem))
                    }

                    // Atualizar o RecyclerView
                    historyAdapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    Toast.makeText(requireContext(), "Erro ao processar os dados.", Toast.LENGTH_SHORT).show()
                    Log.e("HistoryFragment", "Erro JSON: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show()
                Log.e("HistoryFragment", "Erro Volley: ${error.message}")
            }
        )

        // Adicionar o pedido à fila
        requestQueue.add(request)
    }

    private fun openClassDetails(classInfo: ClassInfo) {
        // Navegar para outra tela ou abrir informações detalhadas
        Toast.makeText(requireContext(), classInfo.name, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
