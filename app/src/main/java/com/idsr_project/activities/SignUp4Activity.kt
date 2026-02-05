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
import com.idsr_project.Model.ResponseApi
import com.idsr_project.Model.userSignup
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivitySignUp4Binding
import com.idsr_project.utils.ThemeManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUp4Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUp4Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUp4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPasswordWatchers()

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
                binding.tilConfirmPassword.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updatePasswordStrength(password: String) {
        var score = 0

        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        when (score) {
            0, 1 -> {
                binding.passwordStrengthBar.progress = 25
                binding.tvPasswordStrength.text = "Weak password"
            }
            2 -> {
                binding.passwordStrengthBar.progress = 50
                binding.tvPasswordStrength.text = "Medium strength"
            }
            3 -> {
                binding.passwordStrengthBar.progress = 75
                binding.tvPasswordStrength.text = "Strong password"
            }
            4 -> {
                binding.passwordStrengthBar.progress = 100
                binding.tvPasswordStrength.text = "Very strong password"
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

                val res = response.body()
                if (response.isSuccessful && res?.status == "success") {

                    Toast.makeText(this@SignUp4Activity, res.msg, Toast.LENGTH_SHORT).show()


                    startActivity(
                        Intent(this@SignUp4Activity, MainActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )

                } else {
                    binding.tilPassword.error = res?.msg ?: "Signup failed"
                }
            }

            override fun onFailure(call: Call<ResponseApi>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@SignUp4Activity, t.message, Toast.LENGTH_SHORT).show()
                Log.e("SignUp4", "Signup error", t)
            }
        }
    }



    private fun validatePasswords(password: String, confirmPassword: String): Boolean {

        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        return when {
            password.length < 8 -> {
                binding.tilPassword.error = "Minimum 8 characters required"
                false
            }
            password.none { it.isUpperCase() } -> {
                binding.tilPassword.error = "Must contain uppercase letter"
                false
            }
            password.none { it.isDigit() } -> {
                binding.tilPassword.error = "Must contain a number"
                false
            }
            password.none { !it.isLetterOrDigit() } -> {
                binding.tilPassword.error = "Must contain a symbol"
                false
            }
            confirmPassword != password -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                false
            }
            else -> true
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressSignup.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }
}
