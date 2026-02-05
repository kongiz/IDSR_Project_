package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.databinding.ActivityAnnex2Fimmediate2Binding
import com.idsr_project.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_2_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityAnnex2Fimmediate2Binding
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate2Binding.inflate(layoutInflater)
        setContentView(binding.root)


        setupGenderSpinner()


        setupResidenceSpinner()


        binding.btnBackAnnex1.setOnClickListener {
            finish()
        }


        binding.etDateSeen.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateSeen.setText(dateString)
            }
        }

        binding.etDateOfBirth.setOnClickListener {
            showDatePicker { dateString ->
                binding.etDateOfBirth.setText(dateString)


                val age = calculateAgeFromDOB(dateString)
                binding.tvAge.setText("$age years")
            }
        }


        binding.btnNextAnnex2.setOnClickListener {
            if (validateImmediate2Form()) {
                passDataToNextScreen()
            }
        }
    }

    private fun setupGenderSpinner() {
        val genderOptions = arrayOf("Male", "Female")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.spinnerGender.setAdapter(adapter)

        binding.spinnerGender.setOnItemClickListener { _, _, position, _ ->
            val selectedGender = genderOptions[position]
            Toast.makeText(this, "Gender: $selectedGender", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupResidenceSpinner() {
        val residenceOptions = arrayOf("Urban", "Rural")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, residenceOptions)
        binding.spinnerResidence.setAdapter(adapter)

        binding.spinnerResidence.setOnItemClickListener { _, _, position, _ ->
            val selectedResidence = residenceOptions[position]
            Toast.makeText(this, "Residence: $selectedResidence", Toast.LENGTH_SHORT).show()
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

    private fun calculateAgeFromDOB(dobString: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dob = dateFormat.parse(dobString) ?: return 0

        val dobCal = Calendar.getInstance().apply { time = dob }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    private fun validateImmediate2Form(): Boolean {
        var isValid = true


        if (binding.etDateSeen.text.isNullOrEmpty()) {
            binding.titleDateSeen.error = "Date seen  s required"
            isValid = false
        } else {
            binding.titleDateSeen.error = null
        }


        if (binding.etPatientName.text.isNullOrEmpty()) {
            binding.titlePatientName.error = "Patient Name(s) is required"
            isValid = false
        } else {
            binding.titlePatientName.error = null
        }


        if (binding.etDateOfBirth.text.isNullOrEmpty()) {
            binding.titleDateOfBirth.error = "Date of Birth is required"
            isValid = false
        } else {
            binding.titleDateOfBirth.error = null
        }

        // Age is auto-calculated, so we don't need to validate it separately
        // It will automatically be filled when DOB is selected


        if (binding.spinnerGender.text.isNullOrEmpty()) {
            binding.titleGender.error = "Please select Gender"
            isValid = false
        } else {
            binding.titleGender.error = null
        }


        if (binding.etAddress.text.isNullOrEmpty()) {
            binding.titleAddress.error = "Patient Address is required"
            isValid = false
        } else {
            binding.titleAddress.error = null
        }


        if (binding.etDistrictAnnex2.text.isNullOrEmpty()) {
            binding.titleDistrictResidence.error = "Name of District of residence is required"
            isValid = false
        } else {
            binding.titleDistrictResidence.error = null
        }


        if (binding.spinnerResidence.text.isNullOrEmpty()) {
            binding.titleResidence.error = "Please select Residence"
            isValid = false
        } else {
            binding.titleResidence.error = null
        }

        val phone = binding.etPhoneNumber.text?.toString()?.trim()
        if (phone.isNullOrEmpty()) {
            binding.titlePhoneNumber.error = "Patient Phone is required"
            isValid = false
        } else if (!phone.matches(Regex("^[0-9]{10}$"))) {
            binding.titlePhoneNumber.error = "Enter a valid 10-digit phone number"
            isValid = false
        } else {
            binding.titlePhoneNumber.error = null
        }


        if (binding.etOccupation.text.isNullOrEmpty()) {
            binding.titleOccupation.error = "Patient's Occupation is required"
            isValid = false
        } else {
            binding.titleOccupation.error = null
        }

        return isValid
    }

    private fun passDataToNextScreen() {
        val dateSeen = binding.etDateSeen.text.toString().trim()
        val patientName = binding.etPatientName.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val age = binding.tvAge.text.toString().trim()
        val gender = binding.spinnerGender.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val district = binding.etDistrictAnnex2.text.toString().trim()
        val residence = binding.spinnerResidence.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val occupation = binding.etOccupation.text.toString().trim()

        val receivedAnnex2FReport = intent.getParcelableExtra<immediateReportForm>("Annex2FReport")

        val annex2FReports = immediateReportForm(
            recordId = receivedAnnex2FReport?.recordId ?: "",
            country = receivedAnnex2FReport?.country ?: "",
            province = receivedAnnex2FReport?.province ?: "",
            district = receivedAnnex2FReport?.district ?: "",
            site = receivedAnnex2FReport?.site ?: "",
            disease = receivedAnnex2FReport?.disease ?: "",
            inpatientOutpatient = receivedAnnex2FReport?.inpatientOutpatient ?: "",
            dateSeen = dateSeen,
            patientName = patientName,
            dateOfBirth = dateOfBirth,
            age = age,
            gender = gender,
            address = address,
            districtAnnex2 = district,
            urbanRural = residence,
            phoneNumber = phoneNumber,
            occupation = occupation,
            dateOfOnset = receivedAnnex2FReport?.dateOfOnset ?: "",
            travelHistory = receivedAnnex2FReport?.travelHistory ?: "",
            destination = receivedAnnex2FReport?.destination ?: "",
            vaccineDoses = receivedAnnex2FReport?.vaccineDoses ?: "",
            dateLastVaccine = receivedAnnex2FReport?.dateLastVaccine ?: "",
            dateSpecimen = receivedAnnex2FReport?.dateSpecimen ?: "",
            dateLab = receivedAnnex2FReport?.dateLab ?: "",
            labResults = receivedAnnex2FReport?.labResults ?: "",
            outcome = receivedAnnex2FReport?.outcome ?: "",
            classification = receivedAnnex2FReport?.classification ?: "",
            dateFacilityNotified = receivedAnnex2FReport?.dateFacilityNotified ?: "",
            dateSentDistrict = receivedAnnex2FReport?.dateSentDistrict ?: "",
            reporterName = receivedAnnex2FReport?.reporterName ?: ""
        )

        val intent = Intent(this, Annex2F_Immediate_3_Activity::class.java).apply {
            putExtra("Annex2FReport", annex2FReports)
        }
        startActivity(intent)
    }
}