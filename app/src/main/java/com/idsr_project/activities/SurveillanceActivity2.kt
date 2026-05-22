package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idsr_project.Adapter.DiseaseAdapter
import com.idsr_project.Model.Diseases
import com.idsr_project.Model.FormData
import com.idsr_project.Model.surveillanceData
import com.idsr_project.databinding.ActivitySurveillance2Binding
import com.idsr_project.utils.EditModeExtras
import com.idsr_project.utils.applyWindowInsets
import com.idsr_project.utils.dpToPx

class SurveillanceActivity2 : BaseActivity() {

    private lateinit var binding: ActivitySurveillance2Binding
    private lateinit var adapter: DiseaseAdapter
    private lateinit var surveillanceReceivedData: surveillanceData

    private var isEditMode   = false
    private var editReportId = -1
    private var editFormData: FormData? = null


    private val initialDiseases = mutableListOf(
        Diseases("Acute Flaccid Paralysis"),
        Diseases("Animal Bite"),
        Diseases("Measles"),
        Diseases("Yellow Fever"),
        Diseases("Cholera"),
        Diseases("Meningitis")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySurveillance2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets(
            topView    = binding.appBarLayout,
            bottomView = binding.saveProceedBtn
        )


        val receivedData = intent.getParcelableExtra<surveillanceData>("SurveillanceData")
        if (receivedData == null) {
            Toast.makeText(this, "Session lost. Please restart form.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        surveillanceReceivedData = receivedData

        isEditMode   = intent.getBooleanExtra(EditModeExtras.EXTRA_EDIT_MODE, false)
        editReportId = intent.getIntExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, -1)
        editFormData = intent.getParcelableExtra(EditModeExtras.EXTRA_EDIT_DATA)

        if (isEditMode && editFormData?.diseases?.isNotEmpty() == true) {
            initialDiseases.clear()
            editFormData!!.diseases!!.forEach { disease ->
                initialDiseases.add(Diseases(
                    name           = disease.name ?: "",
                    u5MaleAlive    = disease.under5_male ?: 0,
                    u5FemaleAlive  = disease.under5_female ?: 0,
                    a5MaleAlive    = disease.above5_male ?: 0,
                    a5FemaleAlive  = disease.above5_female ?: 0,
                ))
            }
        }


        adapter = DiseaseAdapter(initialDiseases)
        binding.diseasesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SurveillanceActivity2)
            adapter = this@SurveillanceActivity2.adapter
            setHasFixedSize(true)
        }


        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitWarning()
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        binding.btnBackSur2.setOnClickListener { showExitWarning() }


        binding.saveProceedBtn.setOnClickListener {
            val errors = adapter.getValidationErrors()
            if (errors.isNotEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Fix errors before proceeding")
                    .setMessage(errors.joinToString("\n\n"))
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            val intent = Intent(this, SurveillanceActivity3::class.java).apply {
                putExtra("SurveillanceData", surveillanceReceivedData)
                putParcelableArrayListExtra("UpdatedDiseases", ArrayList(adapter.getDiseases()))
                putExtra(EditModeExtras.EXTRA_EDIT_MODE, isEditMode)
                putExtra(EditModeExtras.EXTRA_EDIT_REPORT_ID, editReportId)
                putExtra(EditModeExtras.EXTRA_EDIT_DATA, editFormData)
            }
            startActivity(intent)
        }
    }

    private fun showExitWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Report?")
            .setMessage("All progress on this disease list will be lost.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }
}