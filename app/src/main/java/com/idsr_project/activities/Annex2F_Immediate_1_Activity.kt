package com.idsr_project.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityAnnex2Fimmediate1Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Annex2F_Immediate_1_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2Fimmediate1Binding

    private var regions = listOf<HealthRegions>()
    private var districts = listOf<HealthDistricts>()
    private var selectedRegionId: Int? = null
    private var selectedDistrictId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackAnnex1.setOnClickListener {
            finish()
        }

        setupSpinners()
        loadRegions()
        setUpFieldListener()

        binding.btnNextAnnex1.setOnClickListener {
            if (validateImmediate1Form()) {
                passDataToNextScreen()
            }
        }
    }

    private fun setupSpinners() {
        // Country is already set to "Gambia" and disabled in XML


        val inpatientOutpatient = arrayOf("Inpatient", "Outpatient")
        val inpatientAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            inpatientOutpatient
        )
        binding.spinnerInpatientOutpatient.setAdapter(inpatientAdapter)

        binding.spinnerInpatientOutpatient.setOnItemClickListener { _, _, _, _ ->
            binding.titleInpatientOutpatient.error = null
        }
    }

    private fun loadRegions() {
        ApiClient.getClient(this).getRegions()
            .enqueue(object : Callback<ApiResponse<List<HealthRegions>>> {

                override fun onResponse(
                    call: Call<ApiResponse<List<HealthRegions>>>,
                    response: Response<ApiResponse<List<HealthRegions>>>
                ) {
                    if (!response.isSuccessful || response.body()?.data == null) {
                        binding.titleRegion.error = "Failed to load regions"
                        return
                    }

                    regions = response.body()!!.data!!
                    val regionNames = regions.map { it.region_name ?: "Region" }

                    val adapter = ArrayAdapter(
                        this@Annex2F_Immediate_1_Activity,
                        android.R.layout.simple_list_item_1,
                        regionNames
                    )

                    binding.spinnerRegion.setAdapter(adapter)

                    binding.spinnerRegion.setOnItemClickListener { _, _, position, _ ->
                        selectedRegionId = regions[position].regions_id
                        binding.titleRegion.error = null

                        // Reset district
                        selectedDistrictId = null
                        binding.spinnerDistrict.setText("", false)

                        // Load districts for selected region
                        loadDistricts(selectedRegionId!!)
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<List<HealthRegions>>>,
                    t: Throwable
                ) {
                    Log.e("Annex2F_Immediate_1", "Error loading regions", t)
                    binding.titleRegion.error = "Error loading regions"
                }
            })
    }

    private fun loadDistricts(regionId: Int) {
        ApiClient.getClient(this).getDistrictsByRegion(regionId)
            .enqueue(object : Callback<ApiResponse<List<HealthDistricts>>> {

                override fun onResponse(
                    call: Call<ApiResponse<List<HealthDistricts>>>,
                    response: Response<ApiResponse<List<HealthDistricts>>>
                ) {
                    if (!response.isSuccessful || response.body()?.data == null) {
                        binding.titleDistrict.error = "Failed to load districts"
                        return
                    }

                    districts = response.body()!!.data!!
                    val districtNames = districts.map { it.district_name ?: "District" }

                    val adapter = ArrayAdapter(
                        this@Annex2F_Immediate_1_Activity,
                        android.R.layout.simple_list_item_1,
                        districtNames
                    )

                    binding.spinnerDistrict.setAdapter(adapter)

                    binding.spinnerDistrict.setOnItemClickListener { _, _, position, _ ->
                        selectedDistrictId = districts[position].district_id
                        binding.titleDistrict.error = null
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<List<HealthDistricts>>>,
                    t: Throwable
                ) {
                    Log.e("Annex2F_Immediate_1", "Error loading districts", t)
                    binding.titleDistrict.error = "Error loading districts"
                }
            })
    }

    @SuppressLint("SuspiciousIndentation")
    private fun validateImmediate1Form(): Boolean {
        var isValid = true

        if (binding.etRecordId.text.isNullOrEmpty()) {
            binding.titleRecordId.error = "Record's Unique Identifier is required"
            isValid = false
        } else {
            binding.titleRecordId.error = null
        }

        // Country is always "Gambia" (no validation needed)

        if (binding.spinnerRegion.text.isNullOrEmpty()) {
            binding.titleRegion.error = "Reporting Region is required"
            isValid = false
        } else {
            binding.titleRegion.error = null
        }

        if (binding.spinnerDistrict.text.isNullOrEmpty()) {
            binding.titleDistrict.error = "Reporting District is required"
            isValid = false
        } else {
            binding.titleDistrict.error = null
        }

        if (binding.etSite.text.isNullOrEmpty()) {
            binding.titleSite.error = "Reporting Site (e.g. Health Facility, Village...) is required"
            isValid = false
        } else {
            binding.titleSite.error = null
        }

        if (binding.etDisease.text.isNullOrEmpty()) {
            binding.titleDisease.error = "Disease/Event (diagnosis) is required"
            isValid = false
        } else {
            binding.titleDisease.error = null
        }

        if (binding.spinnerInpatientOutpatient.text.isNullOrEmpty()) {
            binding.titleInpatientOutpatient.error = "Inpatient or Outpatient? is required"
            isValid = false
        } else {
            binding.titleInpatientOutpatient.error = null
        }

        return isValid
    }

    private fun setUpFieldListener() {
        // Text fields
        binding.etRecordId.addTextChangedListener {
            if (!it.isNullOrEmpty()) {
                binding.titleRecordId.error = null
            }
        }

        binding.etSite.addTextChangedListener {
            if (!it.isNullOrEmpty()) {
                binding.titleSite.error = null
            }
        }

        binding.etDisease.addTextChangedListener {
            if (!it.isNullOrEmpty()) {
                binding.titleDisease.error = null
            }
        }
    }

    private fun passDataToNextScreen() {
        val recordId = binding.etRecordId.text.toString().trim()
        val country = "Gambia" // Fixed to Gambia
        val province = binding.spinnerRegion.text.toString().trim()
        val district = binding.spinnerDistrict.text.toString().trim()
        val site = binding.etSite.text.toString().trim()
        val disease = binding.etDisease.text.toString().trim()
        val inpatientOutpatient = binding.spinnerInpatientOutpatient.text.toString().trim()

        val annex2FReports = immediateReportForm(
            recordId = recordId,
            country = country,
            province = province,
            district = district,
            site = site,
            disease = disease,
            inpatientOutpatient = inpatientOutpatient,
            dateSeen = "",
            patientName = "",
            dateOfBirth = "",
            age = "",
            gender = "",
            address = "",
            districtAnnex2 = "",
            urbanRural = "",
            phoneNumber = "",
            occupation = "",
            dateOfOnset = "",
            travelHistory = "No",
            destination = "",
            vaccineDoses = "0",
            dateLastVaccine = "",
            dateSpecimen = "",
            dateLab = "",
            labResults = "",
            outcome = "",
            classification = "",
            dateFacilityNotified = "",
            dateSentDistrict = "",
            reporterName = ""
        )

        val intent = Intent(this, Annex2F_Immediate_2_Activity::class.java).apply {
            putExtra("Annex2FReport", annex2FReports)
        }
        startActivity(intent)
        finish()
    }
}