package com.idsr_project.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
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
import com.idsr_project.Model.HealthRegions
import com.idsr_project.Model.surveillanceData
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.api.ApiServices
import com.idsr_project.databinding.ActivitySurveillance1Binding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale


class SurveillanceActivity1 : AppCompatActivity() {
    private lateinit var binding: ActivitySurveillance1Binding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001


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
        setupSpinners()
    }

    private fun setupClickListeners() {
        binding.btnBackSur1.setOnClickListener { finish() }

        binding.btnNextSur1.setOnClickListener {
            if (validateSurveillance1From()) {
                moveDataToNextScreen()
            }
        }

        binding.btnUseCurrentLocation.setOnClickListener{ requestLocation() }

        binding.btnPickOnMap.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        binding.etDateFrom.setOnClickListener { showDatePickerDialog(binding.etDateFrom) }
        binding.etDateTo.setOnClickListener { showDatePickerDialog(binding.etDateTo) }
    }
    private fun setupSpinners() {
        val api = ApiClient.getClient(context = this)
        api.getRegions().enqueue(object : retrofit2.Callback<ApiResponse<List<HealthRegions>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<HealthRegions>>?>,
                response: Response<ApiResponse<List<HealthRegions>>?>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val regions = response.body()!!.data ?: emptyList()

                    val regionName = regions.map { it.region_name ?: "Unknown Region" }

                    val regionAdapter = ArrayAdapter(
                        this@SurveillanceActivity1,
                        android.R.layout.simple_spinner_item,
                        regionName
                    )
                    regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerRegion.adapter = regionAdapter

                    binding.spinnerRegion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selectedRegionId = regions[position].regions_id
                            loadDistricts(selectedRegionId)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                } else {
                        Toast.makeText(this@SurveillanceActivity1, "Failed to load regions", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<HealthRegions>>?>, t: Throwable) {
                Toast.makeText(this@SurveillanceActivity1, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("SurveillanceActivity1", "Error loading regions: ${t.message}")
            }
        })
    }
    private fun loadDistricts(regionId: Int) {
        val api = ApiClient.getClient(context = this)
        api.getDistrictsByRegion(regionId).enqueue(object : retrofit2.Callback<ApiResponse<List<HealthDistricts>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<HealthDistricts>>?>,
                response: Response<ApiResponse<List<HealthDistricts>>?>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val districts = response.body()!!.data ?: emptyList()
                    val districtNames = districts.map { it.district_name ?: "Unknown District" }

                    val districtAdapter = ArrayAdapter(
                        this@SurveillanceActivity1,
                        android.R.layout.simple_spinner_item,
                        districtNames
                    )
                    districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerDistrict.adapter = districtAdapter
                } else {
                    Toast.makeText(this@SurveillanceActivity1, "Failed to load districts", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<HealthDistricts>>?>, t: Throwable) {
                Toast.makeText(this@SurveillanceActivity1, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("SurveillanceActivity1", "Error loading districts: ${t.message}")
            }
        })
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val datePickerDialog = DatePickerDialog(this,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                calendar.set(year, month, dayOfMonth)
                editText.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
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
                val geoText = "${location.latitude}, ${location.longitude}"
                binding.facilityGeo.setText(geoText)
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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Location permission denied. Cannot use current location.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateSurveillance1From() : Boolean {
        var isValid = true

        if (binding.etHealthFacility.text.isNullOrEmpty()) {
            binding.etHealthFacility.error = "Health Facility is required"
            isValid = false
        } else {
            binding.tilHealthFacility.error = null
        }
        if (binding.etEpiweek.text.toString().isEmpty()) {
            binding.etEpiweek.error = "Epiweek No. is required"
            isValid = false
        }
        if (binding.etDateFrom.text.toString().isEmpty()) {
            binding.etDateFrom.error = "Date From is required"
            isValid = false
        }
        if (binding.etDateTo.text.toString().isEmpty()) {
            binding.etDateTo.error = "Date To is required"
            isValid = false
        }

        if (binding.spinnerRegion.selectedItemPosition == AdapterView.INVALID_POSITION) {
            Toast.makeText(this, "Please select a health region", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (binding.spinnerDistrict.selectedItemPosition == AdapterView.INVALID_POSITION) {
            Toast.makeText(this, "Please select a district", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun moveDataToNextScreen() {
        val healthFacility = binding.etHealthFacility.text.toString().trim()
        val healthRegion = binding.spinnerRegion.selectedItem.toString()
        val district = binding.spinnerDistrict.selectedItem.toString()
        val epiweek = binding.etEpiweek.text.toString().trim()
        val dateFrom = binding.etDateFrom.text.toString().trim()
        val dateTo = binding.etDateTo.text.toString().trim()
        val facilityGeo = binding.facilityGeo.text.toString().trim()


        val surveillance1Details = surveillanceData(
            healthFacility = healthFacility,
            healthRegion = healthRegion,
            district = district,
            epiweek = epiweek,
            dateFrom = dateFrom,
            dateTo = dateTo,
            facilityGeo = facilityGeo,
            totConU5Male = 0,
            totConU5Female = 0,
            totConA5Male = 0,
            totConA5Female = 0,
            grandTotal = 0,
            officerComment = "",
            officerName = "",
            designation = "",
            updatedDiseases = arrayListOf()
        )

        val intent = Intent(this@SurveillanceActivity1, SurveillanceActivity2::class.java).apply {
            putExtra("SurveillanceData", surveillance1Details)
        }
        startActivity(intent)
    }
}