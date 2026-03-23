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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthFacilities
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.surveillanceData
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivitySurveillance1Binding
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class SurveillanceActivity1 : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySurveillance1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupClickListeners()
        loadRegions()
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


    private fun loadRegions() {
        val api = ApiClient.getClient(context = this)
        api.getRegions().enqueue(object : retrofit2.Callback<ApiResponse<List<HealthRegions>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<HealthRegions>>?>,
                response: Response<ApiResponse<List<HealthRegions>>?>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    regionsList = response.body()!!.data!!
                    val regionNames = regionsList.map { it.region_name }
                    val adapter = ArrayAdapter(
                        this@SurveillanceActivity1,
                        android.R.layout.simple_list_item_1,
                        regionNames
                    )
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
                } else {
                    Toast.makeText(this@SurveillanceActivity1, "Failed to load regions", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<HealthRegions>>?>, t: Throwable) {
                Toast.makeText(this@SurveillanceActivity1, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun loadDistricts(regionId: Int) {
        val api = ApiClient.getClient(context = this)
        api.getDistrictsByRegion(regionId)
            .enqueue(object : retrofit2.Callback<ApiResponse<List<HealthDistricts>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<HealthDistricts>>?>,
                    response: Response<ApiResponse<List<HealthDistricts>>?>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        districtsList = response.body()!!.data!!
                        val districtNames = districtsList.map { it.district_name }
                        val adapter = ArrayAdapter(
                            this@SurveillanceActivity1,
                            android.R.layout.simple_list_item_1,
                            districtNames
                        )
                        binding.spinnerDistrict.setAdapter(adapter)
                        binding.spinnerDistrict.setOnClickListener { binding.spinnerDistrict.showDropDown() }
                        binding.spinnerDistrict.setOnItemClickListener { _, _, position, _ ->
                            selectedDistrictId = districtsList[position].district_id


                            binding.spinnerFacility.setText("", false)
                            selectedFacilityId = null
                            facilitiesList = emptyList()

                            loadFacilities(selectedDistrictId!!)
                        }
                    } else {
                        Toast.makeText(this@SurveillanceActivity1, "Failed to load districts", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<HealthDistricts>>?>, t: Throwable) {
                    Toast.makeText(this@SurveillanceActivity1, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun loadFacilities(districtId: Int) {
        val api = ApiClient.getClient(context = this)
        api.getFacilities(districtId)
            .enqueue(object : retrofit2.Callback<ApiResponse<List<HealthFacilities>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<HealthFacilities>>?>,
                    response: Response<ApiResponse<List<HealthFacilities>>?>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        facilitiesList = response.body()!!.data!!
                        val facilityNames = facilitiesList.map { it.facility_name }
                        val adapter = ArrayAdapter(
                            this@SurveillanceActivity1,
                            android.R.layout.simple_list_item_1,
                            facilityNames
                        )
                        binding.spinnerFacility.setAdapter(adapter)
                        binding.spinnerFacility.setOnClickListener { binding.spinnerFacility.showDropDown() }
                        binding.spinnerFacility.setOnItemClickListener { _, _, position, _ ->
                            selectedFacilityId = facilitiesList[position].facility_id
                        }
                    } else {
                        Toast.makeText(this@SurveillanceActivity1, "Failed to load facilities", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<HealthFacilities>>?>, t: Throwable) {
                    Toast.makeText(this@SurveillanceActivity1, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                binding.facilityGeo.setText("${location.latitude}, ${location.longitude}")
            } else {
                Toast.makeText(this, "Unable to get location. Try map picker instead.", Toast.LENGTH_SHORT).show()
            }
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

    private fun moveDataToNextScreen() {
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
        }
        startActivity(intent)
    }
}