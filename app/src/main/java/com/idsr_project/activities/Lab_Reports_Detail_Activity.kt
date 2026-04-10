package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.idsr_project.Model.LabReportData
import com.idsr_project.R
import com.idsr_project.databinding.ActivityLabReportsDetailBinding
import com.idsr_project.utils.DateUtils
import com.idsr_project.utils.SessionManager

class Lab_Reports_Detail_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLabReportsDetailBinding
    private var labReport: LabReportData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLabReportsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadLabReportData()
    }

    private fun setupClickListeners() {
        binding.btnBackLabReportDetail.setOnClickListener { finish() }
        binding.btnBackLabReportReport.setOnClickListener { finish() }

        binding.imgLabResult.setOnClickListener {
            openImagePreview()
        }
    }

    private fun loadLabReportData() {
        labReport = intent.getParcelableExtra<LabReportData>("data")

        if (labReport == null) {
            Toast.makeText(this, "Error loading lab report data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayLabInformation()
        displayFinalResult()
        displayAdministrativeInfo()
        loadLabResultImages()
    }

    private fun displayLabInformation() {
        labReport?.let { report ->
            binding.txtLabName.text = report.lab_name.orEmpty().ifEmpty { "N/A" }
            binding.txtDateLabReceived.text = DateUtils.formatIsoDate(report.date_lab_received)
            binding.txtSpecimenCondition.text = report.specimen_condition.orEmpty().ifEmpty { "N/A" }
            binding.txtTestTypes.text = report.test_types_performed.orEmpty().ifEmpty { "N/A" }
        }
    }

    private fun displayFinalResult() {
        labReport?.let { report ->
            val finalResult = report.final_lab_result.orEmpty().ifEmpty { "Pending" }
            binding.txtFinalResult.text = finalResult

            when (finalResult.lowercase()) {
                "positive" -> binding.txtFinalResult.setTextColor(getColor(R.color.idsr_error))
                "negative" -> binding.txtFinalResult.setTextColor(getColor(android.R.color.holo_green_dark))
                else -> binding.txtFinalResult.setTextColor(getColor(R.color.idsr_gray))
            }
        }
    }

    private fun displayAdministrativeInfo() {
        labReport?.let { report ->
            binding.txtDateSentDistrict.text = DateUtils.formatIsoDate(report.date_lab_sent_district)
            binding.txtDateDistrictReceived.text = DateUtils.formatIsoDate(report.date_district_received_lab_result)
            binding.txtRegion.text = report.region_name.orEmpty().ifEmpty { "N/A" }
            binding.txtDistrict.text = report.district_name.orEmpty().ifEmpty { "N/A" }
            binding.txtCreatedAt.text = DateUtils.formatIsoDateTime(report.created_at)
        }
    }

    private fun loadLabResultImages() {
        val images = labReport?.lab_result_images

        if (images.isNullOrEmpty()) {
            binding.imgLabResult.setImageResource(R.drawable.placeholder_image)
            binding.tvImageCount.text = "No images available"
            binding.imgLabResult.isClickable = false
            return
        }


        binding.tvImageCount.text = "1 of ${images.size} images (Tap to preview all)"

        val token = SessionManager.getAccessToken(this)
        val firstImageUrl = images[0]


        val glideUrl = GlideUrl(
            firstImageUrl,
            LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        )

        Glide.with(this)
            .load(glideUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.imgLabResult)

        binding.imgLabResult.isClickable = true
    }

    private fun openImagePreview() {
        val images = labReport?.lab_result_images

        if (images.isNullOrEmpty()) {
            Toast.makeText(this, "No images available to preview", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(this, Img_Preview_Activity::class.java).apply {
                // Pass the list as an ArrayList of strings
                putStringArrayListExtra("imageList", ArrayList(images))
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("LAB_REPORT_DETAIL", "Error opening image preview", e)
            Toast.makeText(this, "Unable to open image preview", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "Lab_Reports_Detail"
    }
}