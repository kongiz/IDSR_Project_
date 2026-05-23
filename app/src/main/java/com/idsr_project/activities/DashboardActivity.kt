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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.idsr_project.Model.*
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityDashboardBinding
import com.idsr_project.utils.SessionManager
import com.idsr_project.utils.applyWindowInsets
import retrofit2.Call
import retrofit2.Response

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private var primaryColor       = 0
    private var secondaryColor     = 0
    private var onSurfaceColor     = 0
    private var surfaceVariantColor = 0
    private var backgroundColor    = 0
    private var grayColor          = 0
    private var surfaceColor       = 0


    private val intFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float) =
            if (value == 0f) "" else value.toInt().toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets(topView = binding.appBarLayout)

        initializeThemeColors()
        binding.btnBackDashboard.setOnClickListener { finish() }
        loadAnalytics()
    }

    private fun initializeThemeColors() {
        fun resolve(attr: Int): Int {
            val tv = TypedValue()
            theme.resolveAttribute(attr, tv, true)
            return tv.data
        }
        primaryColor        = resolve(android.R.attr.colorPrimary)
        secondaryColor      = resolve(com.google.android.material.R.attr.colorSecondary)
        onSurfaceColor      = resolve(com.google.android.material.R.attr.colorOnSurface)
        surfaceVariantColor = resolve(com.google.android.material.R.attr.colorSurfaceVariant)
        backgroundColor     = resolve(android.R.attr.colorBackground)
        surfaceColor        = resolve(com.google.android.material.R.attr.colorSurface)
        grayColor           = ContextCompat.getColor(this, R.color.idsr_gray)
    }

    private fun loadAnalytics() {
        val role     = normalizeRole(SessionManager.getUserRole(this))
        val userId   = SessionManager.getUserId(this)
        val region   = SessionManager.getUserRegion(this)?.toIntOrNull()
        val district = SessionManager.getUserDistrict(this)?.toIntOrNull()

        binding.txtAnalyticsTitle.text = when (role) {
            "admin"            -> "National Analytics"
            "regional_officer" -> "Regional Analytics"
            "district_officer" -> "District Analytics"
            else               -> "Analytics Dashboard"
        }

        binding.analyticsProgress.visibility = View.VISIBLE

        ApiClient.getClient(this)
            .getAnalytics(role, userId, region, district)
            .enqueue(object : retrofit2.Callback<AnalyticResponse> {

                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<AnalyticResponse>,
                    response: Response<AnalyticResponse>
                ) {
                    binding.analyticsProgress.visibility = View.GONE

                    if (!response.isSuccessful || response.body() == null) {
                        Toast.makeText(this@DashboardActivity,
                            "Failed to load analytics", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val data = response.body()!!.data

                    binding.txtSurveillanceCount.text = data.totals.surveillance.toString()
                    binding.txtImmediateCount.text    = data.totals.immediate.toString()
                    binding.txtLabResultsCount.text   = data.totals.lab.toString()
                    binding.txtSpecimenCount.text     = data.totals.specimen.toString()

                    setupWeeklyChart(data.weekly_trend    ?: emptyList())
                    setupDiseaseChart(data.top_diseases   ?: emptyList())
                    setupGenderChart(data.gender_distribution ?: GenderDistribution(0, 0))
                    setupAgeChart(data.age_groups         ?: AgeGroups(0, 0, 0, 0, 0))
                    setupFacilityChart(data.top_facilities ?: emptyList())

                    binding.txtLabTurnaround.text =
                        "${data.lab_turnaround?.avg_delay ?: 0} days"
                }

                override fun onFailure(call: Call<AnalyticResponse>, t: Throwable) {
                    binding.analyticsProgress.visibility = View.GONE
                    Toast.makeText(this@DashboardActivity,
                        "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun setupWeeklyChart(list: List<TrendItem>) {
        if (list.isEmpty()) { binding.weeklyChart.visibility = View.GONE; return }

        val entries = list.mapIndexed { i, item -> Entry(i.toFloat(), item.total.toFloat()) }
        val labels  = list.map { "Wk ${it.epiweek}" }

        val dataset = LineDataSet(entries, "Weekly Cases").apply {
            color            = primaryColor
            circleRadius     = 5f
            setCircleColor(primaryColor)
            circleHoleColor  = surfaceColor
            valueTextColor   = onSurfaceColor
            valueTextSize    = 10f
            valueFormatter   = intFormatter
            lineWidth        = 2.5f
            mode             = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor        = primaryColor
            fillAlpha        = 35
        }

        binding.weeklyChart.apply {
            setBackgroundColor(surfaceColor)
            data             = LineData(dataset)
            description.isEnabled = false
            axisRight.isEnabled   = false
            setDrawBorders(false)

            xAxis.apply {
                valueFormatter    = IndexAxisValueFormatter(labels)
                position          = XAxis.XAxisPosition.BOTTOM
                granularity       = 1f
                textColor         = grayColor
                textSize          = 10f
                labelRotationAngle = if (labels.size > 5) -30f else 0f
                setDrawGridLines(false)
                setDrawAxisLine(false)

                labelCount        = minOf(labels.size, 7)
            }

            axisLeft.apply {
                textColor        = grayColor
                gridColor        = surfaceVariantColor
                setDrawAxisLine(false)
                axisMinimum      = 0f
                granularity      = 1f
                valueFormatter   = intFormatter
            }

            legend.apply {
                textColor = onSurfaceColor
                textSize  = 12f
            }

            setExtraOffsets(8f, 8f, 8f, 16f)
            setTouchEnabled(true)
            setPinchZoom(false)
            animateY(800)
            invalidate()
        }
    }


    private fun setupDiseaseChart(list: List<DiseaseItem>) {
        if (list.isEmpty()) { binding.diseaseChart.visibility = View.GONE; return }

        val entries = list.mapIndexed { i, item -> BarEntry(i.toFloat(), item.total.toFloat()) }
        val labels  = list.map { truncate(it.disease_name, 14) }

        val dataset = BarDataSet(entries, "Diseases").apply {
            colors         = gradientColors(primaryColor, list.size)
            valueTextColor = onSurfaceColor
            valueTextSize  = 10f
            valueFormatter = intFormatter
        }

        val barWidthDp  = 70
        val chartWidth  = maxChartWidth(list.size, barWidthDp)

        binding.diseaseChart.apply {
            layoutParams = layoutParams.apply { width = chartWidth }
            setBackgroundColor(surfaceColor)
            data = BarData(dataset).apply { barWidth = 0.55f }
            styleBarChart(this, labels, bottomOffsetDp = 48f)
            animateY(800)
            invalidate()
        }
    }


    private fun setupGenderChart(gender: GenderDistribution) {
        val total = gender.male + gender.female
        if (total == 0) { binding.genderChart.visibility = View.GONE; return }

        val entries = listOf(
            PieEntry(gender.male.toFloat(),   "Male"),
            PieEntry(gender.female.toFloat(), "Female")
        )

        val dataset = PieDataSet(entries, "").apply {
            colors         = listOf(primaryColor, secondaryColor)
            valueTextColor = Color.WHITE
            valueTextSize  = 13f
            sliceSpace     = 3f
            setDrawValues(true)
        }

        binding.genderChart.apply {
            setBackgroundColor(surfaceColor)
            data                 = PieData(dataset)
            setUsePercentValues(true)
            description.isEnabled = false
            centerText           = "Gender\nDistribution"
            setCenterTextColor(onSurfaceColor)
            setCenterTextSize(13f)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            isDrawHoleEnabled    = true
            setHoleColor(surfaceColor)
            holeRadius           = 44f
            transparentCircleRadius = 50f
            legend.apply {
                textColor = onSurfaceColor
                textSize  = 12f
            }
            setExtraOffsets(8f, 8f, 8f, 8f)
            animateY(1000)
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
        val labels = listOf("<5", "6-15", "16-30", "31-60", "60+")

        val dataset = BarDataSet(entries, "Age Groups").apply {
            colors         = gradientColors(primaryColor, 5)
            valueTextColor = onSurfaceColor
            valueTextSize  = 10f
            valueFormatter = intFormatter
        }

        val chartWidth = maxChartWidth(5, 80)

        binding.ageChart.apply {
            layoutParams = layoutParams.apply { width = chartWidth }
            setBackgroundColor(surfaceColor)
            data = BarData(dataset).apply { barWidth = 0.55f }
            styleBarChart(this, labels, bottomOffsetDp = 20f)
            animateY(800)
            invalidate()
        }
    }


    private fun setupFacilityChart(facList: List<FacilityItem>) {
        if (facList.isEmpty()) { binding.facilityChart.visibility = View.GONE; return }

        val entries = facList.mapIndexed { i, item -> BarEntry(i.toFloat(), item.total.toFloat()) }
        val labels  = facList.map { truncate(it.facility_name, 12) }

        val dataset = BarDataSet(entries, "Facilities").apply {
            colors         = gradientColors(secondaryColor, facList.size)
            valueTextColor = onSurfaceColor
            valueTextSize  = 10f
            valueFormatter = intFormatter
        }

        val chartWidth = maxChartWidth(facList.size, 90)

        binding.facilityChart.apply {
            layoutParams = layoutParams.apply { width = chartWidth }
            setBackgroundColor(surfaceColor)
            data = BarData(dataset).apply { barWidth = 0.55f }
            styleBarChart(this, labels, bottomOffsetDp = 52f)
            animateY(800)
            invalidate()
        }
    }


    private fun styleBarChart(chart: BarChart, labels: List<String>, bottomOffsetDp: Float) {
        chart.apply {
            description.isEnabled = false
            axisRight.isEnabled   = false
            setDrawBorders(false)
            setFitBars(true)

            xAxis.apply {
                valueFormatter     = IndexAxisValueFormatter(labels)
                position           = XAxis.XAxisPosition.BOTTOM
                granularity        = 1f
                textColor          = grayColor
                textSize           = 10f
                labelRotationAngle = -40f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setCenterAxisLabels(false)
            }

            axisLeft.apply {
                textColor      = grayColor
                gridColor      = surfaceVariantColor
                setDrawAxisLine(false)
                axisMinimum    = 0f
                granularity    = 1f
                valueFormatter = intFormatter
            }

            legend.apply {
                textColor = onSurfaceColor
                textSize  = 11f
            }

            setExtraOffsets(8f, 8f, 8f, bottomOffsetDp)
            setTouchEnabled(true)
            setPinchZoom(false)
        }
    }


    private fun maxChartWidth(itemCount: Int, widthPerItemDp: Int): Int {
        val density       = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels
        val calculated    = (itemCount * widthPerItemDp * density).toInt()
        return maxOf(calculated, screenWidthPx)
    }

    private fun truncate(text: String, maxLen: Int) =
        if (text.length <= maxLen) text else text.take(maxLen - 1) + "…"

    private fun gradientColors(baseColor: Int, count: Int): List<Int> {
        if (count == 1) return listOf(baseColor)
        val r = Color.red(baseColor)
        val g = Color.green(baseColor)
        val b = Color.blue(baseColor)
        return (0 until count).map { i ->
            val f = 1f - i * (0.35f / (count - 1))
            Color.rgb((r * f).toInt().coerceIn(0, 255),
                (g * f).toInt().coerceIn(0, 255),
                (b * f).toInt().coerceIn(0, 255))
        }
    }

    private fun normalizeRole(role: String?) = when (role?.trim()?.lowercase()) {
        "admin"                                        -> "admin"
        "regional officer", "regional_officer"         -> "regional_officer"
        "district officer", "district_officer"         -> "district_officer"
        else                                           -> "user"
    }
}