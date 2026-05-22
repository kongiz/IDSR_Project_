package com.idsr_project.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.FormData
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthFacilities
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.surveillanceData
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.databinding.ActivitySurveillance1Binding
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.applyWindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class SurveillanceActivity1 : BaseActivity() {
    private lateinit var binding: ActivitySurveillance1Binding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private var regionsList: List<HealthRegions> = emptyList()
    private var districtsList: List<HealthDistricts> = emptyList()
    private var facilitiesList: List<HealthFacilities> = emptyList()


    private var selectedRegionId: Int? = null
    private var selectedDistrictId: Int? = null
    private var selectedFacilityId: Int? = null

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val lat = data?.getDoubleExtra("lat", Double.NaN)
            val lng = data?.getDoubleExtra("lng", Double.NaN)
            if (lat != null && lng != null && !lat.isNaN() && !lng.isNaN()) {
                binding.facilityGeo.setText("$lat, $lng")
            }
        }
    }

    private var isEditMode    = false
    private var editReportId  = -1
    private var editFormData: FormData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySurveillance1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(
            topView    = binding.appBarLayout,
            bottomView = binding.btnNextSur1
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        setupClickListeners()
        loadRegions()
        setupValidationListeners()

        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editFormData = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)
        editReportId = editFormData?.id ?: -1

        if (isEditMode && editFormData != null) {
            supportActionBar?.title = "Edit Surveillance Report"
            binding.btnNextSur1.text = "Next (Editing)"
            prefillSurveillance1(editFormData!!)
        }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "SurveillanceActivity1")
    }


    private fun setupClickListeners() {
        binding.btnBackSur1.setOnClickListener { finish() }

        binding.btnNextSur1.setOnClickListener {
            if (validateForm()) {
                moveDataToNextScreen()
            }
        }

        binding.btnUseCurrentLocation.setOnClickListener { requestLocation() }

        binding.btnPickOnMap.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        binding.etDateFrom.setOnClickListener { showDatePickerDialog(binding.etDateFrom) }
        binding.etDateTo.setOnClickListener { showDatePickerDialog(binding.etDateTo) }
    }

    private fun prefillSurveillance1(form: FormData) {
        binding.etEpiweek.setText(form.epiweek ?: "")
        binding.etDateFrom.setText(formatDateOnly(form.date_from))
        binding.etDateTo.setText(formatDateOnly(form.date_to))
        binding.facilityGeo.setText(form.facility_geo ?: "")
    }
    private fun formatDateOnly(isoDate: String?): String {
        if (isoDate.isNullOrEmpty()) return ""
        return try {
            if (isoDate.contains("T")) isoDate.substring(0, 10) else isoDate
        } catch (e: Exception) { isoDate }
    }


    private fun loadRegions() {
        lifecycleScope.launch {
            val regions = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@SurveillanceActivity1)
                    .referenceDataDao()
                    .getAllRegions()
            }

            if (regions.isEmpty()) {
                Toast.makeText(this@SurveillanceActivity1, "No regions available. Check your connection.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            regionsList = regions.map { HealthRegions(it.id, it.name, "") }
            val regionNames = regionsList.map { it.region_name }
            val adapter = ArrayAdapter(this@SurveillanceActivity1, android.R.layout.simple_list_item_1, regionNames)
            binding.spinnerRegion.setAdapter(adapter)
            binding.spinnerRegion.setOnClickListener { binding.spinnerRegion.showDropDown() }
            binding.spinnerRegion.setOnItemClickListener { _, _, position, _ ->
                selectedRegionId = regionsList[position].region_id
                binding.spinnerDistrict.setText("", false)
                binding.spinnerFacility.setText("", false)
                selectedDistrictId = null
                selectedFacilityId = null
                districtsList = emptyList()
                facilitiesList = emptyList()
                loadDistricts(selectedRegionId!!)
            }

            if (isEditMode && editFormData != null) {
                val idx = regionsList.indexOfFirst { it.region_name == editFormData!!.region_name }
                if (idx >= 0) {
                    binding.spinnerRegion.setText(regionsList[idx].region_name, false)
                    selectedRegionId = regionsList[idx].region_id
                    loadDistricts(selectedRegionId!!)
                }
            }
        }
    }


    private fun loadDistricts(regionId: Int) {
        lifecycleScope.launch {
            val districts = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@SurveillanceActivity1)
                    .referenceDataDao()
                    .getDistrictsByRegion(regionId)
            }

            if (districts.isEmpty()) {
                Toast.makeText(this@SurveillanceActivity1, "No districts found for selected region.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            districtsList = districts.map { HealthDistricts(it.id, it.name, it.regionId) }
            val districtNames = districtsList.map { it.district_name }
            val adapter = ArrayAdapter(this@SurveillanceActivity1, android.R.layout.simple_list_item_1, districtNames)
            binding.spinnerDistrict.setAdapter(adapter)
            binding.spinnerDistrict.setOnClickListener { binding.spinnerDistrict.showDropDown() }
            binding.spinnerDistrict.setOnItemClickListener { _, _, position, _ ->
                selectedDistrictId = districtsList[position].district_id
                binding.spinnerFacility.setText("", false)
                selectedFacilityId = null
                facilitiesList = emptyList()
                loadFacilities(selectedDistrictId!!)
            }

            if (isEditMode && editFormData != null) {
                val idx = districtsList.indexOfFirst { it.district_name == editFormData!!.district_name }
                if (idx >= 0) {
                    binding.spinnerDistrict.setText(districtsList[idx].district_name, false)
                    selectedDistrictId = districtsList[idx].district_id
                    loadFacilities(selectedDistrictId!!)
                }
            }
        }
    }


    private fun loadFacilities(districtId: Int) {
        lifecycleScope.launch {
            val facilities = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@SurveillanceActivity1)
                    .referenceDataDao()
                    .getFacilitiesByDistrict(districtId)
            }

            if (facilities.isEmpty()) {
                Toast.makeText(this@SurveillanceActivity1, "No facilities found for selected district.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            facilitiesList = facilities.map { HealthFacilities(it.id, it.name, it.districtId) }
            val facilityNames = facilitiesList.map { it.facility_name }
            val adapter = ArrayAdapter(this@SurveillanceActivity1, android.R.layout.simple_list_item_1, facilityNames)
            binding.spinnerFacility.setAdapter(adapter)
            binding.spinnerFacility.setOnClickListener { binding.spinnerFacility.showDropDown() }
            binding.spinnerFacility.setOnItemClickListener { _, _, position, _ ->
                selectedFacilityId = facilitiesList[position].facility_id
            }

            if (isEditMode && editFormData != null) {
                val idx = facilitiesList.indexOfFirst { it.facility_name == editFormData!!.facility_name }
                if (idx >= 0) {
                    binding.spinnerFacility.setText(facilitiesList[idx].facility_name, false)
                    selectedFacilityId = facilitiesList[idx].facility_id
                }
            }
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = calendar.time

                if (calendar.after(today)) {
                    Toast.makeText(this, "Date cannot be in the future", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }

                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                when (editText.id) {
                    R.id.etDateFrom -> {
                        if (dayOfWeek != Calendar.MONDAY) {
                            Toast.makeText(this, "Date From must be a Monday (start of the week)", Toast.LENGTH_LONG).show()
                            return@DatePickerDialog
                        }
                    }
                    R.id.etDateTo -> {
                        if (dayOfWeek != Calendar.SUNDAY) {
                            Toast.makeText(this, "Date To must be a Sunday (end of the week)", Toast.LENGTH_LONG).show()
                            return@DatePickerDialog
                        }

                        val dateFromStr = binding.etDateFrom.text.toString()
                        if (dateFromStr.isNotEmpty()) {
                            try {
                                val dateFrom = dateFormat.parse(dateFromStr)
                                if (dateFrom != null && selectedDate.before(dateFrom)) {
                                    Toast.makeText(this, "Date To must be after Date From", Toast.LENGTH_SHORT).show()
                                    return@DatePickerDialog
                                }

                                val diffInMillis = selectedDate.time - dateFrom!!.time
                                val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

                                if (diffInDays != 6) {
                                    Toast.makeText(this, "Date To must be exactly 6 days after Date From (same week)", Toast.LENGTH_LONG).show()
                                    return@DatePickerDialog
                                }
                            } catch (e: Exception) {
                                Log.e("DateValidation", "Error parsing date: ${e.message}")
                            }
                        }
                    }
                }

                editText.setText(dateFormat.format(calendar.time))

                if (editText.id == R.id.etDateFrom) {
                    autoFillDateTo(dateFormat.format(calendar.time))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = today.timeInMillis
        datePickerDialog.show()
    }

    private fun autoFillDateTo(dateFromStr: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateFrom = dateFormat.parse(dateFromStr)
            if (dateFrom != null) {
                val calendar = Calendar.getInstance()
                calendar.time = dateFrom
                calendar.add(Calendar.DAY_OF_MONTH, 6)
                if (binding.etDateTo.text.toString().isEmpty()) {
                    binding.etDateTo.setText(dateFormat.format(calendar.time))
                    Toast.makeText(this, "Date To auto-filled (Sunday of the same week)", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("AutoFill", "Error auto-filling Date To: ${e.message}")
        }
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        binding.btnUseCurrentLocation.isEnabled = false
        binding.btnUseCurrentLocation.text = "Fetching..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            binding.btnUseCurrentLocation.isEnabled = true
            binding.btnUseCurrentLocation.text = "Use Current Location"

            if (location != null) {
                binding.facilityGeo.setText("${location.latitude}, ${location.longitude}")
                binding.tilFacilityGeo.error = null
            } else {
                Toast.makeText(this, "Location signal weak. Use Map Picker.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            binding.btnUseCurrentLocation.isEnabled = true
            binding.btnUseCurrentLocation.text = "Use Current Location"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        binding.tilHealthFacility.error = null
        binding.tilEpiweek.error = null
        binding.tilDateFrom.error = null
        binding.tilDateTo.error = null
        binding.tilFacilityGeo.error = null

        if (binding.spinnerRegion.text.toString().isEmpty()) {
            Toast.makeText(this, "Please select a health region", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (binding.spinnerDistrict.text.toString().isEmpty()) {
            Toast.makeText(this, "Please select a district", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (binding.spinnerFacility.text.toString().isEmpty() || selectedFacilityId == null) {
            binding.tilHealthFacility.error = "Please select a health facility"
            isValid = false
        }

        if (binding.etEpiweek.text.toString().isEmpty()) {
            binding.tilEpiweek.error = "Epiweek No. is required"
            isValid = false
        } else {
            val week = binding.etEpiweek.text.toString().toInt()
            if (week !in 1..53) {
                binding.tilEpiweek.error = "Epiweek must be between 1 and 53"
                isValid = false
            }
        }

        if (binding.etDateFrom.text.toString().isEmpty()) {
            binding.tilDateFrom.error = "Date From is required"
            isValid = false
        }

        if (binding.etDateTo.text.toString().isEmpty()) {
            binding.tilDateTo.error = "Date To is required"
            isValid = false
        }

        if (binding.etDateFrom.text.toString().isNotEmpty() && binding.etDateTo.text.toString().isNotEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val dateFrom = dateFormat.parse(binding.etDateFrom.text.toString())
                val dateTo   = dateFormat.parse(binding.etDateTo.text.toString())
                val today    = Calendar.getInstance().time

                if (dateFrom != null && dateFrom.after(today)) {
                    binding.tilDateFrom.error = "Date cannot be in the future"
                    isValid = false
                }

                if (dateTo != null && dateTo.after(today)) {
                    binding.tilDateTo.error = "Date cannot be in the future"
                    isValid = false
                }

                if (dateFrom != null && dateTo != null && dateTo.before(dateFrom)) {
                    binding.tilDateTo.error = "Date To must be after Date From"
                    isValid = false
                }

                if (dateFrom != null && dateTo != null && isValid) {
                    val calFrom = Calendar.getInstance().apply { time = dateFrom }
                    val calTo   = Calendar.getInstance().apply { time = dateTo }

                    if (calFrom.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                        binding.tilDateFrom.error = "Date From must be a Monday"
                        isValid = false
                    }

                    if (calTo.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        binding.tilDateTo.error = "Date To must be a Sunday"
                        isValid = false
                    }

                    val diffInDays = ((dateTo.time - dateFrom.time) / (1000 * 60 * 60 * 24)).toInt()
                    if (diffInDays != 6) {
                        binding.tilDateTo.error = "Must be exactly one week (Monday to Sunday)"
                        isValid = false
                    }
                }
            } catch (e: Exception) {
                binding.tilDateFrom.error = "Invalid date format"
                Log.e("DateValidation", "Error: ${e.message}")
                isValid = false
            }
        }

        return isValid
    }

    private fun setupValidationListeners() {

        binding.etEpiweek.doAfterTextChanged { it ->
            if (!it.isNullOrBlank()) {
                val week = it.toString().toIntOrNull()
                if (week != null && week in 1..53) {
                    binding.tilEpiweek.error = null
                } else {
                    binding.tilEpiweek.error = "Enter a valid week (1-53)"
                }
            }
        }

        binding.spinnerRegion.doAfterTextChanged { binding.spinnerRegion.error = null }
        binding.spinnerDistrict.doAfterTextChanged { binding.spinnerDistrict.error = null }
        binding.spinnerFacility.doAfterTextChanged { binding.tilHealthFacility.error = null }
    }

    private fun moveDataToNextScreen() {
        binding.btnNextSur1.isEnabled = false

        val surveillance1Details = surveillanceData(
            healthFacility  = binding.spinnerFacility.text.toString().trim(),
            healthRegion    = binding.spinnerRegion.text.toString().trim(),
            district        = binding.spinnerDistrict.text.toString().trim(),
            epiweek         = binding.etEpiweek.text.toString().trim(),
            dateFrom        = binding.etDateFrom.text.toString().trim(),
            dateTo          = binding.etDateTo.text.toString().trim(),
            facilityGeo     = binding.facilityGeo.text.toString().trim(),
            facilityId      = selectedFacilityId ?: 0,
            regionId        = selectedRegionId ?: 0,
            districtId      = selectedDistrictId ?: 0,
            totConU5Male    = 0,
            totConU5Female  = 0,
            totConA5Male    = 0,
            totConA5Female  = 0,
            grandTotal      = 0,
            officerComment  = "",
            officerName     = "",
            designation     = "",
            updatedDiseases = arrayListOf()
        )

        val intent = Intent(this, SurveillanceActivity2::class.java).apply {
            putExtra("SurveillanceData", surveillance1Details)
            putExtra(EditModeExtras.EXTRA_EDIT_MODE, isEditMode)
            putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, editReportId)
            putExtra(EditModeExtras.EXTRA_EDIT_DATA, editFormData)
        }
        startActivity(intent)

        binding.root.postDelayed( {binding.btnNextSur1.isEnabled = true}, 1000)
    }
}