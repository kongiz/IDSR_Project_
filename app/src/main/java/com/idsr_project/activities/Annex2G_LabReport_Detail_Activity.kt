package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.Annex2GData
import com.idsr_project.databinding.ActivityAnnex2GlabReportDetailBinding
import com.idsr_project.utils.DateUtils
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.ExportManager
import com.idsr_project.utils.SessionManager

class Annex2G_LabReport_Detail_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2GlabReportDetailBinding
    private var data: Annex2GData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2GlabReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackAnne2GDetail.setOnClickListener { finish() }
        binding.btnBackAnnex2GReport.setOnClickListener { finish() }

        data = intent.getParcelableExtra<Annex2GData>("data")
        if (data == null) { finish(); return }

        displayData()
        setupEditButton()
        setupExportButton()
    }

    private fun displayData() {
        val d = data!!
        binding.txtPatientNameLab.text       = d.patientNameLab
        binding.txtSex.text                  = d.sex
        binding.txtAge.text                  = d.age
        binding.txtDateSpecimenCollect.text  = DateUtils.formatIsoDate(d.dateSpecimenCollect)
        binding.txtDateSpecimenSentLab.text  = DateUtils.formatIsoDate(d.dateSpecimenSentLab)
        binding.txtClinicianEmail.text       = d.emailClinician
        binding.txtPhoneNumber.text          = d.phoneNumber
        binding.txtRegion.text               = d.region_name
        binding.txtDistrict.text             = d.district_name
        binding.txtCreatedAt.text            = DateUtils.formatIsoDateTime(d.created_at)
        binding.txtSpecimenID.text           = d.specimenUniqueID
        binding.txtSpecimenType.text         = d.specimenType
        binding.txtSuspectedDisease.text     = d.suspectedDisease
    }

    private fun setupEditButton() {
        val d             = data!!
        val currentUserId = SessionManager.getUserId(this)
        val role          = SessionManager.getUserRole(this) ?: ""
        val isOwner       = d.user_id == currentUserId
        val isAdmin       = role == "Admin"

        if (!isOwner && !isAdmin) { binding.btnEditReport.visibility = View.GONE; return }
        if (!isWithin48Hours(d.created_at) && !isAdmin) { binding.btnEditReport.visibility = View.GONE; return }

        binding.btnEditReport.visibility = View.VISIBLE
        binding.btnEditReport.setOnClickListener {
            startActivity(
                Intent(this, Laboratory_Form_1_Annex2G_Activity::class.java).apply {
                    putExtra(EditModeExtras.EXTRA_EDIT_MODE, true)
                    putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, d.id)
                    putExtra(EditModeExtras.EXTRA_EDIT_DATA, d)
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
                .setTitle("Export Specimen Report")
                .setItems(arrayOf("Export as PDF", "Export as CSV")) { _, which ->
                    when (which) {
                        0 -> ExportManager.exportAnnex2GPdf(this, data!!)
                        1 -> ExportManager.exportAnnex2GCsv(this, data!!)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}