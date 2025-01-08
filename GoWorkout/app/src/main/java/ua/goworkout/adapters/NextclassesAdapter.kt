import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ua.goworkout.R
import ua.goworkout.databinding.ItemClassBinding
import java.text.SimpleDateFormat
import java.util.Locale

data class NextClassInfo(
    val name: String,
    val date: String,
    val cor: String,
    val imagem: String
)

class NextclassesAdapter(
    private val onItemClick: (NextClassInfo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val baseUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem_categoria/"
    private val classList = mutableListOf<NextClassInfo>()

    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_EMPTY = 0
    }

    inner class ClassViewHolder(private val binding: ItemClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(classInfo: NextClassInfo) {
            with(binding) {
                // Formatar a data para exibir apenas a hora
                val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val targetFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                try {
                    val date = originalFormat.parse(classInfo.date)
                    val formattedTime = targetFormat.format(date)
                    classNameDate.text = "${classInfo.name} - ${formattedTime}H"
                } catch (e: Exception) {
                    classNameDate.text = "${classInfo.name} - ${classInfo.date}"
                }

                // Configurar a cor da linha vertical
                try {
                    verticalLine.setBackgroundColor(Color.parseColor(classInfo.cor))
                } catch (e: IllegalArgumentException) {
                    verticalLine.setBackgroundColor(Color.GRAY)
                }

                // Carregar a imagem
                val imageUrl = "$baseUrl${classInfo.imagem}"
                Glide.with(root.context)
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(R.drawable.circle_image)
                    .into(classCategoryImage)

                // Configurar o clique no item
                root.setOnClickListener { onItemClick(classInfo) }
            }
        }
    }

    inner class EmptyViewHolder(private val binding: ItemClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            with(binding) {
                classNameDate.text = "Não tem aulas para hoje"
                verticalLine.setBackgroundColor(Color.TRANSPARENT)
                classCategoryImage.setImageDrawable(null)
                root.setOnClickListener(null) // Desativar o clique
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (classList.isEmpty()) VIEW_TYPE_EMPTY else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return if (viewType == VIEW_TYPE_ITEM) {
            ClassViewHolder(binding)
        } else {
            EmptyViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ClassViewHolder && classList.isNotEmpty()) {
            holder.bind(classList[position])
        } else if (holder is EmptyViewHolder) {
            holder.bind()
        }
    }

    override fun getItemCount(): Int {
        return if (classList.isEmpty()) 1 else classList.size
    }

    fun updateData(newList: List<NextClassInfo>) {
        classList.clear()
        classList.addAll(newList)
        notifyDataSetChanged()
    }
}
