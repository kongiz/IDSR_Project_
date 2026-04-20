package com.idsr_project.Adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.idsr_project.Model.Diseases
import com.idsr_project.databinding.DiseaseListItemBinding

class DiseaseAdapter(private val diseaseList: MutableList<Diseases>) :
    RecyclerView.Adapter<DiseaseAdapter.DiseaseViewHolder>() {

    inner class DiseaseViewHolder(val binding: DiseaseListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    init { setHasStableIds(true) }

    override fun getItemId(position: Int) =
        diseaseList[position].name.hashCode().toLong()

    override fun getItemCount() = diseaseList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DiseaseViewHolder(
            DiseaseListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: DiseaseViewHolder, position: Int) {
        val disease = diseaseList[position]
        val b = holder.binding

        b.diseaseName.text = disease.name
        updateExpandState(b, disease.isExpanded)

        b.headerLayout.setOnClickListener {
            disease.isExpanded = !disease.isExpanded
            updateExpandState(b, disease.isExpanded)
        }


        bindField(b.u5MaleAliveLayout,   b.u5MaleAlive,   disease.u5MaleAlive)   { disease.u5MaleAlive   = it }
        bindField(b.u5FemaleAliveLayout, b.u5FemaleAlive, disease.u5FemaleAlive) { disease.u5FemaleAlive = it }
        bindField(b.a5MaleAliveLayout,   b.a5MaleAlive,   disease.a5MaleAlive)   { disease.a5MaleAlive   = it }
        bindField(b.a5FemaleAliveLayout, b.a5FemaleAlive, disease.a5FemaleAlive) { disease.a5FemaleAlive = it }

        bindField(b.u5MaleDeathLayout,   b.u5MaleDeath,   disease.u5MaleDeath)   { disease.u5MaleDeath   = it }
        bindField(b.u5FemaleDeathLayout, b.u5FemaleDeath, disease.u5FemaleDeath) { disease.u5FemaleDeath = it }
        bindField(b.a5MaleDeathLayout,   b.a5MaleDeath,   disease.a5MaleDeath)   { disease.a5MaleDeath   = it }
        bindField(b.a5FemaleDeathLayout, b.a5FemaleDeath, disease.a5FemaleDeath) { disease.a5FemaleDeath = it }

        bindField(b.totalSamplesLayout, b.totalSamples, disease.totalSamples) { disease.totalSamples = it }

        b.btnSaveInExpand.setOnClickListener {
            if (validateCard(disease, b)) {
                disease.isExpanded = false
                updateExpandState(b, false)
            }
        }
    }

    private fun updateExpandState(b: DiseaseListItemBinding, expanded: Boolean) {
        b.detailsLayout.visibility = if (expanded) View.VISIBLE else View.GONE
        b.dropdownIcon.animate()
            .rotation(if (expanded) 180f else 0f)
            .setDuration(200)
            .start()
    }

    private fun bindField(
        layout: TextInputLayout,
        editText: TextInputEditText,
        currentValue: Int,
        onValueChanged: (Int) -> Unit
    ) {

        (editText.tag as? TextWatcher)?.let { editText.removeTextChangedListener(it) }


        val display = currentValue.toString() // "0" instead of ""
        if (editText.text?.toString() != display) editText.setText(display)
        layout.error = null

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val raw = s?.toString().orEmpty()
                when {
                    raw.isEmpty() -> {
                        layout.error = "Required, Please enter 0 if none"
                    }
                    raw.toIntOrNull() == null -> {
                        layout.error = "Enter a valid number"
                    }
                    raw.toInt() < 0 -> {
                        layout.error = "Cannot be negative"
                    }
                    raw.toInt() > 9999 -> {
                        layout.error = "Value seems too large"
                        onValueChanged(raw.toInt())
                    }
                    else -> {
                        layout.error = null
                        onValueChanged(raw.toInt())
                    }
                }
            }
        }

        editText.addTextChangedListener(watcher)
        editText.tag = watcher
    }

    private fun validateCard(disease: Diseases, b: DiseaseListItemBinding): Boolean {
        var isValid = true

        val allFields = listOf(
            b.u5MaleAliveLayout   to b.u5MaleAlive,
            b.u5FemaleAliveLayout to b.u5FemaleAlive,
            b.a5MaleAliveLayout   to b.a5MaleAlive,
            b.a5FemaleAliveLayout to b.a5FemaleAlive,
            b.u5MaleDeathLayout   to b.u5MaleDeath,
            b.u5FemaleDeathLayout to b.u5FemaleDeath,
            b.a5MaleDeathLayout   to b.a5MaleDeath,
            b.a5FemaleDeathLayout to b.a5FemaleDeath,
            b.totalSamplesLayout  to b.totalSamples
        )

        for ((layout, editText) in allFields) {
            val text = editText.text?.toString().orEmpty()
            when {
                text.isEmpty() -> {
                    layout.error = "Required — enter 0 if none"
                    isValid = false
                }
                text.toIntOrNull() == null -> {
                    layout.error = "Enter a valid number"
                    isValid = false
                }
                text.toInt() < 0 -> {
                    layout.error = "Cannot be negative"
                    isValid = false
                }
                else -> layout.error = null
            }
        }

        return isValid
    }

    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        for (disease in diseaseList) {
            val n = disease.name

            val fields = mapOf(
                "U5 Male Alive"    to disease.u5MaleAlive,
                "U5 Female Alive"  to disease.u5FemaleAlive,
                "A5 Male Alive"    to disease.a5MaleAlive,
                "A5 Female Alive"  to disease.a5FemaleAlive,
                "U5 Male Death"    to disease.u5MaleDeath,
                "U5 Female Death"  to disease.u5FemaleDeath,
                "A5 Male Death"    to disease.a5MaleDeath,
                "A5 Female Death"  to disease.a5FemaleDeath,
                "Total Samples"    to disease.totalSamples,
            )

            for ((label, value) in fields) {
                if (value < 0) errors += "[$n] $label cannot be negative"
            }
        }
        return errors.distinct()
    }

    fun getDiseases(): List<Diseases> = diseaseList
}