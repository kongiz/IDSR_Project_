package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.idsr_project.R
import com.idsr_project.databinding.ActivitySplashBinding
import com.idsr_project.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : BaseActivity() {
    override val excludeFromTimeout: Boolean = true

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimations()
        handleNavigation()
    }

    private fun startAnimations() {

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                binding.splashImg.startAnimation(slideUp)
            }
        })

        binding.splashImg.startAnimation(fadeIn)
    }

    private fun handleNavigation() {

        lifecycleScope.launch {


            delay(2200)


            val hasToken = withContext(Dispatchers.IO) {
                !SessionManager.getAccessToken(this@SplashActivity).isNullOrEmpty()
            }

            val nextActivity = if (hasToken) {
                MainActivity::class.java
            } else {
                Login_Activity::class.java
            }

            startActivity(Intent(this@SplashActivity, nextActivity))
            finish()
        }
    }
}
