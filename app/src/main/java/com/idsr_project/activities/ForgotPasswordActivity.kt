package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.ForgotPasswordRequest
import com.idsr_project.Model.OtpResponse
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityForgotPasswordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class ForgotPasswordActivity : BaseActivity() {

    override val excludeFromTimeout: Boolean = true

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)



        binding.btnBack.setOnClickListener { finish() }

        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            sendResetCode(email)
        }
    }

    private fun sendResetCode(email: String) {
        showLoading(true)
        ApiClient.getClient(this)
            .forgotPassword(ForgotPasswordRequest(email))
            .enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                    showLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            body.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(
                            Intent(this@ForgotPasswordActivity, EmailVerifyActivity::class.java).apply {
                                putExtra("email", email)
                                putExtra("mode", "PASSWORD_RESET")
                            }
                        )
                    } else {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            body?.message ?: "Failed to send code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@ForgotPasswordActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility  = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSendCode.isEnabled   = !isLoading
        binding.btnSendCode.text        = if (isLoading) "Sending..." else "Send Code"
    }
}