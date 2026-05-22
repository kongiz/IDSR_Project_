package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.R
import com.idsr_project.databinding.ActivitySuccessBinding
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Success_Activity : BaseActivity() {

    private lateinit var binding: ActivitySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        binding.tvTimestamp.text = "Submitted: $currentTime"


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToMain()
            }
        })


        binding.btnBackSuccess.setOnClickListener {
            navigateToMain()
        }


        val successAnim = AnimationUtils.loadAnimation(
            this,
            R.anim.success_scale_fade
        )
        binding.imgCheck.startAnimation(successAnim)


        binding.confettiView.start(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                size = listOf(Size.SMALL, Size.MEDIUM, Size.LARGE),
                timeToLive = 3000L,
                fadeOutEnabled = true,
                position = Position.Relative(0.5, 0.3),
                emitter = Emitter(
                    duration = 1000,
                    TimeUnit.MILLISECONDS
                ).perSecond(150)
            )
        )


        binding.btnViewSubmission.setOnClickListener {
            val intent = Intent(this, History_Activity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }


        binding.btnNewForm.setOnClickListener {
            navigateToMain()
            finish()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}