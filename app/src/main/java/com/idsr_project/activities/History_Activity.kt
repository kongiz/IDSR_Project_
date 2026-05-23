package com.idsr_project.activities

import FormAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Adapter.OtherFormsAdapter
import com.idsr_project.Model.*
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityHistoryBinding
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class History_Activity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var surveillanceAdapter: FormAdapter
    private lateinit var otherFormsAdapter: OtherFormsAdapter

    private val surveillanceForms = mutableListOf<FormData>()
    private val otherForms        = mutableListOf<OtherFormsData>()

    private var activeTab = TAB_SURVEILLANCE
    private var isLoading = false

    private var survPage       = 1;  private var survTotalPages  = 1
    private var otherPage      = 1;  private var otherTotalPages = 1
    private var currentSearch  = ""


    private val searchHandler  = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    companion object {
        private const val TAB_SURVEILLANCE = "surveillance"
        private const val TAB_OTHER        = "other"
        private const val PAGE_SIZE        = 20
        private const val SEARCH_DELAY_MS  = 300L
    }
    private val surveillanceHints = listOf(
        "Search by region...",
        "Search by district...",
        "Search by facility...",
        "Search by reporter name...",
        "Search by epi week..."
    )

    private val otherFormsHints = listOf(
        "Search by patient name...",
        "Search by disease...",
        "Search by lab name...",
        "Search by specimen type...",
        "Search by reporter name..."
    )
    private var currentHintIndex = 0
    private val hintHandler      = Handler(Looper.getMainLooper())
    private var hintRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(topView = binding.appBarLayout)
        binding.btnBack.setOnClickListener { finish() }

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "History_Activity")


        setupRecyclerViews()
        setupTabs()
        setupSwipeRefresh()
        setupSearch()
        setupRoleLabels()

        setActiveTab(TAB_SURVEILLANCE)
        fetchSurveillance(reset = true)
    }

    private fun setupRoleLabels() {
        val role = SessionManager.getUserRole(this) ?: "Health Officer"
        when (role) {
            "District Officer" -> {
                binding.btnSurveillance.text = "District Surveillance"
                binding.btnOtherForms.text   = "District Forms"
            }
            "Regional Officer" -> {
                binding.btnSurveillance.text = "Regional Surveillance"
                binding.btnOtherForms.text   = "Regional Forms"
            }
            "Admin" -> {
                binding.btnSurveillance.text = "All Surveillance"
                binding.btnOtherForms.text   = "All Forms"
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""


            searchRunnable?.let { searchHandler.removeCallbacks(it) }

            searchRunnable = Runnable {
                if (query != currentSearch) {
                    currentSearch = query
                    if (activeTab == TAB_SURVEILLANCE) {
                        fetchSurveillance(reset = true)
                    } else {
                        fetchOtherForms(reset = true)
                    }
                }
            }
            searchHandler.postDelayed(searchRunnable!!, SEARCH_DELAY_MS)
        }
        startHintAnimation()
    }

    private fun setupInfiniteScroll(recyclerView: RecyclerView, isSurveillance: Boolean) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || isLoading) return
                val layoutManager = rv.layoutManager as LinearLayoutManager
                val visibleItemCount    = layoutManager.childCount
                val totalItemCount      = layoutManager.itemCount
                val firstVisibleItemPos = layoutManager.findFirstVisibleItemPosition()

                val shouldLoadMore = (visibleItemCount + firstVisibleItemPos) >= totalItemCount - 3

                if (shouldLoadMore) {
                    if (isSurveillance && survPage < survTotalPages) {
                        fetchSurveillance(reset = false)
                    } else if (!isSurveillance && otherPage < otherTotalPages) {
                        fetchOtherForms(reset = false)
                    }
                }
            }
        })
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

        setupInfiniteScroll(binding.rvSurveillanceForms, isSurveillance = true)
        setupInfiniteScroll(binding.rvOtherForms,        isSurveillance = false)
    }

    private fun setupTabs() {
        binding.btnSurveillance.setOnClickListener {
            if (activeTab != TAB_SURVEILLANCE) {
                currentHintIndex = 0
                currentSearch = ""
                binding.etSearch.setText("")
                setActiveTab(TAB_SURVEILLANCE)
                fetchSurveillance(reset = true)
            }
        }
        binding.btnOtherForms.setOnClickListener {
            if (activeTab != TAB_OTHER) {
                currentHintIndex = 0
                currentSearch = ""
                binding.etSearch.setText("")
                setActiveTab(TAB_OTHER)
                fetchOtherForms(reset = true)
            }
        }
    }

    private fun setupSwipeRefresh() {
        val primaryColor = MaterialColors.getColor(binding.root, android.R.attr.colorPrimary)
        binding.swipeRefresh.setColorSchemeColors(primaryColor)
        binding.swipeRefresh.setOnRefreshListener {
            currentSearch = ""
            binding.etSearch.setText("")
            if (activeTab == TAB_SURVEILLANCE) {
                fetchSurveillance(reset = true)
            } else {
                fetchOtherForms(reset = true)
            }
        }
    }

    private fun setActiveTab(tab: String) {
        activeTab = tab
        val primaryColor        = MaterialColors.getColor(binding.root, android.R.attr.colorPrimary)
        val onPrimaryColor      = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimary)
        val surfaceVariantColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceVariant)
        val onSurfaceColor      = MaterialColors.getColor(binding.root, android.R.attr.textColor)

        if (tab == TAB_SURVEILLANCE) {
            binding.btnSurveillance.setBackgroundColor(primaryColor)
            binding.btnSurveillance.setTextColor(onPrimaryColor)
            binding.btnOtherForms.setBackgroundColor(surfaceVariantColor)
            binding.btnOtherForms.setTextColor(onSurfaceColor)
            binding.rvSurveillanceForms.visibility = View.VISIBLE
            binding.rvOtherForms.visibility        = View.GONE
        } else {
            binding.btnOtherForms.setBackgroundColor(primaryColor)
            binding.btnOtherForms.setTextColor(onPrimaryColor)
            binding.btnSurveillance.setBackgroundColor(surfaceVariantColor)
            binding.btnSurveillance.setTextColor(onSurfaceColor)
            binding.rvSurveillanceForms.visibility = View.GONE
            binding.rvOtherForms.visibility        = View.VISIBLE
        }
        binding.emptyStateLayout.visibility = View.GONE
    }

    private fun showLoadMoreProgress(show: Boolean) {
        binding.loadMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun finishLoading() {
        isLoading = false
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
        showLoadMoreProgress(false)
    }

    private fun showEmpty(show: Boolean) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.txtEmptyMessage.text = if (currentSearch.isNotEmpty())
                "No results for \"$currentSearch\""
            else if (activeTab == TAB_SURVEILLANCE)
                "No surveillance reports found"
            else
                "No other forms found"
        }
    }

    // ── Fetch surveillance ───
    @SuppressLint("NotifyDataSetChanged")
    private fun fetchSurveillance(reset: Boolean) {
        if (isLoading) return
        isLoading = true

        if (reset) {
            survPage = 1
            surveillanceForms.clear()
            surveillanceAdapter.notifyDataSetChanged()
            binding.progressBar.visibility = View.VISIBLE
        } else {
            showLoadMoreProgress(true)
        }

        binding.emptyStateLayout.visibility = View.GONE

        val search = currentSearch.ifEmpty { null }

        ApiClient.getClient(this)
            .getSurveillanceReport(page = survPage, limit = PAGE_SIZE, search = search)
            .enqueue(object : Callback<FormResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call<FormResponse>, response: Response<FormResponse>) {
                    finishLoading()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body       = response.body()!!
                        survTotalPages = body.total_pages ?: 1
                        val newItems   = body.data ?: emptyList()
                        surveillanceForms.addAll(newItems)
                        surveillanceAdapter.notifyDataSetChanged()
                        if (!reset && newItems.isNotEmpty()) survPage++
                        if (reset && newItems.isNotEmpty()) survPage = 2
                    }
                    showEmpty(surveillanceForms.isEmpty())
                }

                override fun onFailure(call: Call<FormResponse>, t: Throwable) {
                    finishLoading()
                    FirebaseCrashlytics.getInstance().recordException(t)
                    Toast.makeText(this@History_Activity, "Failed to load reports", Toast.LENGTH_SHORT).show()
                    showEmpty(surveillanceForms.isEmpty())
                }
            })
    }

    // ── Fetch other forms (Annex2F + Annex2G + Lab) ───────────────
    @SuppressLint("NotifyDataSetChanged")
    private fun fetchOtherForms(reset: Boolean) {
        if (isLoading) return
        isLoading = true

        if (reset) {
            otherPage = 1
            otherForms.clear()
            otherFormsAdapter.notifyDataSetChanged()
            binding.progressBar.visibility = View.VISIBLE
        } else {
            showLoadMoreProgress(true)
        }

        binding.emptyStateLayout.visibility = View.GONE

        val search        = currentSearch.ifEmpty { null }
        var completedCalls = 0
        val totalCalls     = 3

        @SuppressLint("NotifyDataSetChanged")
        fun checkDone() {
            completedCalls++
            if (completedCalls == totalCalls) {
                otherForms.sortByDescending { it.id }
                finishLoading()
                otherFormsAdapter.notifyDataSetChanged()
                showEmpty(otherForms.isEmpty())
                if (reset) otherPage = 2
            }
        }

        // Annex2F
        ApiClient.getClient(this)
            .getImmediateReport(page = otherPage, limit = PAGE_SIZE, search = search)
            .enqueue(object : Callback<Annex2FResponse> {
                override fun onResponse(call: Call<Annex2FResponse>, response: Response<Annex2FResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.forEach {
                            otherForms.add(OtherFormsData(
                                id       = it.id,
                                type     = "Annex2F Immediate Case Report",
                                title    = it.patientName ?: "Unknown",
                                subTitle = it.disease ?: "",
                                date     = it.dateSeen ?: "",
                                rawData  = it
                            ))
                        }
                    }
                    checkDone()
                }
                override fun onFailure(call: Call<Annex2FResponse>, t: Throwable) { checkDone() }
            })

        // Annex2G
        ApiClient.getClient(this)
            .getAnnex2GLabReports(page = otherPage, limit = PAGE_SIZE, search = search)
            .enqueue(object : Callback<Annex2GResponse> {
                override fun onResponse(call: Call<Annex2GResponse?>, response: Response<Annex2GResponse?>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.results?.forEach {
                            otherForms.add(OtherFormsData(
                                id       = it.id,
                                type     = "Annex2G Lab Report (Specimen)",
                                title    = it.patientNameLab ?: "Unknown",
                                subTitle = it.suspectedDisease ?: "",
                                date     = it.dateSpecimenCollect ?: "",
                                rawData  = it
                            ))
                        }
                    }
                    checkDone()
                }
                override fun onFailure(call: Call<Annex2GResponse?>, t: Throwable) { checkDone() }
            })

        // Lab Reports
        ApiClient.getClient(this)
            .getLabReports(page = otherPage, limit = PAGE_SIZE, search = search)
            .enqueue(object : Callback<LabReportResponse> {
                override fun onResponse(call: Call<LabReportResponse?>, response: Response<LabReportResponse?>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        response.body()?.data?.forEach {
                            otherForms.add(OtherFormsData(
                                id       = it.id,
                                type     = "Lab Report (Final Result)",
                                title    = it.lab_name ?: "",
                                subTitle = it.specimen_condition ?: "",
                                date     = it.date_lab_received ?: "",
                                rawData  = it
                            ))
                        }
                    }
                    checkDone()
                }
                override fun onFailure(call: Call<LabReportResponse?>, t: Throwable) { checkDone() }
            })
    }
    override fun onStart() {
        super.onStart()
        if (binding.etSearch.text.isNullOrEmpty() && !binding.etSearch.hasFocus()) {
            startHintAnimation()
        }
    }

    override fun onStop() {
        super.onStop()
        stopHintAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        stopHintAnimation()
    }

    private fun startHintAnimation() {
        hintRunnable = object : Runnable {
            override fun run() {
                if (binding.etSearch.text.isNullOrEmpty() && !binding.etSearch.hasFocus()) {
                    val hints = if (activeTab == TAB_SURVEILLANCE)
                        surveillanceHints else otherFormsHints

                    val nextHint = hints[currentHintIndex % hints.size]

                    binding.searchLayout.animate()
                        .alpha(0.3f)
                        .setDuration(300)
                        .withEndAction {
                            binding.etSearch.hint = nextHint
                            binding.searchLayout.animate()
                                .alpha(1f)
                                .setDuration(300)
                                .start()
                        }
                        .start()

                    currentHintIndex++
                }
                hintHandler.postDelayed(this, 2500)
            }
        }
        hintHandler.postDelayed(hintRunnable!!, 2500)
    }
    private fun stopHintAnimation() {
        hintRunnable?.let { hintHandler.removeCallbacks(it) }
    }
}