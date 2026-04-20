package com.idsr_project.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.Annex2FData
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.api.ApiClient
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.databinding.ActivityAnnex2Fimmediate1Binding
import com.idsr_project.utils.EditModeExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Annex2F_Immediate_1_Activity : BaseActivity() {
    private lateinit var binding: ActivityAnnex2Fimmediate1Binding


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", Double.NaN)
            val lng = result.data?.getDoubleExtra("lng", Double.NaN)
            if (lat != null && lng != null && !lat.isNaN() && !lng.isNaN()) {
                binding.etCaseGeo.setText("$lat, $lng")
                binding.tilCaseGeo.error = null
            }
        }
    }


    private var regions            = listOf<HealthRegions>()
    private var districts          = listOf<HealthDistricts>()
    private var selectedRegionId:    Int? = null
    private var selectedDistrictId:  Int? = null


    private var isEditMode   = false
    private var editReportId = -1
    private var editData: Annex2FData? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupSpinners()
        loadRegions()
        setupFieldListeners()
        setupGeoButtons()

        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editData     = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)
        editReportId = editData?.id ?: -1

        if (isEditMode && editData != null) {
            binding.btnNextAnnex1.text = "Next (Editing)"
            prefillAnnex2F1(editData!!)
        }

        binding.btnBackAnnex1.setOnClickListener { finish() }
        binding.btnNextAnnex1.setOnClickListener {
            if (validateImmediate1Form()) passDataToNextScreen()
        }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Annex2F_Immediate_1_Activity")
    }


    private fun setupGeoButtons() {
        binding.btnUseCaseLocation.setOnClickListener { requestLocation() }

        binding.btnPickCaseOnMap.setOnClickListener {
            mapPickerLauncher.launch(
                Intent(this, MapPickerActivity::class.java)
            )
        }
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        binding.btnUseCaseLocation.isEnabled = false
        binding.btnUseCaseLocation.text      = "Fetching..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            binding.btnUseCaseLocation.isEnabled = true
            binding.btnUseCaseLocation.text      = "Use Current Location"

            if (location != null) {
                binding.etCaseGeo.setText("${location.latitude}, ${location.longitude}")
                binding.tilCaseGeo.error = null
            } else {
                Toast.makeText(this, "Location signal weak. Use Map Picker.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            binding.btnUseCaseLocation.isEnabled = true
            binding.btnUseCaseLocation.text      = "Use Current Location"
            Toast.makeText(this, "Failed to get location.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation()
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun setupSpinners() {
        val inpatientOutpatient = arrayOf("Inpatient", "Outpatient")
        binding.spinnerInpatientOutpatient.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, inpatientOutpatient)
        )
        binding.spinnerInpatientOutpatient.setOnItemClickListener { _, _, _, _ ->
            binding.titleInpatientOutpatient.error = null
        }
    }

    private fun loadRegions() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@Annex2F_Immediate_1_Activity)
                    .referenceDataDao()
                    .getAllRegions()
            }

            if (result.isEmpty()) {
                binding.titleRegion.error = "No regions available. Check connection."
                return@launch
            }

            regions = result.map { HealthRegions(it.id, it.name, "") }
            binding.spinnerRegion.setAdapter(
                ArrayAdapter(
                    this@Annex2F_Immediate_1_Activity,
                    android.R.layout.simple_list_item_1,
                    regions.map { it.region_name }
                )
            )
            binding.spinnerRegion.setOnItemClickListener { _, _, position, _ ->
                selectedRegionId   = regions[position].region_id
                selectedDistrictId = null
                binding.titleRegion.error = null
                binding.spinnerDistrict.setText("", false)
                loadDistricts(selectedRegionId!!)
            }

            if (isEditMode && editData != null) {
                val idx = regions.indexOfFirst { it.region_name == editData!!.region_name }
                if (idx >= 0) {
                    binding.spinnerRegion.setText(regions[idx].region_name, false)
                    selectedRegionId = regions[idx].region_id
                    loadDistricts(selectedRegionId!!)
                }
            }
        }
    }

    private fun loadDistricts(regionId: Int) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@Annex2F_Immediate_1_Activity)
                    .referenceDataDao()
                    .getDistrictsByRegion(regionId)
            }

            if (result.isEmpty()) {
                binding.titleDistrict.error = "No districts found for selected region."
                return@launch
            }

            districts = result.map { HealthDistricts(it.id, it.name, it.regionId) }
            binding.spinnerDistrict.setAdapter(
                ArrayAdapter(
                    this@Annex2F_Immediate_1_Activity,
                    android.R.layout.simple_list_item_1,
                    districts.map { it.district_name }
                )
            )
            binding.spinnerDistrict.setOnItemClickListener { _, _, position, _ ->
                selectedDistrictId = districts[position].district_id
                binding.titleDistrict.error = null
            }

            if (isEditMode && editData != null) {
                val idx = districts.indexOfFirst { it.district_id == editData!!.district_id }
                if (idx >= 0) {
                    binding.spinnerDistrict.setText(districts[idx].district_name, false)
                    selectedDistrictId = districts[idx].district_id
                }
            }
        }
    }

    private fun setupFieldListeners() {
        binding.etRecordId.addTextChangedListener { binding.titleRecordId.error = null }
        binding.etSite.addTextChangedListener     { binding.titleSite.error     = null }
        binding.etDisease.addTextChangedListener  { binding.titleDisease.error  = null }
    }



    private fun validateImmediate1Form(): Boolean {
        var isValid = true

        fun checkEmpty(
            text: String,
            layout: com.google.android.material.textfield.TextInputLayout,
            msg: String
        ) {
            if (text.trim().isEmpty()) { layout.error = msg; isValid = false }
            else layout.error = null
        }

        checkEmpty(binding.etRecordId.text.toString(), binding.titleRecordId, "Required")
        checkEmpty(binding.etSite.text.toString(),     binding.titleSite,     "Required")
        checkEmpty(binding.etDisease.text.toString(),  binding.titleDisease,  "Required")

        if (binding.spinnerRegion.text.isNullOrEmpty()) {
            binding.titleRegion.error = "Required"; isValid = false
        }
        if (selectedDistrictId == null) {
            binding.titleDistrict.error = "Required"; isValid = false
        }
        if (binding.spinnerInpatientOutpatient.text.isNullOrEmpty()) {
            binding.titleInpatientOutpatient.error = "Required"; isValid = false
        }

        if (binding.etCaseGeo.text.isNullOrEmpty()) {
            binding.tilCaseGeo.helperText = "Recommended for outbreak mapping"
        } else {
            binding.tilCaseGeo.helperText = null
        }

        return isValid
    }



    private fun prefillAnnex2F1(data: Annex2FData) {
        binding.etSite.setText(data.site ?: "")
        binding.etDisease.setText(data.disease ?: "")
        binding.spinnerInpatientOutpatient.setText(data.inpatientOutpatient ?: "", false)
        binding.etCaseGeo.setText(data.caseGeo ?: "")
    }



    private fun passDataToNextScreen() {
        val annex2FReports = immediateReportForm(
            recordId            = binding.etRecordId.text.toString().trim(),
            country             = "Gambia",
            province            = binding.spinnerRegion.text.toString().trim(),
            district            = selectedDistrictId!!,
            site                = binding.etSite.text.toString().trim(),
            disease             = binding.etDisease.text.toString().trim(),
            inpatientOutpatient = binding.spinnerInpatientOutpatient.text.toString().trim(),
            caseGeo             = binding.etCaseGeo.text.toString().trim(),


            dateSeen = "", patientName = "", dateOfBirth = "", age = 0, gender = "",
            address = "", districtAnnex2 = "", urbanRural = "", phoneNumber = "",
            occupation = "", dateOfOnset = "", travelHistory = "No", destination = "",
            vaccineDoses = "0", dateLastVaccine = "", dateSpecimen = "", dateLab = "",
            labResults = "", outcome = "", classification = "", dateFacilityNotified = "",
            dateSentDistrict = "", reporterName = ""
        )

        startActivity(
            Intent(this, Annex2F_Immediate_2_Activity::class.java).apply {
                putExtra("Annex2FReport", annex2FReports)
                putExtra(EditModeExtras.EXTRA_EDIT_MODE,      isEditMode)
                putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, editReportId)
                putExtra(EditModeExtras.EXTRA_EDIT_DATA,      editData)
            }
        )
    }
}