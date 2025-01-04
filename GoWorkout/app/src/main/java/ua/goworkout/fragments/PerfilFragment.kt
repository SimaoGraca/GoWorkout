package ua.goworkout.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import ua.goworkout.EditarPasswordActivity
import ua.goworkout.EditarPerfilActivity
import ua.goworkout.R
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

            // Adicionar o log do link da imagem
            Log.d("PerfilFragment", "Link da imagem de perfil: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profileImage)
        }

        // Clique no botão Editar Perfil
        binding.editProfileButton.setOnClickListener {
            val intent = Intent(requireContext(), EditarPerfilActivity::class.java)
            startActivity(intent)
        }

        // Clique no botão Editar Perfil
        binding.changePasswordButton.setOnClickListener {
            val intent = Intent(requireContext(), EditarPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // Recarregar os dados das SharedPreferences sempre que o Fragment for exibido novamente
        val sharedPref = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val nome = sharedPref?.getString("nome", "User")
        val fotoPerfil = sharedPref?.getString("foto_perfil", null)

        // Definir o nome no TextView
        binding.userName.text = nome

        // Carregar a imagem de perfil usando Glide com recorte circular
        if (fotoPerfil != null) {
            // Adicionar um parâmetro de cache-busting à URL
            val imageUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem/$fotoPerfil?timestamp=${System.currentTimeMillis()}"

            // Log para verificar o link atualizado
            Log.d("PerfilFragment", "Link da imagem de perfil: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Ignora o cache no disco
                .skipMemoryCache(true) // Ignora o cache na memória
                .into(binding.profileImage)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
