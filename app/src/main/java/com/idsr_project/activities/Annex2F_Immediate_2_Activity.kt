package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.Annex2FData
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.databinding.ActivityAnnex2Fimmediate2Binding
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_2_Activity : BaseActivity() {
    private lateinit var binding: ActivityAnnex2Fimmediate2Binding
    private val calendar = Calendar.getInstance()

    private var isEditMode   = false
    private var editReportId = -1
    private var editData: Annex2FData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate2Binding.inflate(layoutInflater)
        setContentView(binding.root)


        setupGenderSpinner()


        setupResidenceSpinner()

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Annex2F_Immediate_2_Activity")


        binding.btnBackAnnex1.setOnClickListener {
            finish()
        }


        binding.etDateSeen.setOnClickListener {
            showDatePicker(isDOB = false) { dateString ->
                binding.etDateSeen.setText(dateString)

                val dob = binding.etDateOfBirth.text.toString()
                if (dob.isNotEmpty()) {
                    val age = calculateAgeFromDOB(dob)
                    binding.tvAge.setText("$age years")
                }
            }
        }


        binding.etDateOfBirth.setOnClickListener {
            showDatePicker(isDOB = true) { dateString ->
                binding.etDateOfBirth.setText(dateString)
                val age = calculateAgeFromDOB(dateString)
                binding.tvAge.setText("$age years")
                binding.titleDateOfBirth.error = null
            }
        }

        binding.btnNextAnnex2.setOnClickListener {
            if (validateImmediate2Form()) {
                passDataToNextScreen()
            }
        }

        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editReportId = intent.getIntExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, -1)
        editData     = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)

        if (isEditMode && editData != null) {
            prefillAnnex2F2(editData!!)
            binding.btnNextAnnex2.text = "Next (Editing)"
        }
    }
    private fun prefillAnnex2F2(data: Annex2FData) {
        binding.etPatientName.setText(data.patientName ?: "")
        binding.etDateSeen.setText(data.dateSeen ?: "")
        binding.etDateOfBirth.setText(data.dateOfBirth ?: "")
        binding.tvAge.setText(data.age ?: "")
        binding.spinnerGender.setText(data.gender ?: "", false)
        binding.etAddress.setText(data.address ?: "")
        binding.etDistrictAnnex2.setText(data.district_name ?: "")
        binding.spinnerResidence.setText(data.urbanRural ?: "", false)
        binding.etPhoneNumber.setText(data.phoneNumber ?: "")
        binding.etOccupation.setText(data.occupation ?: "")
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

    private fun showDatePicker(isDOB: Boolean, onDateSelected: (String) -> Unit) {
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

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

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

        val dateSeenStr = binding.etDateSeen.text.toString().trim()
        val dobStr = binding.etDateOfBirth.text.toString().trim()
        val patientName = binding.etPatientName.text.toString().trim()
        val gender = binding.spinnerGender.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val districtRes = binding.etDistrictAnnex2.text.toString().trim()
        val residence = binding.spinnerResidence.text.toString().trim()
        val phone = binding.etPhoneNumber.text.toString().trim()
        val occupation = binding.etOccupation.text.toString().trim()

        if (dateSeenStr.isEmpty()) {
            binding.titleDateSeen.error = "Date seen is required"
            isValid = false
        } else {
            binding.titleDateSeen.error = null
        }

        if (dobStr.isEmpty()) {
            binding.titleDateOfBirth.error = "Date of Birth is required"
            isValid = false
        } else {
            binding.titleDateOfBirth.error = null
        }

        if (dobStr.isNotEmpty() && dateSeenStr.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dob = sdf.parse(dobStr)
            val dateSeen = sdf.parse(dateSeenStr)

            if (dateSeen != null && dob != null && dateSeen.before(dob)) {
                binding.titleDateSeen.error = "Date seen cannot be before Date of Birth"
                isValid = false
            }
        }

        if (patientName.isEmpty()) {
            binding.titlePatientName.error = "Patient Name is required"
            isValid = false
        } else binding.titlePatientName.error = null

        if (gender.isEmpty()) {
            binding.titleGender.error = "Please select Gender"
            isValid = false
        } else binding.titleGender.error = null

        if (address.isEmpty()) {
            binding.titleAddress.error = "Patient Address is required"
            isValid = false
        } else binding.titleAddress.error = null

        if (districtRes.isEmpty()) {
            binding.titleDistrictResidence.error = "District of residence is required"
            isValid = false
        } else binding.titleDistrictResidence.error = null

        if (residence.isEmpty()) {
            binding.titleResidence.error = "Please select Residence"
            isValid = false
        } else binding.titleResidence.error = null

        if (phone.isEmpty()) {
            binding.titlePhoneNumber.error = "Patient Phone is required"
            isValid = false
        } else if (!phone.matches(Regex("^[0-9]{7,15}$"))) {
            binding.titlePhoneNumber.error = "Enter a valid phone number (7-15 digits)"
            isValid = false
        } else {
            binding.titlePhoneNumber.error = null
        }

        if (occupation.isEmpty()) {
            binding.titleOccupation.error = "Patient's Occupation is required"
            isValid = false
        } else binding.titleOccupation.error = null

        return isValid
    }
    private fun passDataToNextScreen() {
        val dateSeen = binding.etDateSeen.text.toString().trim()
        val patientName = binding.etPatientName.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val gender = binding.spinnerGender.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val district = binding.etDistrictAnnex2.text.toString().trim()
        val residence = binding.spinnerResidence.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val occupation = binding.etOccupation.text.toString().trim()

        val ageRaw = binding.tvAge.text.toString().trim()
        val ageInt = ageRaw.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0


        val receivedAnnex2FReport = intent.getParcelableExtra<immediateReportForm>("Annex2FReport")

        val annex2FReports = immediateReportForm(
            recordId = receivedAnnex2FReport?.recordId ?: "",
            country = receivedAnnex2FReport?.country ?: "",
            province = receivedAnnex2FReport?.province ?: "",
            district = receivedAnnex2FReport?.district ?: 0,
            site = receivedAnnex2FReport?.site ?: "",
            disease = receivedAnnex2FReport?.disease ?: "",
            inpatientOutpatient = receivedAnnex2FReport?.inpatientOutpatient ?: "",
            caseGeo = receivedAnnex2FReport?.caseGeo ?: "",
            dateSeen = dateSeen,
            patientName = patientName,
            dateOfBirth = dateOfBirth,
            age = ageInt,
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
            putExtra(EditModeExtras.EXTRA_EDIT_MODE, isEditMode)
            putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, editReportId)
            putExtra(EditModeExtras.EXTRA_EDIT_DATA, editData)
        }
        startActivity(intent)
    }
}