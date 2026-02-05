package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.idsr_project.databinding.ActivitySignUp1Binding
import com.idsr_project.utils.ThemeManager

class SignUp1Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUp1Binding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUp1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val registerMode = intent.getStringExtra("REGISTER_MODE") ?: "SELF"
        val currentUserRole = intent.getStringExtra("CURRENT_USER_ROLE") ?: ""

        setupListeners(registerMode, currentUserRole)
    }

    private fun setupListeners(registerMode: String, currentUserRole: String) {

        binding.btnBackSignup1.setOnClickListener {
            finish()
        }


        binding.btnAlryLogin.setOnClickListener {
            startActivity(Intent(this, Login_Activity::class.java))
            finish()
        }


        binding.etFirstname.setOnFocusChangeListener { _, _ ->
            binding.tilFirstname.error = null
        }

        binding.etLastname.setOnFocusChangeListener { _, _ ->
            binding.tilLastname.error = null
        }


        binding.btnNext1.setOnClickListener {
            validateAndProceed(registerMode, currentUserRole)
        }
    }

    private fun validateAndProceed(registerMode: String, currentUserRole: String) {

        val firstname = binding.etFirstname.text.toString().trim()
        val lastname = binding.etLastname.text.toString().trim()


        binding.tilFirstname.error = null
        binding.tilLastname.error = null


        when {
            firstname.isEmpty() -> {
                binding.tilFirstname.error = "First name is required"
                binding.etFirstname.requestFocus()
                return
            }
            !firstname.matches(Regex("^[A-Za-z]+$")) -> {
                binding.tilFirstname.error = "First name must contain only letters"
                binding.etFirstname.requestFocus()
                return
            }
            firstname.length < 2 -> {
                binding.tilFirstname.error = "First name is too short"
                binding.etFirstname.requestFocus()
                return
            }
        }

        when {
            lastname.isEmpty() -> {
                binding.tilLastname.error = "Last name is required"
                binding.etLastname.requestFocus()
                return
            }
            !lastname.matches(Regex("^[A-Za-z]+$")) -> {
                binding.tilLastname.error = "Last name must contain only letters"
                binding.etLastname.requestFocus()
                return
            }
            lastname.length < 2 -> {
                binding.tilLastname.error = "Last name is too short"
                binding.etLastname.requestFocus()
                return
            }
        }

        val intent = Intent(this, SignUp2Activity::class.java)
        intent.putExtra("FIRSTNAME", firstname)
        intent.putExtra("LASTNAME", lastname)
        intent.putExtra("REGISTER_MODE", registerMode)
        intent.putExtra("CURRENT_USER_ROLE", currentUserRole)
        startActivity(intent)
    }
}
