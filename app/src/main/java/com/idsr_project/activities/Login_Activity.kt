package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.idsr_project.Model.LoginRequest
import com.idsr_project.Model.loginResponse
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityLoginBinding
import com.idsr_project.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {

        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUp1Activity::class.java))
        }

        binding.etEmail.setOnFocusChangeListener { _, _ ->
            binding.tilEmail.error = null
        }

        binding.etPassword.setOnFocusChangeListener { _, _ ->
            binding.tilPassword.error = null
        }

        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }
    }

    private fun validateAndLogin() {

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                binding.etEmail.requestFocus()
                return
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Enter a valid email address"
                binding.etEmail.requestFocus()
                return
            }
        }

        when {
            password.isEmpty() -> {
                binding.tilPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return
            }

            password.length < 8 -> {
                binding.tilPassword.error = "Password must be at least 8 characters"
                binding.etPassword.requestFocus()
                return
            }

            !password.any { it.isDigit() } -> {
                binding.tilPassword.error = "Password must contain at least one number"
                binding.etPassword.requestFocus()
                return
            }
        }

        loginUser(email, password)
    }

    private fun loginUser(email: String, password: String) {

        // Prevent double clicks
        if (!binding.btnLogin.isEnabled) return

        showLoading(true)

        val request = LoginRequest(
            email = email,
            password = password
        )

        ApiClient.getClient(this).loginUser(request)
            .enqueue(object : Callback<loginResponse> {

                override fun onResponse(
                    call: Call<loginResponse>,
                    response: Response<loginResponse>
                ) {
                    showLoading(false)

                    if (response.isSuccessful && response.body() != null) {

                        val res = response.body()!!

                        if (res.success && res.user != null) {

                            // Save session and navigate AFTER saving completes
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    // Save user session
                                    SessionManager.saveUserSession(
                                        context = this@Login_Activity,
                                        userId = res.user.id,
                                        firstname = res.user.firstname,
                                        lastname = res.user.lastname,
                                        email = res.user.email,
                                        phone = res.user.phone ?: "",
                                        role = res.user.role ?: "Health Officer",
                                        region = res.user.regionId?.toString(),
                                        district = res.user.districtId?.toString(),
                                        accessToken = res.access_token,
                                        refreshToken = res.refresh_token
                                    )

                                    // Navigate on Main thread AFTER saving
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@Login_Activity,
                                            "Welcome ${res.user.firstname}",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val intent = Intent(
                                            this@Login_Activity,
                                            MainActivity::class.java
                                        )
                                        startActivity(intent)
                                        finish()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@Login_Activity,
                                            "Error saving session",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        Log.e("LOGIN", "Session save error: ${e.message}")
                                    }
                                }
                            }

                        } else {
                            binding.tilPassword.error = res.msg ?: "Login failed"
                        }

                    } else {
                        binding.tilPassword.error = "Invalid email or password"
                        Log.e("LOGIN", response.errorBody()?.string() ?: "Login error")
                    }
                }

                override fun onFailure(call: Call<loginResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@Login_Activity,
                        t.localizedMessage ?: "Network error",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("LOGIN", "Failure: ${t.message}")
                }
            })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressLogin.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text =
            if (isLoading) "Please wait..." else "Login"
    }
}