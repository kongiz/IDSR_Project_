package com.idsr_project.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.idsr_project.Model.Annex2GData
import com.idsr_project.R
import com.idsr_project.databinding.ActivityAnnex2GlabReportDetailBinding
import com.idsr_project.utils.DateUtils

class Annex2G_LabReport_Detail_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2GlabReportDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2GlabReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackAnne2GDetail.setOnClickListener { finish() }
        binding.btnBackAnnex2GReport.setOnClickListener { finish() }

        val data = intent.getParcelableExtra<Annex2GData>("data")
        if (data != null) {
            binding.txtPatientNameLab.text = data.patientNameLab
            binding.txtSex.text = data.sex
            binding.txtAge.text = data.age
            binding.txtDateSpecimenCollect.text = DateUtils.formatIsoDate(data.dateSpecimenCollect)
            binding.txtDateSpecimenSentLab.text = DateUtils.formatIsoDate(data.dateSpecimenSentLab)
            binding.txtClinicianEmail.text = data.emailClinician
            binding.txtPhoneNumber.text = data.phoneNumber
            binding.txtRegion.text = data.region_name
            binding.txtDistrict.text = data.district_name
            binding.txtCreatedAt.text = DateUtils.formatIsoDateTime(data.created_at)
            binding.txtSpecimenID.text = data.specimenUniqueID
            binding.txtSpecimenType.text = data.specimenType
            binding.txtSuspectedDisease.text = data.suspectedDisease
        }
    }
}