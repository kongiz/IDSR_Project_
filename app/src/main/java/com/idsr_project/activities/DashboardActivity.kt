package com.idsr_project.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.idsr_project.Model.*
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityDashboardBinding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Response
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val idsrBlue = "#0D47A1".toColorInt()
    private val idsrLightBlue = "#1976D2".toColorInt()
    private val idsrAccent = "#42A5F5".toColorInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackDashboard.setOnClickListener { finish() }

        loadAnalytics()
    }


    private fun loadAnalytics() {

        val role = normalizeRole(SessionManager.getUserRole(this))
        val userId = SessionManager.getUserId(this)
        val region = SessionManager.getUserRegion(this)?.toIntOrNull()
        val district = SessionManager.getUserDistrict(this)?.toIntOrNull()

        ApiClient.getClient(this)
            .getAnalytics(role, userId, region, district)
            .enqueue(object : retrofit2.Callback<AnalyticResponse> {

                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<AnalyticResponse>,
                    response: Response<AnalyticResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) return

                    val data = response.body()!!


                    binding.txtSurveillanceCount.text = data.total_surveillance.toString()
                    binding.txtImmediateCount.text = data.total_immediate.toString()
                    binding.txtLabResultsCount.text = data.total_lab_results.toString()
                    binding.txtSpecimenCount.text = data.total_specimen.toString()

                    setupWeeklyChart(data.weekly_trend)
                    setupDiseaseChart(data.top_diseases)
                    setupGenderChart(data.gender_distribution)
                    setupAgeChart(data.age_groups)
                    setupFacilityChart(data.top_facilities)


                    binding.txtLabTurnaround.text =
                        "${data.lab_turnaround.avg_delay ?: 0} days"
                }

                override fun onFailure(call: Call<AnalyticResponse>, t: Throwable) {
                    Toast.makeText(this@DashboardActivity, t.message, Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun setupWeeklyChart(list: List<TrendItem>) {

        val entries = list.mapIndexed { index, item ->
            Entry(index.toFloat(), item.total.toFloat())
        }

        val labels = list.map { "Wk ${it.epiweek}" }

        val dataset = LineDataSet(entries, "Weekly Cases").apply {
            color = idsrBlue
            circleRadius = 5f
            setCircleColor(idsrAccent)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            lineWidth = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.weeklyChart.apply {
            data = LineData(dataset)

            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            axisLeft.textColor = idsrBlue
            legend.textColor = idsrBlue

            animateY(1000)
            invalidate()
        }
    }


    private fun setupDiseaseChart(list: List<DiseaseItem>) {

        val entries = list.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.total.toFloat())
        }

        val labels = list.map { it.disease_name }

        val dataset = BarDataSet(entries, "Diseases").apply {
            colors = listOf(idsrBlue, idsrLightBlue, idsrAccent)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        binding.diseaseChart.apply {
            data = BarData(dataset)

            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            axisLeft.textColor = idsrBlue
            legend.textColor = idsrBlue

            animateY(900)
            invalidate()
        }
        autoResizeChart(binding.diseaseChart, list.size, 90)
    }


    private fun setupGenderChart(gender: GenderDistribution) {

        val entries = listOf(
            PieEntry(gender.male.toFloat(), "Male"),
            PieEntry(gender.female.toFloat(), "Female")
        )

        val dataset = PieDataSet(entries, "").apply {
            colors = listOf(idsrBlue, idsrAccent)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            sliceSpace = 3f
        }

        binding.genderChart.apply {
            data = PieData(dataset)

            setUsePercentValues(true)
            description.isEnabled = false
            centerText = "Gender"
            setCenterTextColor(idsrBlue)
            animateY(1500)

            invalidate()
        }
    }


    private fun setupAgeChart(age: AgeGroups) {

        val entries = listOf(
            BarEntry(0f, age.u5.toFloat()),
            BarEntry(1f, age.g6_15.toFloat()),
            BarEntry(2f, age.g16_30.toFloat()),
            BarEntry(3f, age.g31_60.toFloat()),
            BarEntry(4f, age.above60.toFloat())
        )

        val labels = listOf("0–5", "6–15", "16–30", "31–60", "60+")

        val dataset = BarDataSet(entries, "Age Groups").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        binding.ageChart.apply {
            data = BarData(dataset)

            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            axisLeft.textColor = idsrBlue
            legend.textColor = idsrBlue

            animateY(1000)
            invalidate()
        }
        autoResizeChart(binding.ageChart, 5, 100)
    }

    private fun setupFacilityChart(facList: List<FacilityItem>) {

        val entries = facList.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.total.toFloat())
        }

        val labels = facList.map { it.health_facility }

        val dataSet = BarDataSet(entries, "Facilities").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }

        binding.facilityChart.apply {
            data = BarData(dataSet)

            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            axisLeft.textColor = idsrBlue
            legend.textColor = idsrBlue

            animateY(1000)
            invalidate()
        }
        autoResizeChart(binding.facilityChart, facList.size, 120)

    }
    private fun setXAxisLabels(xAxis: XAxis, labels: List<String>) {
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 12f
        xAxis.textColor = idsrBlue
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
    }

    private fun normalizeRole(role: String?): String {
        return when (role?.trim()?.lowercase()) {
            "admin" -> "admin"
            "regional officer", "regional", "regional_officer" -> "regional_officer"
            "district officer", "district", "district_officer" -> "district_officer"
            else -> "user"
        }
    }
    private fun autoResizeChart(
        chart: View,
        itemCount: Int,
        widthPerItemDp: Int = 80
    ) {
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels

        val calculatedWidthPx = (itemCount * widthPerItemDp * density).toInt()

        val finalWidth = maxOf(calculatedWidthPx, screenWidthPx)

        chart.layoutParams = chart.layoutParams.apply {
            width = finalWidth
        }
    }
//    For chart to be more readable.
//    chart.setExtraOffsets(10f, 10f, 10f, 20f)
//    val bottomOffset = if (labels.any { it.length > 12 }) 40f else 20f
//    chart.setExtraOffsets(10f, 10f, 10f, bottomOffset)


}
