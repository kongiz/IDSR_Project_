package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.Annex2FData
import com.idsr_project.databinding.ActivityAnnex2FimmediateDetailsBinding
import com.idsr_project.utils.DateUtils
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.ExportManager
import com.idsr_project.utils.SessionManager

class Annex2F_Immediate_Details : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2FimmediateDetailsBinding
    private var data: Annex2FData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2FimmediateDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackAnnex2FReport.setOnClickListener { finish() }
        binding.btnBackAnne2FDetail.setOnClickListener { finish() }

        data = intent.getParcelableExtra<Annex2FData>("data")
        if (data == null) { finish(); return }

        displayData()
        setupEditButton()
        setupExportButton()
    }

    private fun displayData() {
        val d = data!!
        binding.txtPatientName.text          = d.patientName
        binding.txtAge.text                  = d.age
        binding.txtGender.text               = d.gender
        binding.txtPhone.text                = d.phoneNumber
        binding.txtAddress.text              = d.address
        binding.txtOccupation.text           = d.occupation
        binding.txtDisease.text              = d.disease
        binding.txtInpatientOutpatient.text  = d.inpatientOutpatient
        binding.txtDateSeen.text             = DateUtils.formatIsoDate(d.dateSeen)
        binding.txtDateOnset.text            = DateUtils.formatIsoDate(d.dateOfOnset)
        binding.txtTravelHistory.text        = d.travelHistory
        binding.txtDestination.text          = d.destination
        binding.txtSite.text                 = d.site
        binding.txtVaccineDoses.text         = d.vaccineDoses
        binding.txtDateLastVaccine.text      = DateUtils.formatIsoDate(d.dateLastVaccine)
        binding.txtDateSpecimen.text         = DateUtils.formatIsoDate(d.dateSpecimen)
        binding.txtDateLab.text              = DateUtils.formatIsoDate(d.dateLab)
        binding.txtLabResults.text           = d.labResults
        binding.txtDateFacilityNotified.text = DateUtils.formatIsoDate(d.dateFacilityNotified)
        binding.txtDateSentDistrict.text     = DateUtils.formatIsoDate(d.dateSentDistrict)
        binding.txtReporterName.text         = d.reporterName
        binding.txtRegion.text               = d.region_name
        binding.txtDistrict.text             = d.district_name
        binding.txtCreatedAt.text            = DateUtils.formatIsoDateTime(d.created_at)
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
                Intent(this, Annex2F_Immediate_1_Activity::class.java).apply {
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
                .setTitle("Export Annex2F Report")
                .setItems(arrayOf("Export as PDF", "Export as CSV")) { _, which ->
                    when (which) {
                        0 -> ExportManager.exportAnnex2FPdf(this, data!!)
                        1 -> ExportManager.exportAnnex2FCsv(this, data!!)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}