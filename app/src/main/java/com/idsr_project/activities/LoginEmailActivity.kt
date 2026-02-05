//package com.idsr_project.activities
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Patterns
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import com.idsr_project.databinding.ActivityLoginEmailBinding
//
//class LoginEmailActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityLoginEmailBinding
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        val tvSignup = binding.tvSignup
//        tvSignup.setOnClickListener {
//            val intent = Intent(this, SignUp1Activity::class.java)
//            startActivity(intent)
//        }
//
//        binding.btnLogin1.setOnClickListener {
//            val email = binding.etEmail.text.toString()
//
//            when {
//                email.isEmpty() -> {
//                    binding.etEmail.error = "Email is required"
//                    binding.etEmail.requestFocus()
//                }
//                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
//                    binding.etEmail.error = "Invalid Email"
//                    binding.etEmail.requestFocus()
//                }
//                else -> {
//                    val intent = Intent(this, LoginPassActivity::class.java)
//                    intent.putExtra("USER_EMAIL", binding.etEmail.text.toString().trim())
//                    startActivity(intent)
//                }
//            }
//        }
//
//
//    }
//}