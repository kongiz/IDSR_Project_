package com.idsr_project.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Model.Disease
import com.idsr_project.R

class DiseaseListAdapter (
    private val items: MutableList<Disease> = mutableListOf()
): RecyclerView.Adapter<DiseaseListAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtDiseaseName)
        val txtStats: TextView = itemView.findViewById(R.id.txtDiseaseStats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diseaselist, parent, false)
        return VH(view)
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        holder.txtName.text = d.name ?: "Unknown"

        val stats = listOf(
            "U5 M: ${d.under5_male ?: 0}",
            "U5 F: ${d.under5_female ?: 0}",
            "A5 M: ${d.above5_male ?: 0}",
            "A5 F: ${d.above5_female ?: 0}",
            "Total: ${d.total ?: 0}"
        ).joinToString(" • ")

        holder.txtStats.text = stats
    }
    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newList: List<Disease>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}