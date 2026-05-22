package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.Model.OtpResponse
import com.idsr_project.Model.ResetPasswordRequest
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityResetPasswordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private var email: String = ""
    private var otp: String   = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra("email") ?: ""
        otp   = intent.getStringExtra("otp")   ?: ""

        binding.btnBack.setOnClickListener { finish() }

        binding.btnResetPassword.setOnClickListener {
            if (validateInputs()) resetPassword()
        }
    }

    private fun validateInputs(): Boolean {
        val password = binding.etNewPassword.text.toString().trim()
        val confirm  = binding.etConfirmPassword.text.toString().trim()

        binding.tilNewPassword.error     = null
        binding.tilConfirmPassword.error = null

        if (password.isEmpty()) {
            binding.tilNewPassword.error = "Password is required"
            return false
        }
        if (password.length < 8) {
            binding.tilNewPassword.error = "Password must be at least 8 characters"
            return false
        }
        if (!password.any { it.isDigit() }) {
            binding.tilNewPassword.error = "Password must contain at least one number"
            return false
        }
        if (password != confirm) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun resetPassword() {
        showLoading(true)
        val newPassword = binding.etNewPassword.text.toString().trim()

        ApiClient.getClient(this)
            .resetPassword(ResetPasswordRequest(email, otp, newPassword))
            .enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                    showLoading(false)
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Password reset successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(
                            Intent(this@ResetPasswordActivity, Login_Activity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    } else {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            body?.message ?: "Reset failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@ResetPasswordActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility       = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled   = !isLoading
        binding.btnResetPassword.text        = if (isLoading) "Resetting..." else "Reset Password"
    }
}