package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.R
import com.idsr_project.databinding.ActivitySuccessBinding
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

class Success_Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnBackSuccess.setOnClickListener {
            finish()
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
                size = listOf(Size.SMALL, Size.MEDIUM),
                timeToLive = 2000L,
                fadeOutEnabled = true,
                position = Position.Relative(0.5, 0.3),
                emitter = Emitter(
                    duration = 500,
                    TimeUnit.MILLISECONDS
                ).perSecond(150)
            )
        )


        binding.btnViewSubmission.setOnClickListener {
            startActivity(
                Intent(this, History_Activity::class.java)
            )
            finish()
        }

        binding.btnNewForm.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
            )
            finish()
        }


        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }, 3000)
    }
}
