package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.idsr_project.R
import com.idsr_project.databinding.ActivityFormTypeBinding

class Form_Type_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityFormTypeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFormTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackFormType.setOnClickListener { finish() }


        binding.Annex2FImmediate.setOnClickListener {
            val intent = Intent(this, Annex2F_Immediate_1_Activity::class.java)
            startActivity(intent)
        }
        binding.Annex2GCaseBaseReportLab.setOnClickListener {
            val intent = Intent(this, Laboratory_Form_1_Annex2G_Activity::class.java)
            startActivity(intent)
        }
        binding.LabToCompleteForm.setOnClickListener {
            val intent = Intent(this, Laboratory_Form_2_Annex2G_Activity::class.java)
            startActivity(intent)
        }

    }
}