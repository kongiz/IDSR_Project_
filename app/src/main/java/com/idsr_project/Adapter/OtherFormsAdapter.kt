package com.idsr_project.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.idsr_project.utils.DateUtils
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Model.OtherFormsData
import com.idsr_project.R

class OtherFormsAdapter (
    private val forms: MutableList<OtherFormsData>,
    private val onItemClick: (OtherFormsData) -> Unit
) : RecyclerView.Adapter<OtherFormsAdapter.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<OtherFormsData>) {
//        forms.clear()
        forms.addAll(newData)
        notifyDataSetChanged()
    }
    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.txtTitle)
        val subtitle = view.findViewById<TextView>(R.id.txtSubtitle)
        val date = view.findViewById<TextView>(R.id.txtDate)
        val type = view.findViewById<TextView>(R.id.txtType)

        init {
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(forms[pos])
                }
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_other_forms, parent, false)
        return ViewHolder(v)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val form = forms[position]
        holder.title.text = form.title
        holder.subtitle.text = form.subTitle
        holder.date.text = DateUtils.formatIsoDate(form.date)
        holder.type.text = form.type
    }

    override fun getItemCount(): Int {
        return forms.size
    }
}