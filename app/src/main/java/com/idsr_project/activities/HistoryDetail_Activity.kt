package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.idsr_project.Adapter.DiseaseListAdapter
import com.idsr_project.Model.FormData
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityHistoryDetailBinding
import com.idsr_project.utils.DateUtils
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.idsr_project.Model.ResponseApi
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.ExportManager

class HistoryDetail_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryDetailBinding
    private lateinit var diseaseAdapter: DiseaseListAdapter
    private var form: FormData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackDetail.setOnClickListener { finish() }
        binding.btnBack.setOnClickListener { finish() }

        diseaseAdapter = DiseaseListAdapter()
        binding.rvDiseases.layoutManager = LinearLayoutManager(this)
        binding.rvDiseases.adapter = diseaseAdapter
        binding.rvDiseases.setHasFixedSize(true)

        form = intent.getParcelableExtra<FormData>("form")
        if (form == null) { finish(); return }

        displayData()
        setupEditButton()
        setupExportButton()
    }

    private fun displayData() {
        val f = form!!
        binding.txtFacility.text    = f.facility_name ?: "N/A"
        binding.txtRegion.text      = f.region_name   ?: "N/A"
        binding.txtDistrict.text    = f.district_name ?: "N/A"
        binding.txtEpiweek.text     = f.epiweek       ?: "N/A"
        binding.txtDateFrom.text    = DateUtils.formatIsoDate(f.date_from)
        binding.txtDateTo.text      = DateUtils.formatIsoDate(f.date_to)
        binding.txtTotalU5Male.text   = f.tot_con_u5_male?.toString()   ?: "0"
        binding.txtTotalU5Female.text = f.tot_con_u5_female?.toString() ?: "0"
        binding.txtTotalA5Male.text   = f.tot_con_a5_male?.toString()   ?: "0"
        binding.txtTotalA5Female.text = f.tot_con_a5_female?.toString() ?: "0"
        binding.txtGrandTotal.text    = f.grand_total?.toString()       ?: "0"
        binding.txtComment.text       = f.officer_comment ?: "No comments"
        binding.txtOfficerName.text   = f.officer_name    ?: "N/A"
        binding.txtDesignation.text   = f.designation     ?: "N/A"
        binding.txtCreatedAt.text     = DateUtils.formatIsoDateTime(f.created_at)

        val diseases = f.diseases ?: emptyList()
        if (diseases.isEmpty()) {
            binding.txtDiseasesEmpty.visibility = View.VISIBLE
            binding.rvDiseases.visibility       = View.GONE
        } else {
            binding.txtDiseasesEmpty.visibility = View.GONE
            binding.rvDiseases.visibility       = View.VISIBLE
            diseaseAdapter.setItems(diseases)
        }
    }

    private fun setupEditButton() {
        val f          = form!!
        val currentUserId = SessionManager.getUserId(this)
        val role          = SessionManager.getUserRole(this) ?: ""


        val isOwner = f.user_id == currentUserId
        val isAdmin = role == "Admin"

        if (!isOwner && !isAdmin) {
            binding.btnEditReport.visibility = View.GONE
            return
        }

        val createdAt  = f.created_at
        val withinWindow = isWithin48Hours(createdAt)

        if (!withinWindow && !isAdmin) {
            binding.btnEditReport.visibility = View.GONE
            return
        }

        binding.btnEditReport.visibility = View.VISIBLE
        binding.btnEditReport.setOnClickListener {
            startActivity(
                Intent(this, SurveillanceActivity1::class.java).apply {
                    putExtra(EditModeExtras.EXTRA_EDIT_MODE, true)
                    putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, f.id)
                    putExtra(EditModeExtras.EXTRA_EDIT_DATA, f)
                }
            )
        }
    }

    private fun isWithin48Hours(createdAt: String?): Boolean {
        if (createdAt == null) return false
        return try {
            val sdf      = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val created  = sdf.parse(createdAt) ?: return false
            val diffHours = (System.currentTimeMillis() - created.time) / (1000 * 60 * 60)
            diffHours <= 48
        } catch (e: Exception) {
            false
        }
    }
    private fun setupExportButton() {
        val role = SessionManager.getUserRole(this) ?: ""
        val allowedRoles = listOf("Admin", "Regional Officer", "District Officer")

        if (role !in allowedRoles) {
            binding.btnExport.visibility = View.GONE
            return
        }

        binding.btnExport.visibility = View.VISIBLE
        binding.btnExport.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Export Surveillance Report")
                .setItems(arrayOf("Export as PDF", "Export as CSV")) { _, which ->
                    when (which) {
                        0 -> ExportManager.exportSurveillancePdf(this, form!!)
                        1 -> ExportManager.exportSurveillanceCsv(this, form!!)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}