package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.databinding.ActivityFormTypeBinding
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets

class Form_Type_Activity : BaseActivity() {
    private lateinit var binding: ActivityFormTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFormTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(topView = binding.appBarLayout)

        binding.btnBackFormType.setOnClickListener { finish() }
        setupRoleBasedForms()
        setupClickListeners()

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "Form_Type_Activity")
    }

    private fun setupRoleBasedForms() {
        val role = SessionManager.getUserRole(this) ?: "Health Officer"

        when (role) {
            "Lab Technician" -> {
                binding.Annex2FImmediate.visibility         = View.GONE
                binding.Annex2GCaseBaseReportLab.visibility = View.VISIBLE
                binding.LabToCompleteForm.visibility        = View.VISIBLE
                binding.titleTxt.text = "Select a lab form to submit"
            }
            "Clinician" -> {
                binding.Annex2FImmediate.visibility         = View.VISIBLE
                binding.Annex2GCaseBaseReportLab.visibility = View.VISIBLE
                binding.LabToCompleteForm.visibility        = View.GONE
                binding.titleTxt.text = "Select the type of form to submit"
            }
            "Community Health Worker" -> {
                binding.Annex2FImmediate.visibility         = View.VISIBLE
                binding.Annex2GCaseBaseReportLab.visibility = View.VISIBLE
                binding.LabToCompleteForm.visibility        = View.GONE
                binding.titleTxt.text = "Select the type of form to submit"
            }
            "Health Officer" -> {
                binding.Annex2FImmediate.visibility         = View.VISIBLE
                binding.Annex2GCaseBaseReportLab.visibility = View.VISIBLE
                binding.LabToCompleteForm.visibility        = View.VISIBLE
                binding.titleTxt.text = "Select the type of form to submit"
            }
            else -> {
                binding.Annex2FImmediate.visibility         = View.GONE
                binding.Annex2GCaseBaseReportLab.visibility = View.GONE
                binding.LabToCompleteForm.visibility        = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.Annex2FImmediate.setOnClickListener {
            startActivity(Intent(this, Annex2F_Immediate_1_Activity::class.java))
        }
        binding.Annex2GCaseBaseReportLab.setOnClickListener {
            startActivity(Intent(this, Laboratory_Form_1_Annex2G_Activity::class.java))
        }
        binding.LabToCompleteForm.setOnClickListener {
            startActivity(Intent(this, Laboratory_Form_2_Annex2G_Activity::class.java))
        }
    }
}