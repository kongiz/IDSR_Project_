package com.idsr_project.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.idsr_project.Adapter.BannerAdapter
import com.idsr_project.Model.CountResponse
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityMainBinding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bannerPager: ViewPager2
    private lateinit var bannerAdapter: BannerAdapter
    private val sliderHandler = Handler(Looper.getMainLooper())

    private val sliderRunnable = Runnable {
        if (::bannerPager.isInitialized) {
            bannerPager.setCurrentItem(bannerPager.currentItem + 1, true)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setupClicks()
        setupBottomNav()
        displayUserName()


        binding.root.post {
            setupBanner()
            setupAnimations()
            loadReportCount()
        }
    }

    private fun displayUserName() {
        val fullName = SessionManager.getFullName(this)

        binding.tvWelcomeUser.apply {
            text = if (fullName.isNotBlank()) "$fullName 👋" else "Welcome 👋"
            alpha = 0f
            translationY = 20f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(150)
                .start()
        }
    }

    private fun setupBanner() {
        bannerPager = binding.bannerPager

        val images = listOf(
            R.drawable.splash11,
            R.drawable.splash11,
            R.drawable.splash11
        )

        bannerAdapter = BannerAdapter(images)
        bannerPager.adapter = bannerAdapter

        val startPosition = Int.MAX_VALUE / 2
        bannerPager.setCurrentItem(startPosition, false)

        bannerPager.setPageTransformer { page, position ->
            val scale = 0.85f + (1 - abs(position)) * 0.15f
            page.scaleY = scale
            page.alpha = scale
        }

        // Start the auto-slider
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }


    private fun setupAnimations() {
        val scaleAnim: Animation =
            AnimationUtils.loadAnimation(this, R.anim.card_scale)

        binding.btnAlerts.startAnimation(scaleAnim)
        binding.btnSubmitReport.startAnimation(scaleAnim)
        binding.btnWeeklySurveillance.startAnimation(scaleAnim)
        binding.btnViewReports.startAnimation(scaleAnim)
    }

    private fun setupClicks() {

        binding.btnWeeklySurveillance.setOnClickListener {
            startActivity(Intent(this, SurveillanceActivity1::class.java))
        }

        binding.btnSubmitReport.setOnClickListener {
            startActivity(Intent(this, Form_Type_Activity::class.java))
        }

        binding.btnViewReports.setOnClickListener {
            startActivity(Intent(this, History_Activity::class.java))
        }

        binding.btnThemeToggle.setOnClickListener {
            val currentMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK

            if (currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private fun setupBottomNav() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_analytics -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile_Activity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadReportCount() {

        val userId = SessionManager.getUserId(this)
        val role = SessionManager.getUserRole(this)
        val regionId = SessionManager.getUserRegion(this)?.toIntOrNull()
        val districtId = SessionManager.getUserDistrict(this)?.toIntOrNull()

        val api = ApiClient.getClient(this)

        val call: Call<CountResponse> = when (role) {
            "Admin" -> api.getReportCount("admin", userId)
            "Regional Officer" -> api.getReportCount("regional_officer", userId, regionId)
            "District Officer" -> api.getReportCount("district_officer", userId, districtId)
            else -> api.getReportCount("user", userId)
        }

        call.enqueue(object : Callback<CountResponse> {

            override fun onResponse(
                call: Call<CountResponse>,
                response: Response<CountResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val total = response.body()?.total_reports ?: 0
                    animateCounter(0, total)
                } else {
                    animateCounter(0, 0)
                }
            }

            override fun onFailure(call: Call<CountResponse>, t: Throwable) {
                animateCounter(0, 0)
            }
        })
    }

    private fun animateCounter(start: Int, end: Int) {
        val animator = ValueAnimator.ofInt(start, end)
        animator.duration = 1000
        animator.addUpdateListener {
            binding.tvCasesNumber.text = it.animatedValue.toString()
        }
        animator.start()
    }

    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}