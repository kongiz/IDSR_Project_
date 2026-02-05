package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.R
import com.idsr_project.databinding.ActivityLaboratoryForm1Annex2GactivityBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.widget.addTextChangedListener
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.reportFormToLabWithSpecimen
import com.idsr_project.api.ApiClient
import com.idsr_project.utils.SessionManager

class Laboratory_Form_1_Annex2G_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityLaboratoryForm1Annex2GactivityBinding
    private val calendar = Calendar.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLaboratoryForm1Annex2GactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpFieldListener()


        binding.btnBackAnnex2G1.setOnClickListener {
            finish()
        }

        binding.rgSex.setOnCheckedChangeListener { group, checkedId ->
            val selected = findViewById<RadioButton>(checkedId)
            selected?.let {
                val gender = it.text.toString()
                Toast.makeText(this, "Gender: $gender", Toast.LENGTH_SHORT).show()
            }
        }
        binding.etDateSpecimenCollection.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateSpecimenCollection.setText(dateString)
            }
        }
        binding.etDateSpecimenSentLab.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateSpecimenSentLab.setText(dateString)
            }
        }


        val spinnerSpecimen: Spinner = findViewById(R.id.spinnerSpecimen)

        spinnerSpecimen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedOutcome = parent?.getItemAtPosition(position).toString()
                Toast.makeText(this@Laboratory_Form_1_Annex2G_Activity, "Selected Specimen: $selectedOutcome",
                    Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(this@Laboratory_Form_1_Annex2G_Activity, "No Specimen Selected",
                    Toast.LENGTH_SHORT).show()
            }

        }
        binding.btnLabHW1.setOnClickListener {
            if (validateLabForm1()) {

                val dateSpecimenCollection = binding.etDateSpecimenCollection.text.toString()
                val suspectedDisease = binding.etSuspectedDisease.text.toString()
                val specimenType = binding.spinnerSpecimen.selectedItem.toString()
                val specimenUniqueID = binding.etSpecienUniqueID.text.toString()
                val patientNameLab = binding.etPatientNameLab.text.toString()
                val sex = binding.rgSex.findViewById<RadioButton>(binding.rgSex.checkedRadioButtonId).text.toString()
                val age = binding.etAge.text.toString()
                val dateSpecimenSentLab = binding.etDateSpecimenSentLab.text.toString()
                val phoneNumber = binding.etPhoneNumber.text.toString()
                val email = binding.etEmailClinician.text.toString()

                val healthWorkerLabFormRecord = mapOf(
                    "DATE_SPECIMEN_COLLECTION" to dateSpecimenCollection,
                    "SUSPECTED_DISEASE" to suspectedDisease,
                    "SPECIMEN_TYPE" to specimenType,
                    "SPECIMEN_UNIQUE_ID" to specimenUniqueID,
                    "PATIENT_NAME_LAB" to patientNameLab,
                    "SEX" to sex,
                    "AGE" to age,
                    "DATE_SPECIMEN_SENT_LAB" to dateSpecimenSentLab,
                    "PHONE_NUMBER" to phoneNumber,
                    "EMAIL" to email
                )
                submitForm(healthWorkerLabFormRecord)


            }
        }
    }
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onDateSelected(dateFormat.format(selectedCal.time))
            },
            year, month, day
        )
        datePickerDialog.show()
    }
    private fun validateLabForm1(): Boolean {
        var isValid = true

        if (binding.etDateSpecimenCollection.text.isNullOrEmpty()) {
            binding.tilDateSpecimenCollection.error = "Date of specimen collection is required"
            isValid = false
        } else {
            binding.tilDateSpecimenCollection.error = null
        }

        if (binding.etSuspectedDisease.text.isNullOrEmpty()) {
            binding.tilSuspectedDisease.error = "Suspected Disease or Condition is required"
            isValid = false
        } else {
            binding.tilSuspectedDisease.error = null
        }
        if (binding.spinnerSpecimen.selectedItem == null ||
            binding.spinnerSpecimen.selectedItem.toString().isEmpty()){
            Toast.makeText(this, "Select Specimen Type", Toast.LENGTH_SHORT).show()
            isValid = false

        } else {
            binding.tilSpecimenType.error = null
        }
        if (binding.etSpecienUniqueID.text.isNullOrEmpty()) {
            binding.tilSpecienUniqueID.error = "Specimen unique identifier is required"
            isValid = false
        } else {
            binding.tilSpecienUniqueID.error = null
        }
        if (binding.etPatientNameLab.text.isNullOrEmpty()) {
            binding.tilPatientNameLab.error = "Patient Name (s) is required"
            isValid = false
        } else {
            binding.tilPatientNameLab.error = null
        }
        if (binding.rgSex.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Select Gender", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            binding.tilSex.error = null
        }
        if (binding.etAge.text.isNullOrEmpty()) {
            binding.tilAge.error = "Patient Age is required"
            isValid = false
        } else {
            binding.tilAge.error = null
        }
        if (binding.etDateSpecimenSentLab.text.isNullOrEmpty()) {
            binding.tilDateSpecimenSentLab.error = "Date Specimen sent to laboratory is required"
            isValid = false
        } else {
            binding.tilDateSpecimenSentLab.error = null
        }
        if (binding.etPhoneNumber.text.isNullOrEmpty()) {
            binding.tilPhoneNumber.error = "Phone of clinician is required"
            isValid = false
        } else {
            binding.tilPhoneNumber.error = null
        }
        return isValid
    }
    private fun setUpFieldListener() {
        val fields = listOf(
            binding.tilDateSpecimenCollection to binding.etDateSpecimenCollection,
            binding.tilSuspectedDisease to binding.etSuspectedDisease,
            binding.tilSpecienUniqueID to binding.etSpecienUniqueID,
            binding.tilPatientNameLab to binding.etPatientNameLab,
            binding.tilAge to binding.etAge,
            binding.tilDateSpecimenSentLab to binding.etDateSpecimenSentLab,
            binding.tilPhoneNumber to binding.etPhoneNumber
        )
        for ((layout, editText) in fields) {
            editText.addTextChangedListener() {
                if (!it.isNullOrEmpty()) {
                    layout.error = null
                }


            }
        }

    }
    private fun submitForm(record: Map<String, Any?>) {
        val reportForm = reportFormToLabWithSpecimen(
            dateSpecimenCollect = record["DATE_SPECIMEN_COLLECTION"].toString(),
            suspectedDisease = record["SUSPECTED_DISEASE"].toString(),
            specimenType = record["SPECIMEN_TYPE"].toString(),
            specimenUniqueID = record["SPECIMEN_UNIQUE_ID"].toString(),
            patientNameLab = record["PATIENT_NAME_LAB"].toString(),
            sex = record["SEX"].toString(),
            age = record["AGE"].toString(),
            dateSpecimenSentLab = record["DATE_SPECIMEN_SENT_LAB"].toString(),
            phoneNumber = record["PHONE_NUMBER"].toString(),
            emailClinician = record["EMAIL"].toString()
        )
        ApiClient.getClient(context = this).submitLabFormWithspecimen(reportForm).enqueue(object : retrofit2.Callback<ResponseApi> {
            override fun onResponse(call: retrofit2.Call<ResponseApi>, response: retrofit2.Response<ResponseApi>) {
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@Laboratory_Form_1_Annex2G_Activity,
                        "Form submitted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(
                        this@Laboratory_Form_1_Annex2G_Activity,
                        Success_Activity::class.java
                    )
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(
                        this@Laboratory_Form_1_Annex2G_Activity,
                        "Failed to submit form",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            override fun onFailure(call: retrofit2.Call<ResponseApi>, t: Throwable) {
                Toast.makeText(this@Laboratory_Form_1_Annex2G_Activity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}