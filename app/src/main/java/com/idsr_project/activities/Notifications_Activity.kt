package com.idsr_project.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.idsr_project.Adapter.NotificationsAdapter
import com.idsr_project.Model.NotificationData
import com.idsr_project.Model.NotificationsResponse
import com.idsr_project.Model.ResponseApi
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityNotificationsBinding
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Notifications_Activity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationsAdapter
    private val notifications = mutableListOf<NotificationData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets(topView = binding.appBarLayout)
        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupSwipeRefresh()
        fetchNotifications()

        binding.btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(notifications) { notification ->
            if (!notification.is_read) {
                markAsRead(notification.id)
            }
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { fetchNotifications() }
    }

    private fun fetchNotifications() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        ApiClient.getClient(this)
            .getNotifications().enqueue(object : Callback<NotificationsResponse> {
                override fun onResponse(
                    call: Call<NotificationsResponse>,
                    response: Response<NotificationsResponse>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        notifications.clear()
                        notifications.addAll(body.data)
                        adapter.notifyDataSetChanged()

                        // update unread badge
                        val unread = body.unread_count
                        if (unread > 0) {
                            binding.txtUnreadCount.visibility = View.VISIBLE
                            binding.txtUnreadCount.text = if (unread > 99) "99+" else unread.toString()
                        } else {
                            binding.txtUnreadCount.visibility = View.GONE
                        }

                        binding.btnMarkAllRead.visibility =
                            if (unread > 0) View.VISIBLE else View.GONE
                    }

                    binding.emptyStateLayout.visibility =
                        if (notifications.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        this@Notifications_Activity,
                        "Failed to load notifications",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun markAsRead(id: Int) {
        ApiClient.getClient(this)
            .markNotificationRead(id).enqueue(object : Callback<ResponseApi> {
                override fun onResponse(call: Call<ResponseApi>, response: Response<ResponseApi>) {
                    val index = notifications.indexOfFirst { it.id == id }
                    if (index != -1) {
                        notifications[index] = notifications[index].copy(is_read = true)
                        adapter.notifyItemChanged(index)
                    }
                }
                override fun onFailure(call: Call<ResponseApi>, t: Throwable) {}
            })
    }

    private fun markAllAsRead() {
        ApiClient.getClient(this)
            .markAllNotificationsRead().enqueue(object : Callback<ResponseApi> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call<ResponseApi>, response: Response<ResponseApi>) {
                    notifications.replaceAll { it.copy(is_read = true) }
                    adapter.notifyDataSetChanged()
                    binding.txtUnreadCount.visibility = View.GONE
                    binding.btnMarkAllRead.visibility = View.GONE
                }
                override fun onFailure(call: Call<ResponseApi>, t: Throwable) {}
            })
    }
}