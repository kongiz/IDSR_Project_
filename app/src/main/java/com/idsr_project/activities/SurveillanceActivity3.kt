package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.idsr_project.Model.Diseases
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.surveillanceData
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivitySurveillance3Binding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SurveillanceActivity3 : AppCompatActivity() {

    private lateinit var binding: ActivitySurveillance3Binding
    private var receivedData: surveillanceData? = null
    private var diseasesList: ArrayList<Diseases> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySurveillance3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        retrieveActivityData()
        setupListeners()
    }

    private fun retrieveActivityData() {
        receivedData = intent.getParcelableExtra("SurveillanceData")
        diseasesList = intent.getParcelableArrayListExtra("UpdatedDiseases") ?: arrayListOf()

        if (receivedData == null) {
            Toast.makeText(this, "Error: Initial report data missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d("Surveillance3", "Received Data: $receivedData")
        Log.d("Surveillance3", "Received Diseases: ${diseasesList.size} items")
    }

    private fun setupListeners() {
        binding.btnBackSur3.setOnClickListener { finish() }

        binding.etU5Male.addTextChangedListener { calculateGrandTotal() }
        binding.etU5Female.addTextChangedListener { calculateGrandTotal() }
        binding.etA5Male.addTextChangedListener { calculateGrandTotal() }
        binding.etA5Female.addTextChangedListener { calculateGrandTotal() }

        binding.btnSubmit.setOnClickListener {
            calculateGrandTotal()
            if (validateInputs()) {
                submitSurveillanceReport()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.etOfficerName.text.isNullOrEmpty()) {
            binding.tilOfficerName.error = "Officer name is required"
            isValid = false
        } else binding.tilOfficerName.error = null

        if (binding.etDesignation.text.isNullOrEmpty()) {
            binding.tilDesignation.error = "Designation is required"
            isValid = false
        } else binding.tilDesignation.error = null

        return isValid
    }

    private fun calculateGrandTotal() {
        val total = getInt(binding.etU5Male) +
                getInt(binding.etU5Female) +
                getInt(binding.etA5Male) +
                getInt(binding.etA5Female)

        binding.tvGrandTotal.text = total.toString()
    }

    private fun getInt(editText: TextInputEditText): Int {
        return editText.text?.toString()?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    private fun prepareFinalData(): surveillanceData? {

        val base = receivedData ?: return null

        return surveillanceData(
            healthFacility = base.healthFacility,
            healthRegion = base.healthRegion,
            district = base.district,
            epiweek = base.epiweek,
            dateFrom = base.dateFrom,
            dateTo = base.dateTo,
            facilityGeo = base.facilityGeo,
            totConU5Male = getInt(binding.etU5Male),
            totConU5Female = getInt(binding.etU5Female),
            totConA5Male = getInt(binding.etA5Male),
            totConA5Female = getInt(binding.etA5Female),
            grandTotal = binding.tvGrandTotal.text.toString().toIntOrNull() ?: 0,
            officerComment = binding.etComments.text.toString().trim(),
            officerName = binding.etOfficerName.text.toString().trim(),
            designation = binding.etDesignation.text.toString().trim(),
            updatedDiseases = diseasesList
        )
    }

    private fun submitSurveillanceReport() {
        val finalReport = prepareFinalData() ?: run {
            Toast.makeText(this, "Submission failed: Missing data.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.i("FinalReport", "Submitting data: $finalReport")

        binding.btnSubmit.isEnabled = false
        val gson = Gson()
        val jsonString = gson.toJson(finalReport)
        println(jsonString)

        ApiClient.getClient(context = this).submitSurveillanceData(finalReport)
            .enqueue(object : Callback<ResponseApi> {
                override fun onResponse(call: Call<ResponseApi>, response: Response<ResponseApi>) {
                    binding.btnSubmit.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(
                            this@SurveillanceActivity3,
                            response.body()!!.msg,
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@SurveillanceActivity3, Success_Activity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("SubmitResponse", "Error ${response.code()}: $errorBody")
                        Toast.makeText(
                            this@SurveillanceActivity3,
                            "Submission failed: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                    binding.btnSubmit.isEnabled = true
                    Log.e("SubmitFailure", "Network Error: ${t.message}", t)
                    Toast.makeText(
                        this@SurveillanceActivity3,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
