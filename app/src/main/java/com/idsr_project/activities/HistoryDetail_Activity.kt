package com.idsr_project.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.idsr_project.Adapter.DiseaseListAdapter
import com.idsr_project.Model.FormData
import com.idsr_project.databinding.ActivityHistoryDetailBinding
import com.idsr_project.utils.DateUtils

class HistoryDetail_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryDetailBinding
    private lateinit var diseaseAdapter: DiseaseListAdapter

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

        val form = intent.getParcelableExtra<FormData>("form")
        if (form == null) {
            finish()
            return
        }

        binding.txtFacility.text = form.facility_name ?: "N/A"
        binding.txtRegion.text = form.region_name ?: "N/A"
        binding.txtDistrict.text = form.district_name ?: "N/A"
        binding.txtEpiweek.text = form.epiweek ?: "N/A"
        binding.txtDateFrom.text = DateUtils.formatIsoDate(form.date_from)
        binding.txtDateTo.text = DateUtils.formatIsoDate(form.date_to)
        binding.txtTotalU5Male.text = form.tot_con_u5_male?.toString() ?: "0"
        binding.txtTotalU5Female.text = form.tot_con_u5_female?.toString() ?: "0"
        binding.txtTotalA5Male.text = form.tot_con_a5_male?.toString() ?: "0"
        binding.txtTotalA5Female.text = form.tot_con_a5_female?.toString() ?: "0"
        binding.txtGrandTotal.text = form.grand_total?.toString() ?: "0"
        binding.txtComment.text = form.officer_comment ?: "No comments"
        binding.txtOfficerName.text = form.officer_name ?: "N/A"
        binding.txtDesignation.text = form.designation ?: "N/A"
        binding.txtCreatedAt.text = DateUtils.formatIsoDateTime(form.created_at)

        val diseaseNames = form.diseases?.joinToString(", ") { it.name ?: "Unknown" } ?: "No diseases listed"

        val diseases = form.diseases ?: emptyList()
        if (diseases.isEmpty()) {
            binding.txtDiseasesEmpty.visibility = View.VISIBLE
            binding.rvDiseases.visibility = View.GONE
        } else {
            binding.txtDiseasesEmpty.visibility = View.GONE
            binding.rvDiseases.visibility = View.VISIBLE
            diseaseAdapter.setItems(diseases)
        }
    }
}
