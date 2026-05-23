package com.idsr_project.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.OtpResponse
import com.idsr_project.Model.ResendOtpRequest
import com.idsr_project.Model.VerifyEmailRequest
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityEmailVerifyBinding
import com.idsr_project.utils.applyWindowInsets
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmailVerifyActivity : BaseActivity() {

    override val excludeFromTimeout: Boolean = true
    private lateinit var binding: ActivityEmailVerifyBinding
    private var email: String = ""
    private var mode: String = "VERIFY_EMAIL"
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEmailVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(topView = binding.appBarLayout)

        email = intent.getStringExtra("email") ?: ""
        mode  = intent.getStringExtra("mode")  ?: "VERIFY_EMAIL"

        setupUI()
        setupClickListeners()
        startResendTimer()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        binding.txtEmailHint.text = "We sent a 6-digit code to\n$email"
        binding.txtTitle.text = if (mode == "VERIFY_EMAIL")
            "Verify Your Email" else "Enter Reset Code"
        binding.txtSubtitle.text = if (mode == "VERIFY_EMAIL")
            "Enter the code to activate your account"
        else "Enter the code to reset your password"
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6) {
                binding.tilOtp.error = "Enter the 6-digit code"
                return@setOnClickListener
            }
            binding.tilOtp.error = null
            if (mode == "VERIFY_EMAIL") verifyEmail(otp) else verifyResetOtp(otp)
        }

        binding.btnResend.setOnClickListener {
            resendOtp()
        }
    }

    private fun verifyEmail(otp: String) {
        showLoading(true)
        ApiClient.getClient(this)
            .verifyEmail(VerifyEmailRequest(email, otp))
            .enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                    showLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        Toast.makeText(
                            this@EmailVerifyActivity,
                            "Email verified! You can now login.",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this@EmailVerifyActivity, Login_Activity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
                        binding.tilOtp.error = body?.message ?: "Invalid code"
                    }
                }
                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@EmailVerifyActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun verifyResetOtp(otp: String) {
        showLoading(true)
        ApiClient.getClient(this)
            .verifyResetOtp(com.idsr_project.Model.VerifyResetOtpRequest(email, otp))
            .enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                    showLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        startActivity(
                            Intent(this@EmailVerifyActivity, ResetPasswordActivity::class.java).apply {
                                putExtra("email", email)
                                putExtra("otp", binding.etOtp.text.toString().trim())
                            }
                        )
                    } else {
                        binding.tilOtp.error = body?.message ?: "Invalid code"
                    }
                }
                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@EmailVerifyActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun resendOtp() {
        showLoading(true)
        ApiClient.getClient(this)
            .resendOtp(ResendOtpRequest(email))
            .enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                    showLoading(false)
                    Toast.makeText(
                        this@EmailVerifyActivity,
                        response.body()?.message ?: "Code resent",
                        Toast.LENGTH_SHORT
                    ).show()
                    startResendTimer()
                }
                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@EmailVerifyActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun startResendTimer() {
        binding.btnResend.isEnabled = false
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                binding.btnResend.text = "Resend in ${millisUntilFinished / 1000}s"
            }
            override fun onFinish() {
                binding.btnResend.isEnabled = true
                binding.btnResend.text = "Resend Code"
            }
        }.start()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnVerify.isEnabled = !isLoading
        binding.btnVerify.text = if (isLoading) "Verifying..." else "Verify"
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}