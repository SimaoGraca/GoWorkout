import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ua.goworkout.databinding.ItemClassBinding

data class ClassInfo(val name: String, val date: String)

class HistoryAdapter(
    private val classList: List<ClassInfo>,
    private val onItemClick: (ClassInfo) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(private val binding: ItemClassBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(classInfo: ClassInfo) {
            binding.classNameDate.text = "${classInfo.name} - ${classInfo.date}"
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
