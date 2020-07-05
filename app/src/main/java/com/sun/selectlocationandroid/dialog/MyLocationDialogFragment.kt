package com.sun.selectlocationandroid.dialog


import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sun.selectlocationandroid.AddressData
import com.sun.selectlocationandroid.R
import kotlinx.android.synthetic.main.dialog_my_location.*


class MyLocationDialogFragment : DialogFragment(), OnMapReadyCallback {
    private val DEFAULT_ZOOM = 16.0f

    companion object{
        fun newInstance() = MyLocationDialogFragment()
    }

    var addressData:AddressData? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_my_location, container)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)



        val map = MapView(this.activity)
        map.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        StoreMap.addView(map)

        map.onCreate(savedInstanceState)
        map.onResume()
        map.getMapAsync(this)

        addressData!!.addressList.let {

            tvDetailLocation.text = "${it}"
        }


    }




    override fun onMapReady(googleMap: GoogleMap?) {
        val myLatitude =addressData!!.latitude!!.toDouble()
        val myLongitude = addressData!!.longitude!!.toDouble()
        val location = LatLng(myLatitude, myLongitude)
        googleMap?.addMarker(
            MarkerOptions()
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.home_run))
            .position(location))
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
            LatLng(myLatitude, myLongitude),DEFAULT_ZOOM))
    }

}