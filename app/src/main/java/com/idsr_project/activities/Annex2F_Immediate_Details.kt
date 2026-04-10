package com.idsr_project.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.idsr_project.Model.Annex2FData
import com.idsr_project.R
import com.idsr_project.databinding.ActivityAnnex2FimmediateDetailsBinding
import com.idsr_project.utils.DateUtils

class Annex2F_Immediate_Details : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2FimmediateDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2FimmediateDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnBackAnnex2FReport.setOnClickListener { finish() }
        binding.btnBackAnne2FDetail.setOnClickListener { finish() }


        val data = intent.getParcelableExtra<Annex2FData>("data")
        if (data != null) {
            binding.txtPatientName.text = data.patientName
            binding.txtAge.text = data.age
            binding.txtGender.text = data.gender
            binding.txtPhone.text = data.phoneNumber
            binding.txtAddress.text = data.address
            binding.txtOccupation.text = data.occupation
            binding.txtDisease.text = data.disease
            binding.txtInpatientOutpatient.text = data.inpatientOutpatient
            binding.txtDateSeen.text = DateUtils.formatIsoDate(data.dateSeen)
            binding.txtDateOnset.text = DateUtils.formatIsoDate(data.dateOfOnset)
            binding.txtTravelHistory.text = data.travelHistory
            binding.txtDestination.text = data.destination
            binding.txtSite.text = data.site
            binding.txtVaccineDoses.text = data.vaccineDoses
            binding.txtDateLastVaccine.text = DateUtils.formatIsoDate(data.dateLastVaccine)
            binding.txtDateSpecimen.text = DateUtils.formatIsoDate(data.dateSpecimen)
            binding.txtDateLab.text = DateUtils.formatIsoDate(data.dateLab)
            binding.txtLabResults.text = data.labResults
            binding.txtDateFacilityNotified.text = DateUtils.formatIsoDate(data.dateFacilityNotified)
            binding.txtDateSentDistrict.text = DateUtils.formatIsoDate(data.dateSentDistrict)
            binding.txtReporterName.text = data.reporterName
            binding.txtRegion.text = data.region_name
            binding.txtDistrict.text = data.district_name
            binding.txtCreatedAt.text = DateUtils.formatIsoDateTime(data.created_at)
        }
    }
}