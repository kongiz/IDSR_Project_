package com.idsr_project.Adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Model.Diseases
import com.idsr_project.R
import com.idsr_project.databinding.DiseaseListItemBinding // Using ViewBinding for cleaner code

class DiseaseAdapter(private val diseaseList: List<Diseases>) :
    RecyclerView.Adapter<DiseaseAdapter.DiseaseViewHolder>() {

    inner class DiseaseViewHolder(val binding: DiseaseListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseViewHolder {
        val binding = DiseaseListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DiseaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiseaseViewHolder, position: Int) {
        val disease = diseaseList[position]
        val b = holder.binding


        b.diseaseName.text = disease.name


        b.detailsLayout.visibility = if (disease.isExpanded) View.VISIBLE else View.GONE
        b.dropdownIcon.rotation = if (disease.isExpanded) 180f else 0f

        b.headerLayout.setOnClickListener {
            disease.isExpanded = !disease.isExpanded
            notifyItemChanged(position)
        }


        setupField(b.u5MaleAlive, disease.u5MaleAlive) { disease.u5MaleAlive = it }
        setupField(b.u5FemaleAlive, disease.u5FemaleAlive) { disease.u5FemaleAlive = it }
        setupField(b.a5MaleAlive, disease.a5MaleAlive) { disease.a5MaleAlive = it }
        setupField(b.a5FemaleAlive, disease.a5FemaleAlive) { disease.a5FemaleAlive = it }

        setupField(b.u5MaleDeath, disease.u5MaleDeath) { disease.u5MaleDeath = it }
        setupField(b.u5FemaleDeath, disease.u5FemaleDeath) { disease.u5FemaleDeath = it }
        setupField(b.a5MaleDeath, disease.a5MaleDeath) { disease.a5MaleDeath = it }
        setupField(b.a5FemaleDeath, disease.a5FemaleDeath) { disease.a5FemaleDeath = it }

        setupField(b.totalSamples, disease.totalSamples) { disease.totalSamples = it }


        b.btnSaveInExpand.setOnClickListener {
            disease.isExpanded = false
            notifyItemChanged(position)
        }
    }

    private fun setupField(editText: EditText, value: Int, onUpdate: (Int) -> Unit) {
        editText.tag = null
        editText.setText(if (value == 0 && editText.tag == null) "" else value.toString())

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().toIntOrNull() ?: 0
                onUpdate(input)
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        editText.addTextChangedListener(watcher)
        editText.tag = watcher
    }

    override fun getItemCount() = diseaseList.size

    fun getDiseases(): List<Diseases> = diseaseList
}