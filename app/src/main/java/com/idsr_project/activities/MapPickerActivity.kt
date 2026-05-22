package com.idsr_project.activities

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.idsr_project.R
import com.idsr_project.databinding.ActivityMapPickerBinding
import java.util.Locale

class MapPickerActivity : BaseActivity() {
    private lateinit var binding: ActivityMapPickerBinding
    private var mMap: GoogleMap? = null
    private var marker: Marker? = null
    private var selectedLatLng: LatLng? = null


    private val gambiaCenter = LatLng(13.454, -15.674)
    private val gambiaBounds = LatLngBounds(
        LatLng(13.0, -17.0),
        LatLng(13.8, -13.8)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupClickListeners()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            configureMap()
            setupMapListeners()


            val existingLat = intent.getDoubleExtra("existing_lat", 0.0)
            val existingLng = intent.getDoubleExtra("existing_lng", 0.0)

            if (existingLat != 0.0 && existingLng != 0.0) {
                val existingLocation = LatLng(existingLat, existingLng)
                addMarker(existingLocation)
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(existingLocation, 15f))
            } else {

                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(gambiaCenter, 8f))
            }
        }
    }

    private fun configureMap() {
        mMap?.apply {
            // Enable UI controls
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = false // have our own button
            uiSettings.isMapToolbarEnabled = false
            uiSettings.isCompassEnabled = true


            setLatLngBoundsForCameraTarget(gambiaBounds)
            setMinZoomPreference(7f)
            setMaxZoomPreference(20f)


            mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    private fun setupMapListeners() {

        mMap?.setOnMapLongClickListener { latLng ->
            addMarker(latLng)
            showLocationInfo(latLng)
        }


        mMap?.setOnMapClickListener { latLng ->
            addMarker(latLng)
            showLocationInfo(latLng)
        }


        mMap?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {

            }

            override fun onMarkerDrag(marker: Marker) {

            }

            override fun onMarkerDragEnd(marker: Marker) {
                selectedLatLng = marker.position
                showLocationInfo(marker.position)
            }
        })
    }

    private fun setupClickListeners() {
        // Close button
        binding.btnClose.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }


        binding.btnMyLocation.setOnClickListener {
            // TODO: Implement current location functionality

            Toast.makeText(this, "Current location feature - coming soon", Toast.LENGTH_SHORT).show()
        }


        binding.btnSaveLocation.setOnClickListener {
            saveSelectedLocation()
        }
    }

    private fun addMarker(latLng: LatLng) {

        marker?.remove()


        marker = mMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title("Selected Location")
        )

        selectedLatLng = latLng


        marker?.showInfoWindow()


        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun showLocationInfo(latLng: LatLng) {

        binding.locationInfoCard.isVisible = true


        val coordinatesText = String.format(
            Locale.getDefault(),
            "%.6f, %.6f",
            latLng.latitude,
            latLng.longitude
        )
        binding.tvCoordinates.text = coordinatesText


        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = address.getAddressLine(0) ?: "Address not found"


                if (::binding.isInitialized) {
                    binding.tvAddress?.apply {
                        text = addressText
                        isVisible = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

    private fun saveSelectedLocation() {
        val position = selectedLatLng ?: marker?.position

        if (position != null) {
            val data = Intent().apply {
                putExtra("lat", position.latitude)
                putExtra("lng", position.longitude)
                putExtra("coordinates", String.format(
                    Locale.getDefault(),
                    "%.6f, %.6f",
                    position.latitude,
                    position.longitude
                ))
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            Toast.makeText(
                this,
                "Please select a location on the map",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mMap = null
        marker = null
    }
}