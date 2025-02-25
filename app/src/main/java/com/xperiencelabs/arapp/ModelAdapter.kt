import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.xperiencelabs.arapp.ModelItem
import com.xperiencelabs.arapp.R

class ModelAdapter(
    private val models: List<ModelItem>,
    private val onItemClick: (ModelItem) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val modelThumbnail: ImageView = view.findViewById(R.id.model_thumbnail)
        val modelName: TextView = view.findViewById(R.id.model_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]
        holder.modelName.text = model.name

        // Load dynamic image into modelThumbnail using Glide.
        Glide.with(holder.itemView.context)
            .load(model.thumbnailRes) // Assuming your ModelItem property for the 2D URL is named "dynamicImageUrl"
//            .placeholder(R.drawable.placeholder) // optional placeholder
            .error(R.drawable.error) // optional error drawable
            .into(holder.modelThumbnail)

        holder.itemView.setOnClickListener {
            onItemClick(model)
        }
    }

    override fun getItemCount() = models.size
}
