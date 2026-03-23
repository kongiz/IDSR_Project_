package com.idsr_project.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.R
import com.idsr_project.data.local.PendingReportEntity
import com.idsr_project.databinding.ItemReportStatusBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportStatusAdapter(
    private var reports: List<PendingReportEntity>,
    private val onDeleteClick: (PendingReportEntity) -> Unit
) : RecyclerView.Adapter<ReportStatusAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReportStatusBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportStatusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report  = reports[position]
        val b       = holder.binding
        val context = holder.itemView.context

        b.tvFormType.text = when (report.formType) {
            "SURVEILLANCE" -> "Weekly Surveillance"
            "ANNEX2F"      -> "Annex 2F — Immediate Case"
            "SPECIMEN"     -> "Case Specimen Report"
            "LAB"          -> "Lab Report"
            else           -> report.formType
        }


        b.tvSubmittedBy.text = if (report.status == "SYNCED") {
            val ageInDays = ((System.currentTimeMillis() - report.createdAt) / (1000 * 60 * 60 * 24)).toInt()
            val daysLeft  = (30 - ageInDays).coerceAtLeast(0)
            val name      = if (report.submittedBy?.isNotEmpty() == true) report.submittedBy else "Unknown"
            "By: $name · deletes in ${daysLeft}d"
        } else {
            val name = if (report.submittedBy?.isNotEmpty() == true) report.submittedBy else "Unknown"
            "By: $name"
        }

        b.tvDateTime.text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(report.createdAt))

        when (report.status) {
            "PENDING" -> {
                val color = ContextCompat.getColor(context, R.color.idsr_secondary)
                b.tvStatusBadge.text = "PENDING"
                b.tvStatusBadge.setBackgroundColor(color)
                b.viewStatusBar.setBackgroundColor(color)
            }
            "SYNCED" -> {
                val color = ContextCompat.getColor(context, R.color.idsr_primary)
                b.tvStatusBadge.text = "SYNCED"
                b.tvStatusBadge.setBackgroundColor(color)
                b.viewStatusBar.setBackgroundColor(color)
            }
            "FAILED" -> {
                val color = ContextCompat.getColor(context, R.color.idsr_error)
                b.tvStatusBadge.text = "FAILED"
                b.tvStatusBadge.setBackgroundColor(color)
                b.viewStatusBar.setBackgroundColor(color)
            }
        }

    }

    override fun getItemCount() = reports.size

    fun getItemAt(position: Int): PendingReportEntity = reports[position]

    fun removeItemAt(position: Int) {
        val mutable = reports.toMutableList()
        mutable.removeAt(position)
        reports = mutable
        notifyItemRemoved(position)
    }

    fun updateReports(newReports: List<PendingReportEntity>) {
        reports = newReports
        notifyDataSetChanged()
    }

}