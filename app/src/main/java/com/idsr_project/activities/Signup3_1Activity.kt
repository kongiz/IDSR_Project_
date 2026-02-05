package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.ApiResponse
import com.idsr_project.Model.HealthDistricts
import com.idsr_project.Model.HealthRegions
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivitySignup31Binding
import com.idsr_project.utils.ThemeManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Signup3_1Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySignup31Binding

    private var regions: List<HealthRegions> = emptyList()
    private var districts: List<HealthDistricts> = emptyList()

    private var selectedRegionId: Int? = null
    private var selectedDistrictId: Int? = null

    private lateinit var roleToCreate: String
    private lateinit var registerMode: String
    private lateinit var currentUserRole: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignup31Binding.inflate(layoutInflater)
        setContentView(binding.root)

        roleToCreate = intent.getStringExtra("ROLE") ?: ""
        registerMode = intent.getStringExtra("REGISTER_MODE") ?: "SELF"
        currentUserRole = intent.getStringExtra("CURRENT_USER_ROLE") ?: ""

        binding.btnBackSignup31.setOnClickListener { finish() }

        setupUIBasedOnRole()
        setupClearErrorListeners()

        binding.btnNext31.setOnClickListener { onNextPressed() }
    }

    private fun setupClearErrorListeners() {
        binding.actRegion.setOnItemClickListener { _, _, _, _ ->
            binding.tilRegion.error = null
        }

        binding.actDistrict.setOnItemClickListener { _, _, _, _ ->
            binding.tilDistrict.error = null
        }
    }

    private fun setupUIBasedOnRole() {
        when (roleToCreate) {

            "Admin" -> {
                binding.tilRegion.visibility = View.GONE
                binding.tilDistrict.visibility = View.GONE
            }

            "Regional Officer" -> {
                binding.tilRegion.visibility = View.VISIBLE
                binding.tilDistrict.visibility = View.GONE
                loadRegions()
            }

            "District Officer" -> {
                binding.tilRegion.visibility = View.VISIBLE
                binding.tilDistrict.visibility = View.VISIBLE
                loadRegions()
            }

            else -> {
                binding.tilRegion.visibility = View.VISIBLE
                binding.tilDistrict.visibility = View.VISIBLE
                loadRegions()
            }
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
                        binding.tilRegion.error = "Failed to load regions"
                        return
                    }

                    regions = response.body()!!.data!!
                    val regionNames = regions.map { it.region_name ?: "Region" }

                    val adapter = ArrayAdapter(
                        this@Signup3_1Activity,
                        android.R.layout.simple_list_item_1,
                        regionNames
                    )

                    binding.actRegion.setAdapter(adapter)

                    binding.actRegion.setOnItemClickListener { _, _, position, _ ->
                        selectedRegionId = regions[position].regions_id
                        binding.tilRegion.error = null

                        // Reset district
                        selectedDistrictId = null
                        binding.actDistrict.setText("", false)

                        if (roleToCreate == "District Officer" || registerMode == "SELF") {
                            loadDistricts(selectedRegionId!!)
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<List<HealthRegions>>>,
                    t: Throwable
                ) {
                    Log.e("Signup3_1Activity", "Error loading regions", t)
                    binding.tilRegion.error = "Error loading regions"
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
                        binding.tilDistrict.error = "Failed to load districts"
                        return
                    }

                    districts = response.body()!!.data!!
                    val districtNames = districts.map { it.district_name ?: "District" }

                    val adapter = ArrayAdapter(
                        this@Signup3_1Activity,
                        android.R.layout.simple_list_item_1,
                        districtNames
                    )

                    binding.actDistrict.setAdapter(adapter)

                    binding.actDistrict.setOnItemClickListener { _, _, position, _ ->
                        selectedDistrictId = districts[position].district_id
                        binding.tilDistrict.error = null
                    }
                }

                override fun onFailure(
                    call: Call<ApiResponse<List<HealthDistricts>>>,
                    t: Throwable
                ) {
                    Log.e("Signup3_1Activity", "Error loading districts", t)
                    binding.tilDistrict.error = "Error loading districts"
                }
            })
    }

    private fun validate(): Boolean {
        return when (roleToCreate) {

            "Admin" -> true

            "Regional Officer" -> {
                if (selectedRegionId == null) {
                    binding.tilRegion.error = "Please select region"
                    false
                } else true
            }

            "District Officer" -> {
                when {
                    selectedRegionId == null -> {
                        binding.tilRegion.error = "Please select region"
                        false
                    }
                    selectedDistrictId == null -> {
                        binding.tilDistrict.error = "Please select district"
                        false
                    }
                    else -> true
                }
            }

            else -> {
                when {
                    selectedRegionId == null -> {
                        binding.tilRegion.error = "Please select region"
                        false
                    }
                    selectedDistrictId == null -> {
                        binding.tilDistrict.error = "Please select district"
                        false
                    }
                    else -> true
                }
            }
        }
    }

    private fun onNextPressed() {
        if (!validate()) return

        val next = Intent(this, SignUp4Activity::class.java)

        next.putExtra("FIRSTNAME", intent.getStringExtra("FIRSTNAME"))
        next.putExtra("LASTNAME", intent.getStringExtra("LASTNAME"))
        next.putExtra("PHONE", intent.getStringExtra("PHONE"))
        next.putExtra("EMAIL", intent.getStringExtra("EMAIL"))
        next.putExtra("GENDER", intent.getStringExtra("GENDER"))

        next.putExtra("ROLE", roleToCreate)
        next.putExtra("REGISTER_MODE", registerMode)
        next.putExtra("CURRENT_USER_ROLE", currentUserRole)

        next.putExtra("REGION_ID", selectedRegionId ?: -1)
        next.putExtra("DISTRICT_ID", selectedDistrictId ?: -1)

        next.putExtra("CREATOR_REGION_ID", intent.getIntExtra("CREATOR_REGION_ID", -1))
        next.putExtra("CREATOR_DISTRICT_ID", intent.getIntExtra("CREATOR_DISTRICT_ID", -1))

        startActivity(next)
    }
}
