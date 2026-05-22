package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.databinding.ActivitySignUp2Binding
import com.idsr_project.utils.ThemeManager

class SignUp2Activity : BaseActivity() {
    override val excludeFromTimeout: Boolean = true

    private lateinit var binding: ActivitySignUp2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUp2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val firstname = intent.getStringExtra("FIRSTNAME")
        val lastname = intent.getStringExtra("LASTNAME")
        val registerMode = intent.getStringExtra("REGISTER_MODE") ?: "SELF"
        val currentUserRole = intent.getStringExtra("CURRENT_USER_ROLE") ?: ""

        setupListeners(firstname, lastname, registerMode, currentUserRole)
    }

    private fun setupListeners(
        firstname: String?,
        lastname: String?,
        registerMode: String,
        currentUserRole: String
    ) {


        binding.btnBackSignup2.setOnClickListener {
            finish()
        }


        binding.etPhone.setOnFocusChangeListener { _, _ ->
            binding.tilPhone.error = null
        }

        binding.etEmail.setOnFocusChangeListener { _, _ ->
            binding.tilEmail.error = null
        }


        binding.btnNext2.setOnClickListener {
            validateAndProceed(firstname, lastname, registerMode, currentUserRole)
        }
    }

    private fun validateAndProceed(
        firstname: String?,
        lastname: String?,
        registerMode: String,
        currentUserRole: String
    ) {

        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()


        binding.tilPhone.error = null
        binding.tilEmail.error = null


        when {
            phone.isEmpty() -> {
                binding.tilPhone.error = "Phone number is required"
                binding.etPhone.requestFocus()
                return
            }
            !phone.matches(Regex("^[0-9]+$")) -> {
                binding.tilPhone.error = "Phone number must contain only digits"
                binding.etPhone.requestFocus()
                return
            }
            phone.length != 7 -> {
                binding.tilPhone.error = "Phone number must be exactly 7 digits"
                binding.etPhone.requestFocus()
                return
            }
        }


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


        val intent = Intent(this, SignUp3Activity::class.java)
        intent.putExtra("FIRSTNAME", firstname)
        intent.putExtra("LASTNAME", lastname)
        intent.putExtra("PHONE", phone)
        intent.putExtra("EMAIL", email)
        intent.putExtra("REGISTER_MODE", registerMode)
        intent.putExtra("CURRENT_USER_ROLE", currentUserRole)
        startActivity(intent)
    }
}
