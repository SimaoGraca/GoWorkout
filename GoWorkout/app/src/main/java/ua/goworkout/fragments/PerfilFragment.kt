package ua.goworkout.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import ua.goworkout.EditarPasswordActivity
import ua.goworkout.EditarPerfilActivity
import ua.goworkout.R
import ua.goworkout.databinding.FragmentPerfilBinding
import android.widget.RatingBar
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    // Adicione o Volley requestQueue
    private val requestQueue by lazy { Volley.newRequestQueue(requireContext()) }

    private lateinit var bottomNav: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        // Pegue a referência para a Bottom Navigation
        bottomNav = requireActivity().findViewById(R.id.bottom_nav) // Ajuste conforme o id da sua Bottom Navigation

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Definir a ação de editar perfil
        binding.editProfileButton.setOnClickListener {
            val intent = Intent(requireContext(), EditarPerfilActivity::class.java)
            startActivity(intent)
        }

        // Definir a ação de editar senha
        binding.changePasswordButton.setOnClickListener {
            val intent = Intent(requireContext(), EditarPasswordActivity::class.java)
            startActivity(intent)
        }

        // Referências aos elementos do layout
        val ratingBar = binding.ratingBar
        val feedbackMessage = binding.feedbackMessage
        val sendFeedbackButton = binding.sendFeedbackButton

        // Ação ao clicar no botão Enviar
        sendFeedbackButton.setOnClickListener {
            val rating = ratingBar.rating
            val feedback = feedbackMessage.text.toString()

            val sharedPref = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
            val userId = sharedPref?.getInt("id_user", -1)  // Verifique se o id_user existe
            val clubeId = sharedPref?.getString("clube_id", "")  // Clube id é uma string, então pode usar isNotEmpty

            if (userId != -1 && !clubeId.isNullOrEmpty()) {
                submitFeedback(userId.toString(), rating, feedback, clubeId)
            } else {
                Toast.makeText(requireContext(), "ID do user ou do clube não encontrado", Toast.LENGTH_SHORT).show()
            }
        }

        // Ocultar a Bottom Navigation quando o campo de feedback (EditText) receber o foco
        feedbackMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                bottomNav.visibility = View.GONE // Ocultar Bottom Navigation
            }
        }

        // Fechar o teclado ao clicar fora do campo de feedback
        binding.root.setOnTouchListener { _, _ ->
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0) // Fecha o teclado

            // Restaurar a Bottom Navigation quando o teclado for fechado
            bottomNav.visibility = View.VISIBLE

            false // Retorna falso para que o toque possa ser propagado para outros elementos
        }

        // Restaurar a Bottom Navigation quando o teclado for fechado
        binding.root.viewTreeObserver.addOnPreDrawListener {
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val isKeyboardVisible = imm.isAcceptingText
            if (!isKeyboardVisible) {
                // Se o teclado estiver fechado, restaurar a Bottom Navigation
                bottomNav.visibility = View.VISIBLE
            }
            true // Retorna verdadeiro para continuar a renderização
        }
    }

    private fun submitFeedback(userId: String, rating: Float, feedback: String, clubeId: String) {
        val url = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/submitFeedbackclube.php"

        val params = hashMapOf<String, String>(
            "userId" to userId,
            "rating" to rating.toString(),
            "feedback" to feedback,
            "clube_id" to clubeId
        )

        val jsonObject = JSONObject(params as Map<*, *>?)

        Log.d("FeedbackRequest", "JSON being sent: $jsonObject")

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            { response ->
                try {
                    Log.d("FeedbackResponse", "Response: $response")
                    Toast.makeText(requireContext(), "Obrigado pelo seu feedback!", Toast.LENGTH_SHORT).show()

                    // Restaurar a Bottom Navigation após o envio do feedback
                    bottomNav.visibility = View.VISIBLE

                } catch (e: JSONException) {
                    Log.e("FeedbackError", "Erro ao processar JSON da resposta: ${e.message}")
                    Toast.makeText(requireContext(), "Erro ao processar resposta", Toast.LENGTH_SHORT).show()

                    // Restaurar a Bottom Navigation em caso de erro
                    bottomNav.visibility = View.VISIBLE
                }
            },
            { error ->
                Log.e("FeedbackError", "Error: ${error.message}")
                if (error.networkResponse != null) {
                    Log.e("FeedbackError", "Status Code: ${error.networkResponse.statusCode}")
                    Log.e("FeedbackError", "Error Body: ${String(error.networkResponse.data)}")
                }
                Toast.makeText(requireContext(), "Erro ao enviar feedback", Toast.LENGTH_SHORT).show()

                // Restaurar a Bottom Navigation em caso de erro
                bottomNav.visibility = View.VISIBLE
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    override fun onResume() {
        super.onResume()

        bottomNav.visibility = View.VISIBLE

        val sharedPref = activity?.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
        val nome = sharedPref?.getString("nome", "User")
        val fotoPerfil = sharedPref?.getString("foto_perfil", null)

        binding.userName.text = nome

        if (fotoPerfil != null) {
            val imageUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem/$fotoPerfil?timestamp=${System.currentTimeMillis()}"
            Log.d("PerfilFragment", "Link da imagem de perfil: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.profileImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
