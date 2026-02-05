package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idsr_project.Adapter.DiseaseAdapter
import com.idsr_project.Model.Diseases
import com.idsr_project.Model.surveillanceData
import com.idsr_project.databinding.ActivitySurveillance2Binding
import java.util.ArrayList

class SurveillanceActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySurveillance2Binding
    private lateinit var adapter: DiseaseAdapter
    private lateinit var surveillanceReceivedData: surveillanceData


    private val initialDiseases = mutableListOf(
        Diseases("Acute Flaccid Paralysis"),
        Diseases("Animal Bite"),
//        Disease("Anthrax"),
//        Disease("Cholera"),
//        Disease("COVID-19"),
//        Disease("Diarrhoea with blood"),
//        Disease("Dog Bite"),
//        Disease("Human Rabies"),
//        Disease("Leprosy"),
//        Disease("Lymphatic Filariasis"),
//        Disease("Maternal Death"),
//        Disease("Measles"),
//        Disease("Meningitis"),
//        Disease("Neonatal Tetanus"),
//        Disease("Schistosomiasis"),
//        Disease("Shigellosis"),
//        Disease("Snake Bite"),
//        Disease("Suspected VHF (incl. EVD)"),
//        Disease("Trachoma"),
//        Disease("Unexplained Cluster of Health Events"),
//        Disease("Unexplained Cluster of Deaths"),
//        Disease("Yellow Fever")
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySurveillance2Binding.inflate(layoutInflater)
        setContentView(binding.root)


        val receivedData = intent.getParcelableExtra<surveillanceData>("SurveillanceData")
        if (receivedData == null) {
            Toast.makeText(this, "No initial data received. Exiting.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        surveillanceReceivedData = receivedData

        Log.d("Surveillance2", "Received from Activity1: $surveillanceReceivedData")


        binding.diseasesRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = DiseaseAdapter(initialDiseases)
        binding.diseasesRecyclerView.adapter = adapter


        binding.btnBackSur2.setOnClickListener {
            finish()
        }

        binding.saveProceedBtn.setOnClickListener {
            val updatedDiseases = adapter.getDiseases()
            Log.i("UpdatedDiseases", "Count of recorded diseases: ${updatedDiseases.size}")
            val intent = Intent(this, SurveillanceActivity3::class.java).apply {
                putExtra("SurveillanceData", surveillanceReceivedData)
                putParcelableArrayListExtra("UpdatedDiseases", ArrayList(updatedDiseases))

            }
            startActivity(intent)
        }
    }
}