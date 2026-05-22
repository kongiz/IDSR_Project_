package com.idsr_project.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.Annex2FData
import com.idsr_project.Model.immediateReportForm
import com.idsr_project.databinding.ActivityAnnex2Fimmediate2Binding
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.applyWindowInsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Annex2F_Immediate_2_Activity : BaseActivity() {

    private lateinit var binding: ActivityAnnex2Fimmediate2Binding
    private val calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var isEditMode   = false
    private var editReportId = -1
    private var editData: Annex2FData? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAnnex2Fimmediate2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(
            topView    = binding.appBarLayout,
            bottomView = binding.btnNextAnnex2
        )


        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editReportId = intent.getIntExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, -1)
        editData     = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)

        setupGenderSpinner()
        setupResidenceSpinner()
        setupDatePickers()
        setupFieldListeners()

        if (isEditMode && editData != null) {
            prefillAnnex2F2(editData!!)
            binding.btnNextAnnex2.text = "Next (Editing)"
        }

        binding.btnBackAnnex1.setOnClickListener { finish() }
        binding.btnNextAnnex2.setOnClickListener {
            if (validateImmediate2Form()) passDataToNextScreen()
        }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Annex2F_Immediate_2_Activity")
    }


    private fun setupGenderSpinner() {
        val options = arrayOf("Male", "Female", "Other")
        binding.spinnerGender.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.spinnerGender.setOnItemClickListener { _, _, _, _ ->
            binding.titleGender.error = null
        }
    }

    private fun setupResidenceSpinner() {
        val options = arrayOf("Urban", "Rural")
        binding.spinnerResidence.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        )
        binding.spinnerResidence.setOnItemClickListener { _, _, _, _ ->
            binding.titleResidence.error = null
        }
    }


    private fun setupDatePickers() {
        binding.etDateSeen.setOnClickListener {
            showDatePicker(isDOB = false) { dateString ->
                binding.etDateSeen.setText(dateString)
                binding.titleDateSeen.error = null
                // Recalculate age if DOB already set
                val dob = binding.etDateOfBirth.text.toString()
                if (dob.isNotEmpty()) {
                    binding.tvAge.setText("${calculateAgeFromDOB(dob)} years")
                }
            }
        }

        binding.etDateOfBirth.setOnClickListener {
            showDatePicker(isDOB = true) { dateString ->
                binding.etDateOfBirth.setText(dateString)
                binding.tvAge.setText("${calculateAgeFromDOB(dateString)} years")
                binding.titleDateOfBirth.error = null
            }
        }
    }

    private fun showDatePicker(isDOB: Boolean, onDateSelected: (String) -> Unit) {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val cal = Calendar.getInstance().apply { set(year, month, day) }
                onDateSelected(sdf.format(cal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun calculateAgeFromDOB(dobString: String): Int {
        val dob = sdf.parse(dobString) ?: return 0
        val dobCal = Calendar.getInstance().apply { time = dob }
        val today  = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) age--
        return age
    }

    private fun setupFieldListeners() {
        binding.etPatientName.addTextChangedListener  { binding.titlePatientName.error       = null }
        binding.etAddress.addTextChangedListener      { binding.titleAddress.error            = null }
        binding.etDistrictAnnex2.addTextChangedListener { binding.titleDistrictResidence.error = null }
        binding.etPhoneNumber.addTextChangedListener  { binding.titlePhoneNumber.error        = null }
        binding.etOccupation.addTextChangedListener   { binding.titleOccupation.error         = null }
    }


    private fun validateImmediate2Form(): Boolean {
        var isValid = true

        val dateSeenStr = binding.etDateSeen.text.toString().trim()
        val dobStr      = binding.etDateOfBirth.text.toString().trim()

        if (dateSeenStr.isEmpty()) {
            binding.titleDateSeen.error = "Required"; isValid = false
        } else binding.titleDateSeen.error = null

        if (dobStr.isEmpty()) {
            binding.titleDateOfBirth.error = "Required"; isValid = false
        } else binding.titleDateOfBirth.error = null

        if (dobStr.isNotEmpty() && dateSeenStr.isNotEmpty()) {
            val dob      = sdf.parse(dobStr)
            val dateSeen = sdf.parse(dateSeenStr)
            if (dob != null && dateSeen != null && dateSeen.before(dob)) {
                binding.titleDateSeen.error = "Date seen cannot be before Date of Birth"
                isValid = false
            }
        }

        if (binding.etPatientName.text.isNullOrEmpty()) {
            binding.titlePatientName.error = "Required"; isValid = false
        }
        if (binding.spinnerGender.text.isNullOrEmpty()) {
            binding.titleGender.error = "Required"; isValid = false
        }
        if (binding.etAddress.text.isNullOrEmpty()) {
            binding.titleAddress.error = "Required"; isValid = false
        }
        if (binding.etDistrictAnnex2.text.isNullOrEmpty()) {
            binding.titleDistrictResidence.error = "Required"; isValid = false
        }
        if (binding.spinnerResidence.text.isNullOrEmpty()) {
            binding.titleResidence.error = "Required"; isValid = false
        }

        val phone = binding.etPhoneNumber.text.toString().trim()
        when {
            phone.isEmpty() -> {
                binding.titlePhoneNumber.error = "Required"; isValid = false
            }
            !phone.matches(Regex("^[0-9]{7}$")) -> {
                binding.titlePhoneNumber.error = "Enter a valid phone number must be 7 digits"
                isValid = false
            }
            else -> binding.titlePhoneNumber.error = null
        }

        if (binding.etOccupation.text.isNullOrEmpty()) {
            binding.titleOccupation.error = "Required"; isValid = false
        }

        return isValid
    }


    private fun prefillAnnex2F2(data: Annex2FData) {
        binding.etPatientName.setText(data.patientName    ?: "")
        binding.etDateSeen.setText(data.dateSeen          ?: "")
        binding.etDateOfBirth.setText(data.dateOfBirth    ?: "")
        binding.tvAge.setText(data.age                    ?: "")
        binding.spinnerGender.setText(data.gender         ?: "", false)
        binding.etAddress.setText(data.address            ?: "")
        binding.etDistrictAnnex2.setText(data.district_name ?: "")
        binding.spinnerResidence.setText(data.urbanRural  ?: "", false)
        binding.etPhoneNumber.setText(data.phoneNumber    ?: "")
        binding.etOccupation.setText(data.occupation      ?: "")
    }


    private fun passDataToNextScreen() {
        val ageRaw = binding.tvAge.text.toString().trim()
        val ageInt = ageRaw.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

        val received = intent.getParcelableExtra<immediateReportForm>("Annex2FReport")

        val annex2FReports = immediateReportForm(
            recordId            = received?.recordId            ?: "",
            country             = received?.country             ?: "",
            province            = received?.province            ?: "",
            district            = received?.district            ?: 0,
            site                = received?.site                ?: "",
            disease             = received?.disease             ?: "",
            inpatientOutpatient = received?.inpatientOutpatient ?: "",
            caseGeo             = received?.caseGeo             ?: "",
            dateSeen            = binding.etDateSeen.text.toString().trim(),
            patientName         = binding.etPatientName.text.toString().trim(),
            dateOfBirth         = binding.etDateOfBirth.text.toString().trim(),
            age                 = ageInt,
            gender              = binding.spinnerGender.text.toString().trim(),
            address             = binding.etAddress.text.toString().trim(),
            districtAnnex2      = binding.etDistrictAnnex2.text.toString().trim(),
            urbanRural          = binding.spinnerResidence.text.toString().trim(),
            phoneNumber         = binding.etPhoneNumber.text.toString().trim(),
            occupation          = binding.etOccupation.text.toString().trim(),
            dateOfOnset         = received?.dateOfOnset         ?: "",
            travelHistory       = received?.travelHistory       ?: "",
            destination         = received?.destination         ?: "",
            vaccineDoses        = received?.vaccineDoses        ?: "",
            dateLastVaccine     = received?.dateLastVaccine     ?: "",
            dateSpecimen        = received?.dateSpecimen        ?: "",
            dateLab             = received?.dateLab             ?: "",
            labResults          = received?.labResults          ?: "",
            outcome             = received?.outcome             ?: "",
            classification      = received?.classification      ?: "",
            dateFacilityNotified = received?.dateFacilityNotified ?: "",
            dateSentDistrict    = received?.dateSentDistrict    ?: "",
            reporterName        = received?.reporterName        ?: ""
        )

        startActivity(
            Intent(this, Annex2F_Immediate_3_Activity::class.java).apply {
                putExtra("Annex2FReport", annex2FReports)
                putExtra(EditModeExtras.EXTRA_EDIT_MODE,      isEditMode)
                putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, editReportId)
                putExtra(EditModeExtras.EXTRA_EDIT_DATA,      editData)
            }
        )
    }
}