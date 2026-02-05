package com.idsr_project.Adapter

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Model.Diseases
import com.idsr_project.R

class DiseaseAdapter(private val diseaseList: MutableList<Diseases>) :
    RecyclerView.Adapter<DiseaseAdapter.DiseaseViewHolder>() {

    companion object {
        var diseaseDetails = mutableListOf<Diseases>()
    }

    inner class DiseaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val diseaseName: TextView = itemView.findViewById(R.id.diseaseName)
        val dropdownIcon: ImageView = itemView.findViewById(R.id.dropdownIcon)
        val detailLayout: LinearLayout = itemView.findViewById(R.id.detailsLayout)

        val u5MaleAlive: EditText = itemView.findViewById(R.id.u5MaleAlive)
        val u5FemaleAlive: EditText = itemView.findViewById(R.id.u5FemaleAlive)
        val a5MaleAlive: EditText = itemView.findViewById(R.id.a5MaleAlive)
        val a5FemaleAlive: EditText = itemView.findViewById(R.id.a5FemaleAlive)

        val u5MaleDeath: EditText = itemView.findViewById(R.id.u5MaleDeath)
        val u5FemaleDeath: EditText = itemView.findViewById(R.id.u5FemaleDeath)
        val a5MaleDeath: EditText = itemView.findViewById(R.id.a5MaleDeath)
        val a5FemaleDeath: EditText = itemView.findViewById(R.id.a5FemaleDeath)

        val totalSamples: EditText = itemView.findViewById(R.id.totalSamples)
        val btnSaveInExpand: Button = itemView.findViewById(R.id.btnSaveInExpand)

        val textWatchers = mutableMapOf<EditText, TextWatcher>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.disease_list_item, parent, false)
        return DiseaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiseaseViewHolder, position: Int) {
        val disease = diseaseList[position]

        holder.diseaseName.text = disease.name
        holder.detailLayout.visibility = if (disease.isExpanded) View.VISIBLE else View.GONE
        holder.dropdownIcon.rotation = if (disease.isExpanded) 180f else 0f

        holder.itemView.findViewById<LinearLayout>(R.id.headerLayout).setOnClickListener {
            disease.isExpanded = !disease.isExpanded
            notifyItemChanged(position)
        }

        fun bindEditText(editText: EditText, value: Int, onChanged: (Int) -> Unit) {
            holder.textWatchers[editText]?.let { editText.removeTextChangedListener(it) }
            if (editText.text.toString() != value.toString()) {
                editText.setText(value.toString())
            }

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val number = s?.toString()?.toIntOrNull() ?: 0
                    onChanged(number)
                    editText.error = if (s.isNullOrEmpty()) "Required" else null
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            editText.addTextChangedListener(watcher)
            holder.textWatchers[editText] = watcher
        }

        bindEditText(holder.u5MaleAlive, disease.u5MaleAlive) { disease.u5MaleAlive = it }
        bindEditText(holder.u5FemaleAlive, disease.u5FemaleAlive) { disease.u5FemaleAlive = it }
        bindEditText(holder.a5MaleAlive, disease.a5MaleAlive) { disease.a5MaleAlive = it }
        bindEditText(holder.a5FemaleAlive, disease.a5FemaleAlive) { disease.a5FemaleAlive = it }

        bindEditText(holder.u5MaleDeath, disease.u5MaleDeath) { disease.u5MaleDeath = it }
        bindEditText(holder.u5FemaleDeath, disease.u5FemaleDeath) { disease.u5FemaleDeath = it }
        bindEditText(holder.a5MaleDeath, disease.a5MaleDeath) { disease.a5MaleDeath = it }
        bindEditText(holder.a5FemaleDeath, disease.a5FemaleDeath) { disease.a5FemaleDeath = it }
        bindEditText(holder.totalSamples, disease.totalSamples) { disease.totalSamples = it }


        holder.btnSaveInExpand.setOnClickListener {
            if (validateDisease(disease)) {
                if (diseaseDetails.none { it.name == disease.name }) {
                    diseaseDetails.add(disease.copy())
                } else {
                    val index = diseaseDetails.indexOfFirst { it.name == disease.name }
                    diseaseDetails[index] = disease.copy()
                }
                Log.i("DiseaseAdapter", "Saved: ${disease.name}")
            }
        }
    }

    private fun validateDisease(disease: Diseases): Boolean {
        val fields = listOf(
            disease.u5MaleAlive,
            disease.u5FemaleAlive,
            disease.a5MaleAlive,
            disease.a5FemaleAlive,
            disease.u5MaleDeath,
            disease.u5FemaleDeath,
            disease.a5MaleDeath,
            disease.a5FemaleDeath,
            disease.totalSamples
        )
        return fields.all { it >= 0 }
    }

    fun getDiseases(): List<Diseases> = diseaseList

    override fun getItemCount(): Int = diseaseList.size
}
