package com.idsr_project.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.MapPoint
import com.idsr_project.Model.MapPointsResponse
import com.idsr_project.R
import com.idsr_project.api.ApiClient
import com.idsr_project.databinding.ActivityDiseaseMapBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DiseaseMapActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDiseaseMapBinding
    private lateinit var map: GoogleMap


    private var allSurveillancePoints = listOf<MapPoint>()
    private var allAnnexPoints        = listOf<MapPoint>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDiseaseMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackMap.setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        FirebaseCrashlytics.getInstance().setCustomKey("screen", "DiseaseMapActivity")
    }



    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap


        map.uiSettings.apply {
            isZoomControlsEnabled    = true
            isCompassEnabled         = true
            isMyLocationButtonEnabled = false
        }


        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        setupChipFilters()
        fetchAndPlotPoints()


        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(13.4432, -15.3101), 8f)
        )
    }


    private fun setupChipFilters() {
        listOf(
            binding.chipAll,
            binding.chipSurveillance,
            binding.chipAnnex2F,
            binding.chipAlive,
            binding.chipDead
        ).forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ -> applyFilters() }
        }
    }

    private fun applyFilters() {
        map.clear()

        val showAll          = binding.chipAll.isChecked
        val showSurveillance = showAll || binding.chipSurveillance.isChecked
        val showAnnex        = showAll || binding.chipAnnex2F.isChecked
        val showAlive        = showAll || binding.chipAlive.isChecked
        val showDead         = showAll || binding.chipDead.isChecked

        if (showSurveillance) {
            plotSurveillancePoints(allSurveillancePoints)
        }

        if (showAnnex) {
            val filtered = allAnnexPoints.filter { point ->
                when (point.outcome?.lowercase()) {
                    "alive" -> showAlive
                    "dead"  -> showDead
                    else    -> true
                }
            }
            plotAnnexPoints(filtered)
        }
    }


    private fun fetchAndPlotPoints() {
        binding.loadingOverlay.visibility = View.VISIBLE

        ApiClient.getClient(this).getMapPoints()
            .enqueue(object : Callback<MapPointsResponse> {
                override fun onResponse(
                    call: Call<MapPointsResponse>,
                    response: Response<MapPointsResponse>
                ) {
                    binding.loadingOverlay.visibility = View.GONE

                    if (!response.isSuccessful || response.body()?.data == null) {
                        Toast.makeText(
                            this@DiseaseMapActivity,
                            "Failed to load map data",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val data              = response.body()!!.data
                    allSurveillancePoints = data.surveillance
                    allAnnexPoints        = data.annex2f

                    plotSurveillancePoints(allSurveillancePoints)
                    plotAnnexPoints(allAnnexPoints)
                    updateLegendCounts()
                }

                override fun onFailure(call: Call<MapPointsResponse>, t: Throwable) {
                    binding.loadingOverlay.visibility = View.GONE
                    Toast.makeText(
                        this@DiseaseMapActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }



    private fun plotSurveillancePoints(points: List<MapPoint>) {
        for (point in points) {
            val coords = parseGeo(point.geo) ?: continue
            map.addMarker(
                MarkerOptions()
                    .position(coords)
                    .title("📊 ${point.facility_name ?: point.label}")
                    .snippet(
                        buildString {
                            append("Epiweek ${point.epiweek ?: "—"}")
                            if (!point.region_name.isNullOrBlank())
                                append(" | ${point.region_name}")
                            if (!point.district_name.isNullOrBlank())
                                append(", ${point.district_name}")
                            if (!point.date_from.isNullOrBlank() && !point.date_to.isNullOrBlank())
                                append("\n${point.date_from} → ${point.date_to}")
                        }
                    )
                    .icon(BitmapDescriptorFactory.defaultMarker(210f))
            )
        }
    }

    private fun plotAnnexPoints(points: List<MapPoint>) {
        for (point in points) {
            val coords = parseGeo(point.geo) ?: continue

            val hue = when (point.outcome?.lowercase()) {
                "alive" -> BitmapDescriptorFactory.HUE_GREEN
                "dead"  -> BitmapDescriptorFactory.HUE_RED
                else    -> BitmapDescriptorFactory.HUE_YELLOW
            }

            map.addMarker(
                MarkerOptions()
                    .position(coords)
                    .title("🦠 ${point.label}")
                    .snippet(
                        buildString {
                            if (!point.gender.isNullOrBlank()) append("${point.gender}")
                            if (point.age != null)             append(", Age ${point.age}")
                            if (!point.outcome.isNullOrBlank()) append(" | ${point.outcome}")
                            if (!point.date_seen.isNullOrBlank()) append("\nSeen: ${point.date_seen}")
                            if (!point.region_name.isNullOrBlank())
                                append("\n${point.region_name}")
                            if (!point.district_name.isNullOrBlank())
                                append(", ${point.district_name}")
                        }
                    )
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )
        }
    }



    private fun updateLegendCounts() {
        val aliveCount = allAnnexPoints.count {
            it.outcome?.lowercase() == "alive"
        }
        val deadCount = allAnnexPoints.count {
            it.outcome?.lowercase() == "dead"
        }
        val unknownCount = allAnnexPoints.count {
            it.outcome.isNullOrBlank() ||
                    (it.outcome.lowercase() != "alive" && it.outcome.lowercase() != "dead")
        }

        binding.tvSurveillanceCount.text = "${allSurveillancePoints.size} facility reports"
        binding.tvAliveCount.text        = "$aliveCount alive cases"
        binding.tvDeadCount.text         = "$deadCount dead cases"
        binding.tvUnknownCount.text      = "$unknownCount pending cases"
    }

    private fun parseGeo(geo: String?): LatLng? {
        if (geo.isNullOrBlank()) return null
        return try {
            val parts = geo.split(",").map { it.trim().toDouble() }
            if (parts.size != 2) return null
            val lat = parts[0]
            val lng = parts[1]
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null
            LatLng(lat, lng)
        } catch (e: Exception) {
            null
        }
    }
}