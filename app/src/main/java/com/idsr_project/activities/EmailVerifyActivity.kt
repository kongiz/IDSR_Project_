package com.idsr_project.activities

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.R
import com.idsr_project.databinding.ActivityEmailVerifyBinding

class EmailVerifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerifyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmailVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val btnBack : ImageButton = findViewById(R.id.btnBackVerify)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }
}

