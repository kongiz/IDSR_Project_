package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idsr_project.Adapter.DiseaseAdapter
import com.idsr_project.Model.Diseases
import com.idsr_project.Model.surveillanceData
import com.idsr_project.databinding.ActivitySurveillance2Binding

class SurveillanceActivity2 : BaseActivity() {

    private lateinit var binding: ActivitySurveillance2Binding
    private lateinit var adapter: DiseaseAdapter
    private lateinit var surveillanceReceivedData: surveillanceData


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


        val receivedData = intent.getParcelableExtra<surveillanceData>("SurveillanceData")
        if (receivedData == null) {
            Toast.makeText(this, "Session lost. Please restart form.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        surveillanceReceivedData = receivedData


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
            val finalDiseases = adapter.getDiseases()


            if (finalDiseases.any { it.u5MaleAlive < 0 }) {
                Toast.makeText(this, "Please check for negative values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, SurveillanceActivity3::class.java).apply {
                putExtra("SurveillanceData", surveillanceReceivedData)
                putParcelableArrayListExtra("UpdatedDiseases", ArrayList(finalDiseases))
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