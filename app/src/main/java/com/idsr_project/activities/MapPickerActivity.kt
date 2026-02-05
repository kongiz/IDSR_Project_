package com.idsr_project.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.idsr_project.R
import com.idsr_project.databinding.ActivityMapPickerBinding

class MapPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapPickerBinding
    private  var mMap: GoogleMap? = null
    private  var marker: Marker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            mMap?.uiSettings?.isZoomControlsEnabled = true
            val start = LatLng(13.454, -15.674)
//            val gambiaBounds = LatLngBounds(
//                LatLng(13.0, -17.0),
//                LatLng(13.8, -13.8)
//            )
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 15f))

//            mMap?.setLatLngBoundsForCameraTarget (gambiaBounds)
//
//            mMap?.setMinZoomPreference(7f)
//            mMap?.setMaxZoomPreference(28f)


                        mMap ?. setOnMapLongClickListener { latLng ->
                    marker?.remove()
                    marker = mMap?.addMarker(MarkerOptions().position(latLng).draggable(true))
                }
                        binding . btnSaveLocation . setOnClickListener {
                    marker?.position?.let { position ->
                        val data = Intent().apply {
                            putExtra("lat", position.latitude)
                            putExtra("lng", position.longitude)
                        }
                        setResult(RESULT_OK, data)
                        finish()
                    }
                }

        }


    }
}