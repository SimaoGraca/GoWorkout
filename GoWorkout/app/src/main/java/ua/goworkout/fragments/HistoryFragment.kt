package ua.goworkout.fragments

import ClassInfo
import HistoryAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ua.goworkout.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val classList = listOf(
        ClassInfo("Cycle", "25/04/2024"),
        ClassInfo("Yoga", "26/04/2024"),
        ClassInfo("Pilates", "27/04/2024")
    )

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

        // Configurar RecyclerView
        binding.recyclerViewClasses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoryAdapter(classList) { classInfo ->
                // Clique em um item da lista
                openClassDetails(classInfo)
            }
        }
    }

    private fun openClassDetails(classInfo: ClassInfo) {
        // Navegar para outra tela ou abrir informações detalhadas
        // Exemplo: Toast.makeText(requireContext(), classInfo.name, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
