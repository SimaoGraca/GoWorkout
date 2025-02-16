import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.android.volley.Request
import ua.goworkout.R
import java.text.SimpleDateFormat
import java.util.*

data class Aula(
    val id_aula: Int,
    val userId: Int,
    val nomeCategoria: String,
    val horario: String,
    val instrutor_nome: String,
    val duracao: Int,
    val cor: String,
    val imagem_categoria: String,
    var isMarked: Boolean
)

class AulasAdapter(
    private var aulasList: List<Aula>?,
    private val listener: OnAulaCheckClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_AULA = 0
    private val VIEW_TYPE_EMPTY = 1

    class AulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeCategoriaTextView: TextView = itemView.findViewById(R.id.tvNomeCategoria)
        val horarioDuracaoTextView: TextView = itemView.findViewById(R.id.tvHorario)
        val instrutorTextView: TextView = itemView.findViewById(R.id.tvInstrutor)
        val buttonMarcar: ImageButton = itemView.findViewById(R.id.btnCheck)
        val buttonDesmarcar: ImageButton = itemView.findViewById(R.id.btnUncheck)
        val linhacor: View = itemView.findViewById(R.id.vertical_line)
    }

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emptyTextView: TextView = itemView.findViewById(R.id.tvempty)
    }

    interface OnAulaCheckClickListener {
        fun onAulaCheckClick(aulaId: Int, userId: Int, isMarked: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AULA) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_aula, parent, false)
            AulaViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_empty, parent, false)
            EmptyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_AULA) {
            val aula = aulasList!![position]

            // Formatar horário
            val originalDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val targetDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = originalDateFormat.parse(aula.horario)
            val formattedHorario = targetDateFormat.format(date)

            // Formatar a duração
            val formattedDuracao = "${aula.duracao}'"

            // Acesse o contexto diretamente através do holder (ViewHolder)
            val aulaHolder = holder as AulaViewHolder
            val context = aulaHolder.itemView.context // Contexto do itemView dentro do ViewHolder

            // Atualizar os TextViews
            aulaHolder.nomeCategoriaTextView.text = aula.nomeCategoria
            aulaHolder.horarioDuracaoTextView.text = "$formattedHorario | $formattedDuracao"
            // Aqui usamos context.getString() para acessar o texto
            aulaHolder.instrutorTextView.text = "${context.getString(R.string.text_instrutor)}: ${aula.instrutor_nome}"

            try {
                val corLinha = Color.parseColor(aula.cor) // Conversão da string hexadecimal para cor
                aulaHolder.linhacor.setBackgroundColor(corLinha)
            } catch (e: IllegalArgumentException) {
                aulaHolder.linhacor.setBackgroundColor(Color.GRAY) // Cor padrão em caso de erro
            }

            aulaHolder.buttonMarcar.setOnClickListener {
                // Formatar o horário da aula
                val originalDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val classDate = originalDateFormat.parse(aula.horario)

                // Verificar se o horário da aula já passou
                val currentTime = Date()

                if (classDate != null && classDate.after(currentTime)) {

                    // Recuperar dados do user do SharedPreferences
                    val sharedPref = aulaHolder.itemView.context.getSharedPreferences("pmLogin", Context.MODE_PRIVATE)
                    val idUser = sharedPref?.getInt("id_user", -1) ?: -1  // Caso id_user não esteja presente, retorna -1

                    // Verificar o instrutor e a lotação da aula
                    val lotacaoUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/verificarlotacaoaula.php"
                    val lotacaoParams = JSONObject().apply {
                        put("id_aula", aula.id_aula)
                        put("id_user", idUser)
                    }

                    // Log dos parâmetros enviados para a verificação do instrutor e da lotação
                    Log.d("MarcacaoFragment", "Enviando para a API: id_aula = ${aula.id_aula}, id_user = $idUser")

                    val queue = Volley.newRequestQueue(aulaHolder.itemView.context)

                    val lotacaoRequest = JsonObjectRequest(
                        Request.Method.POST, lotacaoUrl, lotacaoParams,
                        { lotacaoResponse ->
                            // Verifica se o utilizador é o instrutor
                            val isInstrutor = lotacaoResponse.optBoolean("isInstrutor", false)

                            // Log do valor de 'isInstrutor'
                            Log.d("MarcacaoFragment", "isInstrutor: $isInstrutor")

                            if (isInstrutor) {
                                // Se o utilizador for o instrutor da aula, exibe a mensagem e interrompe o processo
                                Log.d("MarcacaoFragment", "O utilizador é o instrutor da aula")
                                Toast.makeText(aulaHolder.itemView.context, "Você é o instrutor desta aula, não pode marcar", Toast.LENGTH_SHORT).show()
                            } else {
                                // Caso o utilizador não seja o instrutor, verifica a lotação
                                val isLotacaoCheia = lotacaoResponse.optBoolean("isLotacaoCheia", false)

                                // Log do valor de 'isLotacaoCheia'
                                Log.d("MarcacaoFragment", "isLotacaoCheia: $isLotacaoCheia")

                                if (isLotacaoCheia) {
                                    Log.d("MarcacaoFragment", "Aula com ID ${aula.id_aula} está cheia.")
                                    Toast.makeText(aulaHolder.itemView.context, "Esta aula já está cheia", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Se a aula não estiver cheia, pode atualizar o estado de 'isMarked'
                                    aula.isMarked = true

                                    // Chama o listener para fazer a operação de marcação
                                    listener.onAulaCheckClick(aula.id_aula, aula.userId, true)

                                    // Notifica o RecyclerView para atualizar o item da aula específico
                                    notifyItemChanged(position)
                                }
                            }
                        },
                        { error ->
                            Log.e("MarcacaoFragment", "Erro ao verificar lotação da aula: $error")
                            Toast.makeText(aulaHolder.itemView.context, "Erro ao verificar lotação da aula", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Adiciona a requisição para o queue
                    queue.add(lotacaoRequest)
                } else {
                    // Se a aula já passou, não permite marcar e exibe uma mensagem
                    Log.d("MarcacaoFragment", "Aula com ID ${aula.id_aula} já passou.")
                    Toast.makeText(aulaHolder.itemView.context, "Esta aula já não pode ser marcada", Toast.LENGTH_SHORT).show()
                }
            }





            aulaHolder.buttonDesmarcar.setOnClickListener {
                // Atualiza o estado de 'isMarked' da aula
                aula.isMarked = false

                // Chama o listener para fazer a operação de desmarcação
                listener.onAulaCheckClick(aula.id_aula, aula.userId, false)

                // Notifica o RecyclerView para atualizar o item da aula específico
                notifyItemChanged(position)
            }

            // Atualizar visibilidade dos botões com base no valor de 'isMarked'
            if (aula.isMarked) {
                aulaHolder.buttonMarcar.visibility = View.GONE
                aulaHolder.buttonDesmarcar.visibility = View.VISIBLE
            } else {
                aulaHolder.buttonMarcar.visibility = View.VISIBLE
                aulaHolder.buttonDesmarcar.visibility = View.GONE
            }
        } else {
            // Mostrar a mensagem "Não existe aulas para esse dia"
            val emptyHolder = holder as EmptyViewHolder
            emptyHolder.emptyTextView.text = "Não existe aulas para este dia"
        }
    }

    override fun getItemCount(): Int {
        return if (aulasList.isNullOrEmpty()) 1 else aulasList!!.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (aulasList.isNullOrEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_AULA
    }

    fun updateData(newAulasList: List<Aula>) {
        aulasList = newAulasList
        notifyDataSetChanged()
    }
}
