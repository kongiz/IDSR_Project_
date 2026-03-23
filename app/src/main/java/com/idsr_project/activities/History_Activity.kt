package com.idsr_project.activities

import FormAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.idsr_project.Adapter.OtherFormsAdapter
import com.idsr_project.Model.*
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityHistoryBinding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class History_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    private lateinit var surveillanceAdapter: FormAdapter
    private lateinit var otherFormsAdapter: OtherFormsAdapter

    private val surveillanceForms = mutableListOf<FormData>()
    private val otherForms = mutableListOf<OtherFormsData>()

    private var activeTab = TAB_SURVEILLANCE
    private var isLoading = false

    companion object {
        private const val TAB_SURVEILLANCE = "surveillance"
        private const val TAB_OTHER = "other"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupTabs()
        setupSwipeRefresh()

        setActiveTab(TAB_SURVEILLANCE)
        fetchSurveillance()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerViews() {
        surveillanceAdapter = FormAdapter(
            forms = surveillanceForms,
            onItemClick = { form ->
                startActivity(
                    Intent(this, HistoryDetail_Activity::class.java)
                        .putExtra("form", form)
                )
            }
        )

        otherFormsAdapter = OtherFormsAdapter(otherForms) { item ->
            when (item.type) {
                "Annex2F Immediate Case Report" ->
                    startActivity(
                        Intent(this, Annex2F_Immediate_Details::class.java)
                            .putExtra("data", item.rawData)
                    )

                "Annex2G Lab Report (Specimen)" ->
                    startActivity(
                        Intent(this, Annex2G_LabReport_Detail_Activity::class.java)
                            .putExtra("data", item.rawData)
                    )

                "Lab Report (Final Result)" ->
                    startActivity(
                        Intent(this, Lab_Reports_Detail_Activity::class.java)
                            .putExtra("data", item.rawData)
                    )
            }
        }

        binding.rvSurveillanceForms.apply {
            layoutManager = LinearLayoutManager(this@History_Activity)
            adapter = surveillanceAdapter
        }

        binding.rvOtherForms.apply {
            layoutManager = LinearLayoutManager(this@History_Activity)
            adapter = otherFormsAdapter
        }
    }

    private fun setupTabs() {
        binding.btnSurveillance.setOnClickListener {
            setActiveTab(TAB_SURVEILLANCE)
            fetchSurveillance()
        }

        binding.btnOtherForms.setOnClickListener {
            setActiveTab(TAB_OTHER)
            fetchOtherForms()
        }
    }

    private fun setupSwipeRefresh() {

        val primaryColor = MaterialColors.getColor(
            binding.root,
            android.R.attr.colorPrimary
        )

        binding.swipeRefresh.setColorSchemeColors(primaryColor)

        binding.swipeRefresh.setOnRefreshListener {
            if (activeTab == TAB_SURVEILLANCE) {
                fetchSurveillance()
            } else {
                fetchOtherForms()
            }
        }
    }

    private fun setActiveTab(tab: String) {
        activeTab = tab

        val primaryColor = MaterialColors.getColor(
            binding.root,
            android.R.attr.colorPrimary
        )

        val onPrimaryColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOnPrimary
        )

        val surfaceVariantColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorSurfaceVariant
        )

        val onSurfaceColor = MaterialColors.getColor(
            binding.root,
            android.R.attr.textColor
        )

        if (tab == TAB_SURVEILLANCE) {

            binding.btnSurveillance.setBackgroundColor(primaryColor)
            binding.btnSurveillance.setTextColor(onPrimaryColor)


            binding.btnOtherForms.setBackgroundColor(surfaceVariantColor)
            binding.btnOtherForms.setTextColor(onSurfaceColor)


            binding.rvSurveillanceForms.visibility = View.VISIBLE
            binding.rvOtherForms.visibility = View.GONE
        } else {

            binding.btnOtherForms.setBackgroundColor(primaryColor)
            binding.btnOtherForms.setTextColor(onPrimaryColor)


            binding.btnSurveillance.setBackgroundColor(surfaceVariantColor)
            binding.btnSurveillance.setTextColor(onSurfaceColor)


            binding.rvSurveillanceForms.visibility = View.GONE
            binding.rvOtherForms.visibility = View.VISIBLE
        }

        binding.emptyStateLayout.visibility = View.GONE
    }

    private fun finishLoading() {
        isLoading = false
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    private fun showEmpty(show: Boolean) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE

        if (show) {
            binding.txtEmptyMessage.text = if (activeTab == TAB_SURVEILLANCE) {
                "No surveillance reports found"
            } else {
                "No other forms found"
            }
        }
    }

    private fun fetchSurveillance() {
        if (isLoading) return
        isLoading = true

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        val request = FormRequest(
            userId = SessionManager.getUserId(this),
            role = SessionManager.getUserRole(this) ?: "Health Officer",
            type = "surveillance"
        )

        ApiClient.getClient(this)
            .getSurveillanceReport("Bearer ${SessionManager.getAccessToken(this)}")
            .enqueue(object : Callback<FormResponse> {

                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<FormResponse>,
                    response: Response<FormResponse>
                ) {
                    finishLoading()
                    surveillanceForms.clear()

                    if (response.isSuccessful && response.body()?.success == true) {
                        surveillanceForms.addAll(response.body()?.data ?: emptyList())
                    }

                    surveillanceAdapter.notifyDataSetChanged()
                    showEmpty(surveillanceForms.isEmpty())
                }

                override fun onFailure(call: Call<FormResponse>, t: Throwable) {
                    finishLoading()
                    Toast.makeText(
                        this@History_Activity,
                        "Failed to load reports: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmpty(surveillanceForms.isEmpty())
                }
            })
    }

    private fun fetchOtherForms() {
        if (isLoading) return
        isLoading = true

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        otherForms.clear()

        var completedCalls = 0
        val totalCalls = 3

        fun checkAndFinishLoading() {
            completedCalls++
            if (completedCalls == totalCalls) {
                finishLoading()
                otherFormsAdapter.notifyDataSetChanged()
                showEmpty(otherForms.isEmpty())
            }
        }

        // Fetch Immediate Reports (Annex2F)
        ApiClient.getClient(this)
            .getImmediateReport("Bearer ${SessionManager.getAccessToken(this)}")
            .enqueue(object : Callback<Annex2FResponse> {
                override fun onResponse(
                    call: Call<Annex2FResponse>,
                    response: Response<Annex2FResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.forEach {
                            otherForms.add(
                                OtherFormsData(
                                    id = it.id,
                                    type = "Annex2F Immediate Case Report",
                                    title = it.patientName ?: "Unknown",
                                    subTitle = it.disease ?: "",
                                    date = it.dateSeen ?: "",
                                    rawData = it
                                )
                            )
                        }
                    }
                    checkAndFinishLoading()
                }

                override fun onFailure(call: Call<Annex2FResponse>, t: Throwable) {
                    checkAndFinishLoading()
                }
            })

        // Fetch Specimen Reports (Annex2G Part 1)
        ApiClient.getClient(this)
            .getAnnex2GLabReports("Bearer ${SessionManager.getAccessToken(this)}")
            .enqueue(object : Callback<Annex2GResponse> {
                override fun onResponse(
                    call: Call<Annex2GResponse?>,
                    response: Response<Annex2GResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // data is now a wrapper object, results is the list
                        response.body()?.data?.results?.forEach {
                            otherForms.add(
                                OtherFormsData(
                                    id = it.id,
                                    type = "Annex2G Lab Report (Specimen)",
                                    title = it.patientNameLab ?: "Unknown",
                                    subTitle = it.suspectedDisease ?: "",
                                    date = it.dateSpecimenCollect ?: "",
                                    rawData = it
                                )
                            )
                        }
                    } else {
                        Log.e("ANNEX2G", "Failed: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                    checkAndFinishLoading()
                }

                override fun onFailure(call: Call<Annex2GResponse?>, t: Throwable) {
                    Log.e("ANNEX2G", "Error: ${t.message}")
                    checkAndFinishLoading()
                }
            })

        // Fetch Lab Results (Annex2G Part 2)
        ApiClient.getClient(this)
            .getLabReports("Bearer ${SessionManager.getAccessToken(this)}")
            .enqueue(object : Callback<LabReportResponse> {
                override fun onResponse(
                    call: Call<LabReportResponse?>,
                    response: Response<LabReportResponse?>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        response.body()?.data?.forEach {
                            otherForms.add(
                                OtherFormsData(
                                    id = it.id,
                                    type = "Lab Report (Final Result)",
                                    title = it.lab_name ?: "",
                                    subTitle = it.specimen_condition ?: "",
                                    date = it.date_lab_received ?: "",
                                    rawData = it
                                )
                            )
                        }
                    }
                    checkAndFinishLoading()
                }

                override fun onFailure(call: Call<LabReportResponse?>, t: Throwable) {
                    checkAndFinishLoading()
                }
            })
    }
}