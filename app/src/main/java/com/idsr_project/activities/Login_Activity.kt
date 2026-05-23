package com.idsr_project.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.idsr_project.Model.LoginRequest
import com.idsr_project.Model.OtpResponse
import com.idsr_project.Model.loginResponse
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityLoginBinding
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets
import com.idsr_project.workers.ReferenceDataSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login_Activity : BaseActivity() {

    override val excludeFromTimeout: Boolean = true
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(topView = binding.appBarLayout)

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
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
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
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
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
                                    SessionManager.saveTokens(
                                        context = this@Login_Activity,
                                        accessToken = res.access_token,
                                        refreshToken = res.refresh_token
                                    )

                                    val crashlytics = FirebaseCrashlytics.getInstance()
                                    crashlytics.setUserId(res.user.id.toString())
                                    crashlytics.setCustomKey("user_role", res.user.role ?: "Health Officer")
                                    crashlytics.setCustomKey("user_email", res.user.email)
                                    crashlytics.setCustomKey("region_id", res.user.regionId?.toString() ?: "none")
                                    crashlytics.setCustomKey("district_id", res.user.districtId?.toString() ?: "none")
                                    crashlytics.log("User logged in: ${res.user.firstname} ${res.user.lastname}")

                                    withContext(Dispatchers.Main) { registerFcmToken() }
                                    withContext(Dispatchers.Main) { ReferenceDataSyncWorker.schedule(this@Login_Activity) }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@Login_Activity,
                                            "Welcome ${res.user.firstname}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startActivity(
                                            Intent(
                                                this@Login_Activity,
                                                MainActivity::class.java
                                            )
                                        )
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
                        val errorJson = response.errorBody()?.string()
                        try {
                            val errorBody = com.google.gson.Gson()
                                .fromJson(errorJson, OtpResponse::class.java)

                            when {
                                errorBody?.requiresVerification == true -> {
                                    startActivity(
                                        Intent(
                                            this@Login_Activity,
                                            EmailVerifyActivity::class.java
                                        ).apply {
                                            putExtra("email", errorBody.email)
                                            putExtra("mode", "VERIFY_EMAIL")
                                        }
                                    )
                                }
                                response.code() == 401 -> {
                                    binding.tilPassword.error = "Invalid email or password"
                                }

                                else -> {
                                    binding.tilPassword.error = errorBody?.message ?: "Login failed"
                                }
                            }
                        } catch (e: Exception) {
                            binding.tilPassword.error = "Invalid email or password"
                            Log.e("LOGIN", errorJson ?: "Login error")
                        }
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

    @SuppressLint("HardwareIds")
    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result
            val deviceId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            SessionManager.saveFcmToken(this, token)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    ApiClient.getClient(this@Login_Activity)
                        .saveFcmToken(mapOf("fcm_token" to token, "device_id" to deviceId)).execute()
                    Log.d("FCM", "Token registered after login")
                } catch (e: Exception) {
                    Log.e("FCM", "Token registration failed: ${e.message}")
                }
            }
        }
    }
}