import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ua.goworkout.databinding.ItemClassBinding
import java.text.SimpleDateFormat
import java.util.*

data class ClassInfo(val name: String, val date: String, val cor: String, val imagem: String)

class HistoryAdapter(
    private val classList: List<ClassInfo>,
    private val onItemClick: (ClassInfo) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ClassViewHolder>() {

    private val baseUrl = "https://esan-tesp-ds-paw.web.ua.pt/tesp-ds-g37/uploads/imagem_categoria/"

    inner class ClassViewHolder(private val binding: ItemClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(classInfo: ClassInfo) {
            // Converter a data para o formato desejado
            val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val desiredFormat = SimpleDateFormat("dd/MM/yyyy | HH:mm'H'", Locale.getDefault())

            val date = originalFormat.parse(classInfo.date)
            val formattedDate = date?.let { desiredFormat.format(it) }

            // Exibir a data formatada
            binding.classNameDate.text = "${classInfo.name} - $formattedDate"

            // A linha vertical, onde a cor será alterada
            binding.verticalLine.setBackgroundColor(Color.parseColor(classInfo.cor))

            // Concatenar a URL base com o nome da imagem
            val imageUrl = "$baseUrl${classInfo.imagem}"

            // Carregar a imagem da categoria usando Glide
            Glide.with(binding.root.context)
                .load(imageUrl) // URL completo da imagem
                .apply(RequestOptions.circleCropTransform())
                .into(binding.classCategoryImage) // A ImageView no layout

            // Definir o click listener
            binding.root.setOnClickListener { onItemClick(classInfo) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val binding = ItemClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(classList[position])
    }

    override fun getItemCount() = classList.size
}
