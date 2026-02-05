package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.idsr_project.Model.LabReportData
import com.idsr_project.R
import com.idsr_project.databinding.ActivityLabReportsDetailBinding

class Lab_Reports_Detail_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityLabReportsDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLabReportsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackLabReportDetail.setOnClickListener { finish() }
        binding.btnBackLabReportReport.setOnClickListener { finish() }

        val labReport = intent.getParcelableExtra<LabReportData>("data")
        if (labReport != null) {
            binding.txtLabName.text = labReport.lab_name
            binding.txtDateLabReceived.text = labReport.date_lab_received
            binding.txtSpecimenCondition.text = labReport.specimen_condition
            binding.txtTestTypes.text = labReport.test_types_performed

            binding.txtFinalResult.text = labReport.final_lab_result

            binding.txtDateSentDistrict.text = labReport.date_lab_sent_district
            binding.txtDateDistrictReceived.text = labReport.date_district_received_lab_result
            binding.txtRegion.text = labReport.region_name
            binding.txtDistrict.text = labReport.district_name
            binding.txtCreatedAt.text = labReport.created_at

            Glide.with(this)
                .load(labReport.lab_result_image)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(binding.imgLabResult)
            Log.e("LAB_IMG_URL", labReport.lab_result_image ?: "NULL URL")

        }
        binding.imgLabResult.setOnClickListener {
            val imageUri = labReport?.lab_result_image
            if (imageUri != null) {
                val intent = Intent(this, Img_Preview_Activity::class.java)
                intent.putExtra("imageUri", imageUri)
                startActivity(intent)
            }

        }

    }
}