package com.idsr_project.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.idsr_project.Model.*
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityDashboardBinding
import com.idsr_project.utils.SessionManager
import retrofit2.Call
import retrofit2.Response
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding


    private var primaryColor: Int = 0
    private var secondaryColor: Int = 0
    private var onSurfaceColor: Int = 0
    private var surfaceVariantColor: Int = 0
    private var backgroundColor: Int = 0
    private var grayColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initializeThemeColors()

        binding.btnBackDashboard.setOnClickListener { finish() }

        loadAnalytics()
    }

    private fun initializeThemeColors() {

        primaryColor = getThemeColor(android.R.attr.colorPrimary)

        secondaryColor = getThemeColor(com.google.android.material.R.attr.colorSecondary)


        onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)


        surfaceVariantColor = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant)


        backgroundColor = getThemeColor(android.R.attr.colorBackground)


        grayColor = ContextCompat.getColor(this, R.color.idsr_gray)
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
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
                    if (!response.isSuccessful || response.body() == null) {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Failed to load analytics",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val data = response.body()!!.data

                    binding.txtSurveillanceCount.text = data.totals.surveillance.toString()
                    binding.txtImmediateCount.text     = data.totals.immediate.toString()
                    binding.txtLabResultsCount.text    = data.totals.lab.toString()
                    binding.txtSpecimenCount.text      = data.totals.specimen.toString()

                    setupWeeklyChart(data.weekly_trend ?: emptyList())
                    setupDiseaseChart(data.top_diseases ?: emptyList())
                    setupGenderChart(data.gender_distribution ?: GenderDistribution(0, 0))
                    setupAgeChart(data.age_groups ?: AgeGroups(0, 0, 0, 0, 0))
                    setupFacilityChart(data.top_facilities ?: emptyList())

                    binding.txtLabTurnaround.text = "${data.lab_turnaround?.avg_delay ?: 0} days"
                }

                override fun onFailure(call: Call<AnalyticResponse>, t: Throwable) {
                    Toast.makeText(
                        this@DashboardActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun setupWeeklyChart(list: List<TrendItem>) {
        if (list.isEmpty()) {
            binding.weeklyChart.visibility = View.GONE
            return
        }

        val entries = list.mapIndexed { index, item ->
            Entry(index.toFloat(), item.total.toFloat())
        }

        val labels = list.map { "Wk ${it.epiweek}" }


        val dataset = LineDataSet(entries, "Weekly Cases").apply {
            color = primaryColor // Line color
            circleRadius = 5f
            setCircleColor(primaryColor) // Circle outline
            circleHoleColor = backgroundColor // Circle fill (matches background)
            valueTextColor = onSurfaceColor // Value labels
            valueTextSize = 11f
            lineWidth = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = primaryColor
            fillAlpha = 40 // Slightly more visible fill
        }

        binding.weeklyChart.apply {
            data = LineData(dataset)
            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            // FIX 3: Y-axis styling with proper colors
            axisLeft.apply {
                textColor = grayColor // Gray for axis labels
                gridColor = surfaceVariantColor // Subtle grid lines
                setDrawAxisLine(false)
                axisMinimum = 0f // Start from 0
            }

            legend.apply {
                textColor = onSurfaceColor
                textSize = 12f
            }

            setExtraOffsets(10f, 10f, 10f, 20f)
            setTouchEnabled(true)
            setPinchZoom(false)

            animateY(1000)
            invalidate()
        }
    }

    private fun setupDiseaseChart(list: List<DiseaseItem>) {
        if (list.isEmpty()) {
            binding.diseaseChart.visibility = View.GONE
            return
        }

        val entries = list.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.total.toFloat())
        }

        val labels = list.map { it.disease_name }

        // FIX 4: Create gradient colors from primary color
        val colors = createGradientColors(primaryColor, list.size)

        val dataset = BarDataSet(entries, "Diseases").apply {
            this.colors = colors
            valueTextColor = onSurfaceColor
            valueTextSize = 11f
            setDrawValues(true)
        }

        binding.diseaseChart.apply {
            data = BarData(dataset).apply {
                barWidth = 0.7f // Slightly thinner bars for better spacing
            }
            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            axisLeft.apply {
                textColor = grayColor
                gridColor = surfaceVariantColor
                setDrawAxisLine(false)
                axisMinimum = 0f
            }

            legend.apply {
                textColor = onSurfaceColor
                textSize = 12f
            }

            setExtraOffsets(10f, 10f, 10f, 40f)
            setTouchEnabled(true)
            setPinchZoom(false)

            animateY(900)
            invalidate()
        }

        autoResizeChart(binding.diseaseChart, list.size, 90)
    }

    private fun setupGenderChart(gender: GenderDistribution) {
        val total = gender.male + gender.female

        if (total == 0) {
            binding.genderChart.visibility = View.GONE
            return
        }

        val entries = listOf(
            PieEntry(gender.male.toFloat(), "Male"),
            PieEntry(gender.female.toFloat(), "Female")
        )

        // FIX 5: Use primary and secondary colors for gender
        val dataset = PieDataSet(entries, "").apply {
            colors = listOf(primaryColor, secondaryColor)
            valueTextColor = Color.WHITE // White text on colored slices
            valueTextSize = 14f
            sliceSpace = 3f
            valueLineColor = onSurfaceColor
            setDrawValues(true)
        }

        binding.genderChart.apply {
            data = PieData(dataset)
            setUsePercentValues(true)
            description.isEnabled = false

            // FIX 6: Center text with proper color
            centerText = "Gender\nDistribution"
            setCenterTextColor(onSurfaceColor)
            setCenterTextSize(14f)

            // Labels on slices
            setEntryLabelColor(Color.WHITE) // White labels on colored slices
            setEntryLabelTextSize(12f)

            // Transparent hole in center
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 45f
            setTransparentCircleColor(onSurfaceColor)
            transparentCircleRadius = 50f

            legend.apply {
                textColor = onSurfaceColor
                textSize = 12f
            }

            setExtraOffsets(10f, 10f, 10f, 10f)
            setTouchEnabled(true)

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

        // FIX 7: Create 5 gradient colors from primary
        val colors = createGradientColors(primaryColor, 5)

        val dataset = BarDataSet(entries, "Age Groups").apply {
            this.colors = colors
            valueTextColor = onSurfaceColor
            valueTextSize = 11f
        }

        binding.ageChart.apply {
            data = BarData(dataset).apply {
                barWidth = 0.7f
            }
            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            axisLeft.apply {
                textColor = grayColor
                gridColor = surfaceVariantColor
                setDrawAxisLine(false)
                axisMinimum = 0f
            }

            legend.apply {
                textColor = onSurfaceColor
                textSize = 12f
            }

            setExtraOffsets(10f, 10f, 10f, 20f)
            setTouchEnabled(true)
            setPinchZoom(false)

            animateY(1000)
            invalidate()
        }

        autoResizeChart(binding.ageChart, 5, 100)
    }

    private fun setupFacilityChart(facList: List<FacilityItem>) {
        if (facList.isEmpty()) {
            binding.facilityChart.visibility = View.GONE
            return
        }

        val entries = facList.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.total.toFloat())
        }

        val labels = facList.map { it.facility_name }

        // FIX 8: Use secondary color for facilities (cyan tones)
        val colors = createGradientColors(secondaryColor, facList.size)

        val dataSet = BarDataSet(entries, "Facilities").apply {
            this.colors = colors
            valueTextColor = onSurfaceColor
            valueTextSize = 11f
        }

        binding.facilityChart.apply {
            data = BarData(dataSet).apply {
                barWidth = 0.7f
            }
            description.isEnabled = false
            axisRight.isEnabled = false

            setXAxisLabels(xAxis, labels)

            axisLeft.apply {
                textColor = grayColor
                gridColor = surfaceVariantColor
                setDrawAxisLine(false)
                axisMinimum = 0f
            }

            legend.apply {
                textColor = onSurfaceColor
                textSize = 12f
            }

            // Extra bottom padding for long facility names
            val bottomOffset = if (labels.any { it.length > 12 }) 60f else 40f
            setExtraOffsets(10f, 10f, 10f, bottomOffset)
            setTouchEnabled(true)
            setPinchZoom(false)

            animateY(1000)
            invalidate()
        }

        autoResizeChart(binding.facilityChart, facList.size, 120)
    }

    private fun setXAxisLabels(xAxis: XAxis, labels: List<String>) {
        xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            textSize = 10f
            textColor = grayColor // Gray for X-axis labels
            granularity = 1f
            labelRotationAngle = -45f // Rotated for long labels
            setDrawAxisLine(false)
            setDrawGridLines(false)
        }
    }

    // FIX 9: Improved gradient generation with better contrast
    private fun createGradientColors(baseColor: Int, count: Int): List<Int> {
        if (count <= 1) return listOf(baseColor)

        val colors = mutableListOf<Int>()
        val red = Color.red(baseColor)
        val green = Color.green(baseColor)
        val blue = Color.blue(baseColor)

        for (i in 0 until count) {
            // Create gradient from base color to 40% darker
            // This provides better visual distinction between bars
            val factor = 1f - (i * (0.4f / (count - 1)))
            colors.add(
                Color.rgb(
                    (red * factor).toInt().coerceIn(0, 255),
                    (green * factor).toInt().coerceIn(0, 255),
                    (blue * factor).toInt().coerceIn(0, 255)
                )
            )
        }

        return colors
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
}