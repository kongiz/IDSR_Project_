package com.idsr_project.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.idsr_project.R
import com.idsr_project.utils.NetworkStatusHelper
import com.idsr_project.utils.SessionManager

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var banner: TextView
    private val timeoutHandler     = Handler(Looper.getMainLooper())
    private val warningHandler     = Handler(Looper.getMainLooper())
    private var timeoutDialog: AlertDialog? = null

    private val WARNING_AFTER_MS   = 25 * 60 * 1000L
    private val LOGOUT_AFTER_MS    = 5  * 60 * 1000L  // logout 5 mins after warning
    private var isTimeoutActive    = false

    open val excludeFromTimeout: Boolean = false

    private val showWarningRunnable = Runnable {
        if (!isFinishing && !excludeFromTimeout) {
            showTimeoutWarning()
        }
    }

    private val logoutRunnable = Runnable {
        if (!isFinishing) {
            performSessionTimeout()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeConnectivity()
    }

    override fun onResume() {
        super.onResume()
        if (!excludeFromTimeout) {
            resetTimeoutTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimeoutTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimeoutTimer()
        timeoutDialog?.dismiss()
    }


    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!excludeFromTimeout) {
            resetTimeoutTimer()
        }
    }

    private fun resetTimeoutTimer() {
        stopTimeoutTimer()
        timeoutDialog?.dismiss()
        timeoutDialog = null
        isTimeoutActive = false
        timeoutHandler.postDelayed(showWarningRunnable, WARNING_AFTER_MS)
    }

    private fun stopTimeoutTimer() {
        timeoutHandler.removeCallbacks(showWarningRunnable)
        warningHandler.removeCallbacks(logoutRunnable)
    }

    private fun showTimeoutWarning() {
        if (timeoutDialog?.isShowing == true) return
        isTimeoutActive = true

        timeoutDialog = AlertDialog.Builder(this)
            .setTitle("Still there?")
            .setMessage(
                "You've been inactive for 25 minutes.\n\n" +
                        "For security, you'll be logged out in 5 minutes " +
                        "unless you tap 'Stay Logged In'."
            )
            .setPositiveButton("Stay Logged In") { _, _ ->
                resetTimeoutTimer()
            }
            .setNegativeButton("Log Out") { _, _ ->
                performSessionTimeout()
            }
            .setCancelable(false)
            .create()

        timeoutDialog?.show()

        warningHandler.postDelayed(logoutRunnable, LOGOUT_AFTER_MS)
    }


    private fun performSessionTimeout() {
        stopTimeoutTimer()
        timeoutDialog?.dismiss()
        timeoutDialog = null

        SessionManager.clearSession(this)

        val intent = Intent(this, Login_Activity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("session_timeout", true)
        }
        startActivity(intent)
        finish()
    }


    override fun setContentView(layoutResID: Int) {
        val rootLayout = buildRootLayout()
        super.setContentView(rootLayout)
        LayoutInflater.from(this).inflate(layoutResID, rootLayout.findViewWithTag("content"), true)
    }

    override fun setContentView(view: View?) {
        val rootLayout = buildRootLayout()
        rootLayout.findViewWithTag<FrameLayout>("content").addView(view)
        super.setContentView(rootLayout)
    }

    private fun buildRootLayout(): LinearLayout {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        banner = LayoutInflater.from(this)
            .inflate(R.layout.layout_connectivity_banner, rootLayout, false) as TextView
        rootLayout.addView(banner)

        val contentContainer = FrameLayout(this).apply {
            tag = "content"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        }
        rootLayout.addView(contentContainer)

        return rootLayout
    }

    @SuppressLint("SetTextI18n")
    private fun observeConnectivity() {
        NetworkStatusHelper(this).observe(this) { isOnline ->
            if (isOnline) {
                if (banner.isVisible) {
                    banner.text = "Back Online"
                    banner.setBackgroundColor(ContextCompat.getColor(this, R.color.idsr_green))
                    banner.post {
                        banner.animate()
                            .translationY(-banner.height.toFloat())
                            .setDuration(400)
                            .withEndAction { banner.visibility = View.GONE }
                            .start()
                    }
                }
            } else {
                banner.text = "No Internet Connection"
                banner.setBackgroundColor(ContextCompat.getColor(this, R.color.idsr_error))
                banner.visibility = View.VISIBLE
                banner.translationY = -300f
                banner.post {
                    banner.animate()
                        .translationY(0f)
                        .setDuration(400)
                        .start()
                }
            }
        }
    }
}