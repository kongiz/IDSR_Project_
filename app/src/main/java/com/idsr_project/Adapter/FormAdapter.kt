import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.idsr_project.utils.DateUtils
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Model.FormData
import com.idsr_project.R

class FormAdapter(
    private val forms: MutableList<FormData>,
    private val onItemClick: (FormData) -> Unit,
    private val onLoadMore: (() -> Unit)? = null
) : RecyclerView.Adapter<FormAdapter.FormViewHolder>() {

    inner class FormViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvFormTitle: TextView = view.findViewById(R.id.tvFormTitle)
        private val tvFormDate: TextView = view.findViewById(R.id.tvFormDate)

        fun bind(form: FormData) {

            tvFormTitle.text = form.facility_name ?: "Unknown Facility"

            val fromDate = DateUtils.formatIsoDate(form.date_from)
            val toDate = DateUtils.formatIsoDate(form.date_to)

            tvFormDate.text = "$fromDate – $toDate"

            itemView.setOnClickListener {
                onItemClick(form)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_form_card, parent, false)
        return FormViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
        holder.bind(forms[position])

        // Pagination trigger
        if (position == forms.lastIndex) {
            onLoadMore?.invoke()
        }
    }

    override fun getItemCount(): Int = forms.size

    fun updateForms(newForms: List<FormData>) {
        forms.clear()
        forms.addAll(newForms)
        notifyDataSetChanged()
    }

    fun addMoreForms(newForms: List<FormData>) {
        val start = forms.size
        forms.addAll(newForms)
        notifyItemRangeInserted(start, newForms.size)
    }
}
