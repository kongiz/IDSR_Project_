package com.idsr_project.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.idsr_project.R
import com.idsr_project.utils.NetworkStatusHelper
import androidx.core.graphics.toColorInt

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var banner: TextView


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeConnectivity()
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
                banner.text = "Offline — Saving to Device"
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