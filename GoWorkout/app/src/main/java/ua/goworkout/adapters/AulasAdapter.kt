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
                // Verificar se a aula está cheia
                val lotacaoUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/api/verificarlotacaoaula.php"
                val lotacaoParams = JSONObject().apply {
                    put("id_aula", aula.id_aula)
                }

                val queue = Volley.newRequestQueue(context)

                val lotacaoRequest = JsonObjectRequest(
                    Request.Method.POST, lotacaoUrl, lotacaoParams,
                    { lotacaoResponse ->
                        val isLotacaoCheia = lotacaoResponse.optBoolean("isLotacaoCheia", false)

                        // Se a aula estiver cheia, não permite marcar e exibe uma mensagem
                        if (isLotacaoCheia) {
                            Log.d("MarcacaoFragment", "Aula com ID ${aula.id_aula} está cheia.")
                            Toast.makeText(context, "Esta aula já está cheia", Toast.LENGTH_SHORT).show()
                        } else {
                            // Se a aula não estiver cheia, pode atualizar o estado de 'isMarked'
                            aula.isMarked = true

                            // Chama o listener para fazer a operação de marcação
                            listener.onAulaCheckClick(aula.id_aula, aula.userId, true)

                            // Notifica o RecyclerView para atualizar o item da aula específico
                            notifyItemChanged(position)
                        }
                    },
                    { error ->
                        Log.e("MarcacaoFragment", "Erro ao verificar lotação da aula: $error")
                        Toast.makeText(context, "Erro ao verificar lotação da aula", Toast.LENGTH_SHORT).show()
                    }
                )

                // Adiciona a requisição para o queue
                queue.add(lotacaoRequest)
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
