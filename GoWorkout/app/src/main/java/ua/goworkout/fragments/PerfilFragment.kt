package ua.goworkout.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ua.goworkout.databinding.FragmentPerfilBinding

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Buscar dados das SharedPreferences
        val sharedPref = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val nome = sharedPref?.getString("nome", "User")
        val fotoPerfil = sharedPref?.getString("foto_perfil", null)

        // Definir o nome no TextView
        binding.userName.text = nome

        // Carregar a imagem de perfil usando Glide com recorte circular
        if (fotoPerfil != null) {
            val imageUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem/$fotoPerfil"
            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform()) // Aplica o recorte circular
                .into(binding.profileImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
