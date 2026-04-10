package com.idsr_project.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.data.local.AppDatabase
import com.idsr_project.data.local.PendingReportEntity
import com.idsr_project.databinding.ActivitySyncStatusBinding
import com.idsr_project.sync.LabSyncWorker
import com.idsr_project.sync.SyncWorker
import com.idsr_project.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext  // ← ADD THIS

class SyncStatusActivity : BaseActivity() {

    private lateinit var binding: ActivitySyncStatusBinding
    private lateinit var adapter: ReportStatusAdapter
    private var allReports: List<PendingReportEntity> = emptyList()
    private var currentTab = 0


    private val dao by lazy { AppDatabase.getInstance(this).pendingReportDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySyncStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupTabs()
        loadReports()

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "SyncStatusActivity")

        binding.btnRetryFailed.setOnClickListener {
            SyncWorker.schedule(this)
            LabSyncWorker.schedule(this)
            binding.btnRetryFailed.text = "Retrying..."
            binding.btnRetryFailed.isEnabled = false
        }
    }

    private fun setupRecyclerView() {
        adapter = ReportStatusAdapter(emptyList()) {}
        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = adapter

        val swipeCallback = SwipeToDeleteCallback(
            context = this,
            adapter = adapter,
            onDelete = { report, position ->
                AlertDialog.Builder(this)
                    .setTitle("Delete report?")
                    .setMessage(
                        if (report.status == "SYNCED")
                            "This report is on the server. Deleting removes local history only."
                        else
                            "This failed report cannot be recovered. Delete it?"
                    )
                    .setPositiveButton("Delete") { _, _ -> deleteReport(report) }
                    .setNegativeButton("Cancel") { _, _ -> adapter.notifyItemChanged(position) }
                    .setCancelable(false)
                    .show()
            },
            onRetry = { report, position ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dao.updateStatus(report.id, "PENDING") }
                    adapter.notifyItemChanged(position)
                    SyncWorker.schedule(this@SyncStatusActivity)
                    Toast.makeText(this@SyncStatusActivity, "Retrying...", Toast.LENGTH_SHORT).show()
                    loadReports()
                }
            }
        )
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvReports)
    }

    private fun deleteReport(report: PendingReportEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { dao.deleteById(report.id) }
            Toast.makeText(this@SyncStatusActivity, "Report deleted", Toast.LENGTH_SHORT).show()
            loadReports()
        }
    }

    private fun setupTabs() {
        listOf("All", "Pending", "Synced", "Failed").forEach {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it))
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                filterAndDisplay()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun loadReports() {
        lifecycleScope.launch {
            val role     = SessionManager.getUserRole(this@SyncStatusActivity)?.uppercase() ?: ""
            val username = SessionManager.getFullName(this@SyncStatusActivity)
            val region   = SessionManager.getUserRegion(this@SyncStatusActivity) ?: ""
            val district = SessionManager.getUserDistrict(this@SyncStatusActivity) ?: ""

            allReports = withContext(Dispatchers.IO) {  // ← also move DB call to IO
                when (role) {
                    "ADMIN"            -> dao.getAllReports()
                    "REGIONAL_OFFICER" -> dao.getReportsByRegion(region)
                    "DISTRICT_OFFICER" -> dao.getReportsByDistrict(district)
                    else               -> dao.getReportsByUser(username)
                }
            }

            updateSummaryCounts()
            filterAndDisplay()
        }
    }

    private fun updateSummaryCounts() {
        val pending = allReports.count { it.status == "PENDING" }
        val synced  = allReports.count { it.status == "SYNCED"  }
        val failed  = allReports.count { it.status == "FAILED"  }

        binding.tvPendingCount.text = pending.toString()
        binding.tvSyncedCount.text  = synced.toString()
        binding.tvFailedCount.text  = failed.toString()

        binding.btnRetryFailed.visibility =
            if (failed > 0) View.VISIBLE else View.GONE
        binding.btnRetryFailed.isEnabled = true
        binding.btnRetryFailed.text = "Retry Failed Reports"
    }

    private fun filterAndDisplay() {
        val filtered = when (currentTab) {
            1    -> allReports.filter { it.status == "PENDING" }
            2    -> allReports.filter { it.status == "SYNCED"  }
            3    -> allReports.filter { it.status == "FAILED"  }
            else -> allReports
        }

        binding.tvEmpty.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.rvReports.visibility =
            if (filtered.isEmpty()) View.GONE else View.VISIBLE

        adapter.updateReports(filtered)
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }
}