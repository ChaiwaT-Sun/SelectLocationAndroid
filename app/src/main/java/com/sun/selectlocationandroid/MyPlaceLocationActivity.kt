package com.sun.selectlocationandroid

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sun.selectlocationandroid.dialog.MyLocationDialogFragment
import java.util.*

class MyPlaceLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "PlacePickerActivity"
    }

    private lateinit var map: GoogleMap


    private lateinit var markerImage: ImageView
    private lateinit var markerShadowImage: ImageView
    private lateinit var placeSelectedFab: FloatingActionButton
    private lateinit var myLocationFab: FloatingActionButton
    private lateinit var placeNameTextView: TextView
    private lateinit var placeAddressTextView: TextView
    private lateinit var infoLayout: FrameLayout
    private lateinit var placeCoordinatesTextView: TextView
    private lateinit var placeProgressBar: ProgressBar

    private var latitude = 13.736717
    private var longitude = 100.523186
    private var initLatitude = 13.736717
    private var initLongitude = 100.523186
    private var showLatLong = true
    private var zoom = 14.0F
    private var addressRequired: Boolean = true
    private var shortAddress = ""
    private var fullAddress = ""
    private var mapRawResourceStyleRes: Int = -1
    private var addresses: List<Address>? = null

    lateinit var dialog : MyLocationDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        bindViews()
        setupview()
    }


    private fun setupview(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        placeCoordinatesTextView.visibility = if (showLatLong) View.VISIBLE else View.GONE

        placeSelectedFab.setOnClickListener {
            if (addresses != null) {
                val addressData = AddressData(latitude, longitude, addresses)
                val returnIntent = Intent()
                returnIntent.putExtra(Constants.ADDRESS_INTENT, addressData)
                setResult(RESULT_OK, returnIntent)
                //  finish()

                dialog = MyLocationDialogFragment()
                dialog.addressData = addressData
                dialog.show(supportFragmentManager,"ddd")


                Toast.makeText(this, "${addressData}", Toast.LENGTH_LONG).show()
            } else {
                if (!addressRequired) {
                    sendOnlyCoordinates()
                } else {
                    Toast.makeText(this, R.string.no_address, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        myLocationFab.setOnClickListener {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(initLatitude, initLongitude),
                    zoom
                )
            )
        }

    }


    private fun bindViews() {
        markerImage = findViewById(R.id.marker_image_view)
        markerShadowImage = findViewById(R.id.marker_shadow_image_view)
        placeSelectedFab = findViewById(R.id.place_chosen_button)
        myLocationFab = findViewById(R.id.my_location_button)
        placeNameTextView = findViewById(R.id.text_view_place_name)
        placeAddressTextView = findViewById(R.id.text_view_place_address)
        placeCoordinatesTextView = findViewById(R.id.text_view_place_coordinates)
        infoLayout = findViewById(R.id.info_layout)
        placeProgressBar = findViewById(R.id.progress_bar_place)
    }

    private fun sendOnlyCoordinates() {
        val addressData = AddressData(latitude, longitude, null)
        val returnIntent = Intent()
        returnIntent.putExtra(Constants.ADDRESS_INTENT, addressData)
        setResult(RESULT_OK, returnIntent)
        finish()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnCameraMoveStartedListener {
            if (markerImage.translationY == 0f) {
                markerImage.animate()
                    .translationY(-75f)
                    .setInterpolator(OvershootInterpolator())
                    .setDuration(250)
                    .start()
            }
        }

        map.setOnCameraIdleListener {
            markerImage.animate()
                .translationY(0f)
                .setInterpolator(OvershootInterpolator())
                .setDuration(250)
                .start()

            showLoadingBottomDetails()
            val latLng = map.cameraPosition.target
            latitude = latLng.latitude
            longitude = latLng.longitude
            AsyncTask.execute {
                getAddressForLocation()
                runOnUiThread { setPlaceDetails(latitude, longitude, shortAddress, fullAddress) }
            }
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom))
        if (mapRawResourceStyleRes != -1) {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, mapRawResourceStyleRes))
        }

    }

    private fun showLoadingBottomDetails() {
        placeNameTextView.text = ""
        placeAddressTextView.text = ""
        placeCoordinatesTextView.text = ""
        placeProgressBar.visibility = View.VISIBLE
    }

    private fun setPlaceDetails(
        latitude: Double,
        longitude: Double,
        shortAddress: String,
        fullAddress: String
    ) {

        if (latitude == -1.0 || longitude == -1.0) {
            placeNameTextView.text = ""
            placeAddressTextView.text = ""
            placeProgressBar.visibility = View.VISIBLE
            return
        }
        placeProgressBar.visibility = View.INVISIBLE

        placeNameTextView.text = if (shortAddress.isEmpty()) "Dropped Pin" else shortAddress
        placeAddressTextView.text = fullAddress
        placeCoordinatesTextView.text =
            Location.convert(latitude, Location.FORMAT_DEGREES) + ", " + Location.convert(
                longitude,
                Location.FORMAT_DEGREES
            )
    }

    private fun getAddressForLocation() {
        setAddress(latitude, longitude)
    }

    private fun setAddress(
        latitude: Double,
        longitude: Double
    ) {
        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geoCoder.getFromLocation(latitude, longitude, 1)
            this.addresses = addresses
            return if (addresses != null && addresses.size != 0) {
                fullAddress = addresses[0].getAddressLine(
                    0
                ) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                shortAddress = generateFinalAddress(fullAddress).trim()
            } else {
                shortAddress = ""
                fullAddress = ""
            }
        } catch (e: Exception) {
            //Time Out in getting address
            Log.e(TAG, e.message)
            shortAddress = ""
            fullAddress = ""
            addresses = null
        }
    }

    private fun generateFinalAddress(
        address: String
    ): String {
        val s = address.split(",")
        return if (s.size >= 3) s[1] + "," + s[2] else if (s.size == 2) s[1] else s[0]
    }
}
