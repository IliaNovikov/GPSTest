package com.novikov.gpstest

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.novikov.gpstest.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationRequest
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.directions.driving.VehicleType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider


class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding

    private lateinit var locationManager: LocationManager

    private lateinit var map:Map

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private lateinit var startpos: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey("f211024a-b739-4a93-b1c9-6d065fedfdbb")
        MapKitFactory.initialize(this)

        binding = ActivityMainBinding.inflate(layoutInflater)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        map = binding.mapView.map

        checkPermissions()

//        map.move(CameraPosition(Point(60.0, 60.0), 15f, 50f, 10f))
//        map.move(
//            CameraPosition(
//                Point(55.751225, 37.629540),
//                /* zoom = */ 18.0f,
//                /* azimuth = */ 250.0f,
//                /* tilt = */ 30.0f
//            )
//        )

        Log.i("point", map.cameraPosition.target.latitude.toString() + " " + map.cameraPosition.target.longitude)

        setContentView(binding.root)

    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )
        } else {
//            locationManager.requestLocationUpdates(
//                LocationManager.GPS_PROVIDER,
//                5,
//                0f,
//                LocationListener {
//                    //Toast.makeText(parent, it.toString(), Toast.LENGTH_LONG).show()
//                    binding.mapView.map.move(
//                        CameraPosition(
//                            Point(it.latitude, it.longitude),
//                            10f,
//                            50f,
//                            10f
//                        )
//                    )
//                })
            startpos = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!!
            startpos.let {
                map.move(
                    CameraPosition(
                        Point(it.latitude, it.longitude),
                        /* zoom = */ 16.0f,
                        /* azimuth = */ 0f,
                        /* tilt = */ 0f
                    )
                )
            }
            Log.i("latitude", startpos.latitude.toString())
            Log.i("longitude", startpos.longitude.toString())

            setMark()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == RESULT_OK){
            checkPermissions();
        }
        else{
            Toast.makeText(this, "successful", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        MapKitFactory.getInstance().onStop()
        binding.mapView.onStop()
    }

    private fun setMark() {
        val marker = R.drawable.location
        mapObjectCollection = map.mapObjects.addCollection()
        placemarkMapObject = map.mapObjects.addPlacemark().apply {
            Point(startpos.latitude, startpos.longitude)
        }
        val placemarkMapObject2 = mapObjectCollection.addPlacemark(
            Point(startpos.latitude + 0.01f, startpos.longitude + 0.01f)
        )
        Log.i("imageProvider", ImageProvider.fromResource(this, R.drawable.marker).toString())
        Log.i("latitude + longitude", startpos.latitude.toString() + " " + startpos.longitude.toString())
        placemarkMapObject.opacity = 0.8f
        placemarkMapObject.setText("Ваше местоположение")

        placemarkMapObject2.setText("2 точка")

        buildRoute(placemarkMapObject.geometry, placemarkMapObject2.geometry)

//        map.mapObjects.addPlacemark().apply {
//            geometry = Point(startpos.latitude, startpos.longitude)
//            setIcon(ImageProvider.fromResource(this@MainActivity, R.drawable.marker))
//            setIconStyle(IconStyle().apply {
//                scale = 0.6f
//            })
//        }

    }
    private fun buildRoute(point1:Point, point2:Point){

        val router = DirectionsFactory.getInstance().createDrivingRouter()
        val routeOptions = DrivingOptions().apply {
            routesCount = 3
        }
        val vehicleOptions = VehicleOptions().apply {
            vehicleType = VehicleType.DEFAULT
        }
        val points = buildList {
            add(RequestPoint(point1, RequestPointType.WAYPOINT, null, "0"))
            add(RequestPoint(point1, RequestPointType.WAYPOINT, null, "1"))
        }
        Log.i("size", points.size.toString())

        val drivingListener = object : DrivingRouteListener{
            override fun onDrivingRoutes(p0: MutableList<DrivingRoute>) {
                val selectedRoute = p0[0]
                val polylineMapObject = map.mapObjects.addPolyline(selectedRoute.geometry)
                polylineMapObject.strokeWidth = 1f
                polylineMapObject.setStrokeColor(Color.BLACK)
                Log.i("drivelistener", "end")

            }

            override fun onDrivingRoutesError(p0: Error) {
                TODO("Not yet implemented")
            }

        }

        router.requestRoutes(points, routeOptions, vehicleOptions, drivingListener)

    }
}