package com.idsr_project.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.R
import com.idsr_project.databinding.ActivityErrorBinding

class Error_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityErrorBinding

    companion object {
        const val EXTRA_ERROR_CODE    = "error_code"
        const val EXTRA_ERROR_TITLE   = "error_title"
        const val EXTRA_ERROR_MESSAGE = "error_message"

        fun launch401(context: Context) {
            context.startActivity(
                Intent(context, Error_Activity::class.java).apply {
                    putExtra(EXTRA_ERROR_CODE,    401)
                    putExtra(EXTRA_ERROR_TITLE,   "Session Expired")
                    putExtra(EXTRA_ERROR_MESSAGE, "Your session has expired. Please log in again.")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        fun launch403(context: Context) {
            context.startActivity(
                Intent(context, Error_Activity::class.java).apply {
                    putExtra(EXTRA_ERROR_CODE,    403)
                    putExtra(EXTRA_ERROR_TITLE,   "Access Denied")
                    putExtra(EXTRA_ERROR_MESSAGE, "You don't have permission to perform this action.")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        fun launch404(context: Context) {
            context.startActivity(
                Intent(context, Error_Activity::class.java).apply {
                    putExtra(EXTRA_ERROR_CODE,    404)
                    putExtra(EXTRA_ERROR_TITLE,   "Not Found")
                    putExtra(EXTRA_ERROR_MESSAGE, "The resource you're looking for doesn't exist.")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        fun launch500(context: Context) {
            context.startActivity(
                Intent(context, Error_Activity::class.java).apply {
                    putExtra(EXTRA_ERROR_CODE,    500)
                    putExtra(EXTRA_ERROR_TITLE,   "Server Error")
                    putExtra(EXTRA_ERROR_MESSAGE, "Something went wrong on our end. Please try again later.")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }

        fun launchNoInternet(context: Context) {
            context.startActivity(
                Intent(context, Error_Activity::class.java).apply {
                    putExtra(EXTRA_ERROR_CODE,    0)
                    putExtra(EXTRA_ERROR_TITLE,   "No Internet")
                    putExtra(EXTRA_ERROR_MESSAGE, "Please check your connection and try again.")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val code    = intent.getIntExtra(EXTRA_ERROR_CODE, 0)
        val title   = intent.getStringExtra(EXTRA_ERROR_TITLE)   ?: "Error"
        val message = intent.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "Something went wrong."

        binding.txtErrorCode.text    = if (code != 0) code.toString() else ""
        binding.txtErrorTitle.text   = title
        binding.txtErrorMessage.text = message


        val animRes = when (code) {
            401  -> R.raw.anim_offline
            403  -> R.raw.anim_forbidden_403
            404  -> R.raw.anim_error_404
            500  -> R.raw.anim_server_error
            0    -> R.raw.anim_no_connection
            else -> R.raw.anim_error
        }
        binding.lottieError.setAnimation(animRes)
        binding.lottieError.playAnimation()

        // action button behaviour
        when (code) {
            401 -> {
                binding.btnAction.text = "Go to Login"
                binding.btnRetry.visibility = android.view.View.GONE
                binding.btnAction.setOnClickListener {
                    startActivity(
                        Intent(this, Login_Activity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
            }
            0 -> {

                binding.btnAction.text = "Go Back"
                binding.btnAction.setOnClickListener { finish() }
                binding.btnRetry.setOnClickListener { finish() }
            }
            else -> {
                binding.btnAction.text = "Go Back"
                binding.btnRetry.visibility = android.view.View.GONE
                binding.btnAction.setOnClickListener { finish() }
            }
        }
    }
}