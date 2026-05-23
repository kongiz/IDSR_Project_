package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.userSignup
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivitySignUp4Binding
import com.idsr_project.utils.applyWindowInsets
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUp4Activity : BaseActivity() {
    override val excludeFromTimeout: Boolean = true

    private lateinit var binding: ActivitySignUp4Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUp4Binding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(binding.appBarLayout)

        setupPasswordWatchers()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBackSignup4.setOnClickListener { finish() }
        binding.btnRegister.setOnClickListener { submitSignup() }
    }

    private fun setupPasswordWatchers() {
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s.toString())
                binding.tilPassword.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

                if (!s.isNullOrEmpty()) {
                    validatePasswordMatch(binding.etPassword.text.toString(), s.toString())
                } else {
                    binding.tilConfirmPassword.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validatePasswordMatch(password: String, confirmPassword: String) {
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
        } else {
            binding.tilConfirmPassword.error = null
        }
    }

    private fun updatePasswordStrength(password: String) {
        var score = 0


        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++


        when (score) {
            0, 1 -> {
                binding.passwordStrengthBar.apply {
                    progress = 25
                    setIndicatorColor(ContextCompat.getColor(this@SignUp4Activity, R.color.idsr_error))
                }
                binding.tvPasswordStrength.apply {
                    text = "Weak password"
                    setTextColor(ContextCompat.getColor(this@SignUp4Activity, R.color.idsr_error))
                }
            }
            2 -> {
                binding.passwordStrengthBar.apply {
                    progress = 50
                    setIndicatorColor(ContextCompat.getColor(this@SignUp4Activity, android.R.color.holo_orange_dark))
                }
                binding.tvPasswordStrength.apply {
                    text = "Medium strength"
                    setTextColor(ContextCompat.getColor(this@SignUp4Activity, android.R.color.holo_orange_dark))
                }
            }
            3 -> {
                binding.passwordStrengthBar.apply {
                    progress = 75
                    setIndicatorColor(ContextCompat.getColor(this@SignUp4Activity, android.R.color.holo_blue_dark))
                }
                binding.tvPasswordStrength.apply {
                    text = "Strong password"
                    setTextColor(ContextCompat.getColor(this@SignUp4Activity, android.R.color.holo_blue_dark))
                }
            }
            4 -> {
                binding.passwordStrengthBar.apply {
                    progress = 100
                    setIndicatorColor(ContextCompat.getColor(this@SignUp4Activity, android.R.color.holo_green_dark))
                }
                binding.tvPasswordStrength.apply {
                    text = "Very strong password"
                    setTextColor(ContextCompat.getColor(this@SignUp4Activity, android.R.color.holo_green_dark))
                }
            }
        }
    }

    private fun submitSignup() {
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (!validatePasswords(password, confirmPassword)) return

        showLoading(true)

        val api = ApiClient.getClient(this)
        val registerMode = intent.getStringExtra("REGISTER_MODE") ?: "SELF"

        if (registerMode == "SELF") {
            val request = userSignup(
                firstname = intent.getStringExtra("FIRSTNAME").orEmpty(),
                lastname = intent.getStringExtra("LASTNAME").orEmpty(),
                phone = intent.getStringExtra("PHONE").orEmpty(),
                email = intent.getStringExtra("EMAIL").orEmpty(),
                gender = intent.getStringExtra("GENDER").orEmpty(),
                role = intent.getStringExtra("ROLE").orEmpty(),
                region_id = intent.getIntExtra("REGION_ID", -1),
                district_id = intent.getIntExtra("DISTRICT_ID", -1),
                password = password
            )

            api.registerUser(request).enqueue(handleResponse())
        } else {
            val data = hashMapOf(
                "firstname" to intent.getStringExtra("FIRSTNAME").orEmpty(),
                "lastname" to intent.getStringExtra("LASTNAME").orEmpty(),
                "phone" to intent.getStringExtra("PHONE").orEmpty(),
                "email" to intent.getStringExtra("EMAIL").orEmpty(),
                "gender" to intent.getStringExtra("GENDER").orEmpty(),
                "role" to intent.getStringExtra("ROLE").orEmpty(),
                "region_id" to intent.getIntExtra("REGION_ID", -1).toString(),
                "district_id" to intent.getIntExtra("DISTRICT_ID", -1).toString(),
                "password" to password,
                "creator_role" to intent.getStringExtra("CURRENT_USER_ROLE").orEmpty(),
                "creator_region_id" to intent.getIntExtra("CREATOR_REGION_ID", -1).toString(),
                "creator_district_id" to intent.getIntExtra("CREATOR_DISTRICT_ID", -1).toString()
            )

            api.registerPrivilegeUser(data).enqueue(handleResponse())
        }
    }

    private fun handleResponse(): Callback<ResponseApi> {
        return object : Callback<ResponseApi> {
            override fun onResponse(call: Call<ResponseApi>, response: Response<ResponseApi>) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    val res = response.body()!!
                    Toast.makeText(this@SignUp4Activity, res.msg, Toast.LENGTH_SHORT).show()

                    val registerMode = intent.getStringExtra("REGISTER_MODE") ?: "SELF"

                    if (registerMode == "SELF") {
                        val email = intent.getStringExtra("EMAIL") ?: ""
                        startActivity(
                            Intent(this@SignUp4Activity, EmailVerifyActivity::class.java).apply {
                                putExtra("email", email)
                                putExtra("mode", "VERIFY_EMAIL")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                        finish()
                    } else {
                        Toast.makeText(this@SignUp4Activity, "User registered successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                } else {
                    val errorMessage = response.body()?.msg ?: "Signup failed. Please try again."
                    binding.tilPassword.error = errorMessage
                    Toast.makeText(this@SignUp4Activity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                showLoading(false)
                Toast.makeText(
                    this@SignUp4Activity,
                    "Network error. Please check your connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("SignUp4", "Signup error", t)
            }
        }
    }
    private fun validatePasswords(password: String, confirmPassword: String): Boolean {
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        return when {
            password.isEmpty() -> {
                binding.tilPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                false
            }
            password.length < 8 -> {
                binding.tilPassword.error = "Minimum 8 characters required"
                binding.etPassword.requestFocus()
                false
            }
            password.none { it.isUpperCase() } -> {
                binding.tilPassword.error = "Must contain uppercase letter"
                binding.etPassword.requestFocus()
                false
            }
            password.none { it.isDigit() } -> {
                binding.tilPassword.error = "Must contain a number"
                binding.etPassword.requestFocus()
                false
            }
            password.none { !it.isLetterOrDigit() } -> {
                binding.tilPassword.error = "Must contain a symbol"
                binding.etPassword.requestFocus()
                false
            }
            confirmPassword.isEmpty() -> {
                binding.tilConfirmPassword.error = "Please confirm your password"
                binding.etConfirmPassword.requestFocus()
                false
            }
            confirmPassword != password -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                binding.etConfirmPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressSignup.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading


        if (loading) {
            binding.btnRegister.text = "Creating account..."
        } else {
            binding.btnRegister.text = "Create account"
        }
    }
}