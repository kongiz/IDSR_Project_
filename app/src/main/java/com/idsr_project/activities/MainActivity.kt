package com.idsr_project.activities

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.idsr_project.Adapter.BannerAdapter
import com.idsr_project.Model.AppUpdateState
import com.idsr_project.Model.CountResponse
import com.idsr_project.Model.NotificationsResponse
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.databinding.ActivityMainBinding
import com.idsr_project.sync.CleanupWorker
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.abs

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bannerAdapter: BannerAdapter
    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var inAppUpdateManager: InAppUpdateManager

    private val updatedLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            FirebaseCrashlytics.getInstance().log("Update flow result failed or cancelled")
        }
    }

    private val sliderRunnable = object : Runnable {
        override fun run() {
            if (::bannerAdapter.isInitialized && binding.bannerPager.adapter != null) {
                val next = (binding.bannerPager.currentItem + 1) % 3
                binding.bannerPager.setCurrentItem(next, true)
                sliderHandler.postDelayed(this, 3000)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets(topView = binding.appBarLayout)
        applyBottomNavInsets()

        inAppUpdateManager = InAppUpdateManager(this)
        lifecycleScope.launch { inAppUpdateManager.checkForUpdate(updatedLauncher) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                inAppUpdateManager.updateState.collect(::handleUpdateState)
            }
        }

        setupClicks()
        setupBottomNav()
        setupRoleBasedUI()
        displayUserName()
        setupCrashlyticsContext()
        requestNotificationPermission()
        CleanupWorker.schedule(this)

        binding.root.post {
            setupBanner()
            setupAnimations()
            loadReportCount()
        }
    }

    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(sliderRunnable, 3000)
        loadReportCount()
        loadSyncStatus()
        loadUnreadCount()
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onDestroy() {
        sliderHandler.removeCallbacks(sliderRunnable)
        if (::inAppUpdateManager.isInitialized) inAppUpdateManager.unregister()
        super.onDestroy()
    }

    private fun applyBottomNavInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom
            )
            binding.scrollView.setPadding(
                binding.scrollView.paddingLeft,
                binding.scrollView.paddingTop,
                binding.scrollView.paddingRight,
                resources.getDimensionPixelSize(R.dimen.card_min_height) + systemBars.bottom
            )
            insets
        }
    }

    private fun setupRoleBasedUI() {
        val role = SessionManager.getUserRole(this) ?: "Health Officer"

        binding.cardRowA.visibility              = View.VISIBLE
        binding.btnSubmitReport.visibility       = View.VISIBLE
        binding.btnWeeklySurveillance.visibility = View.VISIBLE
        binding.btnManageUsers.visibility        = View.GONE

        when (role) {
            "District Officer", "Regional Officer" -> {
                binding.cardRowA.visibility = View.GONE
                setFullWidth(binding.btnViewReports)
            }
            "Admin" -> {
                binding.cardRowA.visibility       = View.GONE
                binding.btnManageUsers.visibility = View.VISIBLE
            }
            "Lab Technician" -> {
                binding.btnWeeklySurveillance.visibility = View.GONE
                setFullWidth(binding.btnSubmitReport)
                setFullWidth(binding.btnViewReports)
            }
            else -> {
                setFullWidth(binding.btnViewReports)
            }
        }
    }

    private fun setFullWidth(view: View) {
        view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
            width       = 0
            weight      = 1f
            marginStart = 0
            marginEnd   = 0
        }
    }

    private fun setupBanner() {
        val images = listOf(R.drawable.splash11, R.drawable.splash11, R.drawable.splash11)
        bannerAdapter = BannerAdapter(images)
        binding.bannerPager.adapter = bannerAdapter
        binding.bannerPager.setCurrentItem(0, false)
        binding.bannerPager.setPageTransformer { page, position ->
            val scale = 0.85f + (1 - abs(position)) * 0.15f
            page.scaleY = scale
            page.alpha  = scale
        }
        repeat(3) { binding.bannerIndicator.addTab(binding.bannerIndicator.newTab()) }
        binding.bannerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bannerIndicator.selectTab(binding.bannerIndicator.getTabAt(position))
            }
        })
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    private fun setupAnimations() {
        val scaleAnim: Animation = AnimationUtils.loadAnimation(this, R.anim.card_scale)
        val animTargets = listOf(
            binding.btnAlerts      to 0L,
            binding.cardRowA       to 100L,
            binding.cardRowB       to 200L,
            binding.cardSyncStatus to 300L
        )
        animTargets.forEach { (view, delay) ->
            if (view.visibility == View.VISIBLE) {
                view.postDelayed({ view.startAnimation(scaleAnim) }, delay)
            }
        }
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
        binding.btnMap.setOnClickListener {
            startActivity(Intent(this, DiseaseMapActivity::class.java))
        }
        binding.cardSyncStatus.setOnClickListener {
            startActivity(Intent(this, SyncStatusActivity::class.java))
        }
        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, Notifications_Activity::class.java))
        }
        binding.btnManageUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        val role = SessionManager.getUserRole(this) ?: "Health Officer"
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.menu
            .findItem(R.id.nav_analytics)
            ?.isVisible = role in listOf("Admin", "Regional Officer", "District Officer")

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home      -> true
                R.id.nav_analytics -> navigate(DashboardActivity::class.java)
                R.id.nav_profile   -> navigate(Profile_Activity::class.java)
                else               -> false
            }
        }
    }

    private fun navigate(target: Class<*>): Boolean {
        startActivity(Intent(this, target))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun displayUserName() {
        val fullName = SessionManager.getUserName(this)?.toString() ?: ""
        val role     = SessionManager.getUserRole(this) ?: "Health Officer"
        val greeting = getGreeting()


        binding.tvGreetingLabel.text = greeting


        binding.tvWelcomeUser.apply {
            text = if (fullName.isNotBlank()) "$fullName 👋" else "👋"
            alpha = 0f
            translationY = 20f
            animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(150).start()
        }


        binding.tvUserRole.visibility = View.VISIBLE
        binding.tvUserRole.text = role
    }

    private fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else      -> "Good Night"
        }
    }

    private fun loadSyncStatus() {
        lifecycleScope.launch {
            val pendingCount = AppDatabase.getInstance(this@MainActivity)
                .pendingReportDao()
                .getPendingCount()

            when {
                pendingCount == 0 -> {
                    binding.tvSyncStatusLabel.text = "All reports synced"
                    binding.tvSyncStatusLabel.setTextColor(
                        ContextCompat.getColor(this@MainActivity, R.color.idsr_primary)
                    )
                    binding.tvSyncBadge.visibility = View.GONE
                }
                else -> {
                    val label = if (pendingCount == 1) "1 report pending sync"
                    else "$pendingCount reports pending sync"
                    binding.tvSyncStatusLabel.text = label
                    binding.tvSyncStatusLabel.setTextColor(
                        ContextCompat.getColor(this@MainActivity, R.color.idsr_secondary)
                    )
                    binding.tvSyncBadge.text       = "$pendingCount"
                    binding.tvSyncBadge.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadReportCount() {
        val trace = FirebasePerformance.getInstance().newTrace("load_report_count")
        trace.start()
        FirebaseCrashlytics.getInstance().log("Loading report count")

        val userId     = SessionManager.getUserId(this)
        val role       = SessionManager.getUserRole(this)
        val regionId   = SessionManager.getUserRegion(this)?.toIntOrNull()
        val districtId = SessionManager.getUserDistrict(this)?.toIntOrNull()
        val api        = ApiClient.getClient(this)

        binding.tvCasesLabel.text = when (role) {
            "Admin"            -> "National Reports"
            "Regional Officer" -> "Regional Reports"
            "District Officer" -> "District Reports"
            else               -> "Reported Forms"
        }

        val call: Call<CountResponse> = when (role) {
            "Admin"            -> api.getReportCount("admin",            userId)
            "Regional Officer" -> api.getReportCount("regional_officer", userId, regionId)
            "District Officer" -> api.getReportCount("district_officer", userId, districtId)
            else               -> api.getReportCount("user",             userId)
        }

        call.enqueue(object : Callback<CountResponse> {
            override fun onResponse(call: Call<CountResponse>, response: Response<CountResponse>) {
                trace.stop()
                val total = if (response.isSuccessful && response.body()?.success == true)
                    response.body()?.total_reports ?: 0 else 0
                animateCounter(0, total)
            }
            override fun onFailure(call: Call<CountResponse>, t: Throwable) {
                trace.stop()
                FirebaseCrashlytics.getInstance().recordException(t)
                animateCounter(0, 0)
            }
        })
    }

    private fun animateCounter(start: Int, end: Int) {
        ValueAnimator.ofInt(start, end).apply {
            duration = 1200
            addUpdateListener { binding.tvCasesNumber.text = it.animatedValue.toString() }
            start()
        }
    }

    private fun loadUnreadCount() {
        ApiClient.getClient(this)
            .getNotifications()
            .enqueue(object : Callback<NotificationsResponse> {
                override fun onResponse(
                    call: Call<NotificationsResponse>,
                    response: Response<NotificationsResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val unread = response.body()!!.unread_count
                        binding.tvNotificationBadge.visibility =
                            if (unread > 0) View.VISIBLE else View.GONE
                        binding.tvNotificationBadge.text =
                            if (unread > 99) "99+" else unread.toString()
                    }
                }
                override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                    FirebaseCrashlytics.getInstance().recordException(t)
                }
            })
    }

    private fun handleUpdateState(state: AppUpdateState) {
        when (state) {
            is AppUpdateState.ReadyToInstall -> showUpdateSnackbar()
            is AppUpdateState.Error          -> Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            else                             -> Unit
        }
    }

    private fun showUpdateSnackbar() {
        Snackbar.make(binding.root, "An update is ready to install.", Snackbar.LENGTH_INDEFINITE)
            .setAction("RESTART") { inAppUpdateManager.completeUpdate() }
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun setupCrashlyticsContext() {
        val userId     = SessionManager.getUserId(this) ?: return
        val role       = SessionManager.getUserRole(this)     ?: "Health Officer"
        val regionId   = SessionManager.getUserRegion(this)   ?: "none"
        val districtId = SessionManager.getUserDistrict(this) ?: "none"

        FirebaseCrashlytics.getInstance().apply {
            setUserId(userId.toString())
            setCustomKey("user_role",   role)
            setCustomKey("region_id",   regionId)
            setCustomKey("district_id", districtId)
            setCustomKey("screen",      "MainActivity")
            log("MainActivity launched — role: $role")
        }
    }
}