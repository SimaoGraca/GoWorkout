import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

            // Atualizar os TextViews
            val aulaHolder = holder as AulaViewHolder
            aulaHolder.nomeCategoriaTextView.text = aula.nomeCategoria
            aulaHolder.horarioDuracaoTextView.text = "$formattedHorario | $formattedDuracao"
            aulaHolder.instrutorTextView.text = "Instrutor: ${aula.instrutor_nome}"

            // Verificar se a aula já passou
            val aulaDateStr = aula.horario.split(" ")[0] // 'yyyy-MM-dd'
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val isPast = aulaDateStr < currentDate

            // Atualizar visibilidade dos botões
            if (aula.isMarked) {
                aulaHolder.buttonMarcar.visibility = View.GONE
                aulaHolder.buttonDesmarcar.visibility = View.VISIBLE
            } else {
                aulaHolder.buttonMarcar.visibility = View.VISIBLE
                aulaHolder.buttonDesmarcar.visibility = View.GONE
            }

            // Adicionar listeners aos botões
            aulaHolder.buttonMarcar.setOnClickListener {
                if (!isPast) {
                    aula.isMarked = true
                    listener.onAulaCheckClick(aula.id_aula, aula.userId, true)
                    notifyItemChanged(position)
                }
            }

            aulaHolder.buttonDesmarcar.setOnClickListener {
                if (!isPast) {
                    aula.isMarked = false
                    listener.onAulaCheckClick(aula.id_aula, aula.userId, false)
                    notifyItemChanged(position)
                }
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
