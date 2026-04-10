package com.idsr_project.activities


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityAnnex2Fimmediate1Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class Annex2F_Immediate_1_Activity : BaseActivity() {
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

        setupClickListeners()
        setupSpinners()
        loadRegions()
        setUpFieldListener()

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Annex2F_Immediate_1_Activity")
    }

    private fun setupClickListeners() {
        binding.btnBackAnnex1.setOnClickListener { finish() }

        binding.btnNextAnnex1.setOnClickListener {
            if (validateImmediate1Form()) {
                passDataToNextScreen()
            }
        }
    }

    private fun setupSpinners() {
        val inpatientOutpatient = arrayOf("Inpatient", "Outpatient")
        val inpatientAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, inpatientOutpatient)
        binding.spinnerInpatientOutpatient.setAdapter(inpatientAdapter)


        binding.spinnerInpatientOutpatient.setOnItemClickListener { _, _, _, _ ->
            binding.titleInpatientOutpatient.error = null
        }
    }

    private fun loadRegions() {
        ApiClient.getClient(this).getRegions()
            .enqueue(object : Callback<ApiResponse<List<HealthRegions>>> {
                override fun onResponse(call: Call<ApiResponse<List<HealthRegions>>>, response: Response<ApiResponse<List<HealthRegions>>>) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        regions = response.body()!!.data!!
                        val regionNames = regions.map { it.region_name ?: "Unknown Region" }
                        val adapter = ArrayAdapter(this@Annex2F_Immediate_1_Activity, android.R.layout.simple_list_item_1, regionNames)
                        binding.spinnerRegion.setAdapter(adapter)

                        binding.spinnerRegion.setOnItemClickListener { _, _, position, _ ->
                            selectedRegionId = regions[position].region_id
                            binding.titleRegion.error = null

                            // Reset district when region changes
                            selectedDistrictId = null
                            binding.spinnerDistrict.setText("", false)
                            loadDistricts(selectedRegionId!!)
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<HealthRegions>>>, t: Throwable) {
                    binding.titleRegion.error = "Check internet connection"
                }
            })
    }

    private fun loadDistricts(regionId: Int) {
        ApiClient.getClient(this).getDistrictsByRegion(regionId)
            .enqueue(object : Callback<ApiResponse<List<HealthDistricts>>> {
                override fun onResponse(call: Call<ApiResponse<List<HealthDistricts>>>, response: Response<ApiResponse<List<HealthDistricts>>>) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        districts = response.body()!!.data!!
                        val districtNames = districts.map { it.district_name ?: "Unknown District" }
                        val adapter = ArrayAdapter(this@Annex2F_Immediate_1_Activity, android.R.layout.simple_list_item_1, districtNames)
                        binding.spinnerDistrict.setAdapter(adapter)

                        binding.spinnerDistrict.setOnItemClickListener { _, _, position, _ ->
                            selectedDistrictId = districts[position].district_id
                            binding.titleDistrict.error = null
                        }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<List<HealthDistricts>>>, t: Throwable) {
                    binding.titleDistrict.error = "Error loading districts"
                }
            })
    }

    private fun validateImmediate1Form(): Boolean {
        var isValid = true

        fun checkEmpty(et: android.widget.EditText, til: com.google.android.material.textfield.TextInputLayout, msg: String) {
            if (et.text.toString().trim().isEmpty()) {
                til.error = msg
                isValid = false
            } else til.error = null
        }

        checkEmpty(binding.etRecordId, binding.titleRecordId, "Required")
        checkEmpty(binding.etSite, binding.titleSite, "Required")
        checkEmpty(binding.etDisease, binding.titleDisease, "Required")

        if (binding.spinnerRegion.text.isEmpty()) {
            binding.titleRegion.error = "Required"
            isValid = false
        }
        if (selectedDistrictId == null) {
            binding.titleDistrict.error = "Required"
            isValid = false
        }
        if (binding.spinnerInpatientOutpatient.text.isEmpty()) {
            binding.titleInpatientOutpatient.error = "Required"
            isValid = false
        }

        return isValid
    }

    private fun setUpFieldListener() {
        binding.etRecordId.addTextChangedListener { binding.titleRecordId.error = null }
        binding.etSite.addTextChangedListener { binding.titleSite.error = null }
        binding.etDisease.addTextChangedListener { binding.titleDisease.error = null }
    }

    private fun passDataToNextScreen() {
        val annex2FReports = immediateReportForm(
            recordId             = binding.etRecordId.text.toString().trim(),
            country              = "Gambia",
            province             = binding.spinnerRegion.text.toString().trim(),
            district             = selectedDistrictId!!, // Pass ID
            site                 = binding.etSite.text.toString().trim(),
            disease              = binding.etDisease.text.toString().trim(),
            inpatientOutpatient  = binding.spinnerInpatientOutpatient.text.toString().trim(),


            dateSeen = "", patientName = "", dateOfBirth = "", age = 0, gender = "",
            address = "", districtAnnex2 = "", urbanRural = "", phoneNumber = "",
            occupation = "", dateOfOnset = "", travelHistory = "No", destination = "",
            vaccineDoses = "0", dateLastVaccine = "", dateSpecimen = "", dateLab = "",
            labResults = "", outcome = "", classification = "", dateFacilityNotified = "",
            dateSentDistrict = "", reporterName = ""
        )

        val intent = Intent(this, Annex2F_Immediate_2_Activity::class.java).apply {
            putExtra("Annex2FReport", annex2FReports)
        }
        startActivity(intent)
    }
}