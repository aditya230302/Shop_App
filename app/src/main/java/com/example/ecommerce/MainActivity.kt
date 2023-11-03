package com.example.ecommerce

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.location.Address
import android.location.Geocoder
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecommerce.adapter.MyDrinkAdapter
import com.example.ecommerce.eventbus.UpdateCartEvent
import com.example.ecommerce.listener.ICartLoadListener
import com.example.ecommerce.listener.IDrinkLoadListener
import com.example.ecommerce.model.CartModel
import com.example.ecommerce.model.DrinkModel
import com.example.ecommerce.utils.SpaceItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nex3z.notificationbadge.NotificationBadge
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.example.ecommerce.adapter.LocationConfirmationAdapter
import com.example.ecommerce.adapter.CustomLocationAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import android.Manifest
import android.widget.Toast

class MainActivity : AppCompatActivity(), IDrinkLoadListener, ICartLoadListener {

    lateinit var drinkLoadListener: IDrinkLoadListener
    lateinit var cartLoadListener: ICartLoadListener
    private lateinit var auth: FirebaseAuth
    private lateinit var locationConfirmationAdapter: LocationConfirmationAdapter
    private lateinit var customLocationAdapter: CustomLocationAdapter
    private val LOCATION_PERMISSION_REQUEST_CODE=100
    private lateinit var fusedLocationClient:FusedLocationProviderClient


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        if(EventBus.getDefault().hasSubscriberForEvent(UpdateCartEvent::class.java))
            EventBus.getDefault().removeStickyEvent(UpdateCartEvent::class.java)
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public fun onUpdateCartEvent(event: UpdateCartEvent)
    {
        countCartFromFirebase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        loadDrinkFromFirebase()
        countCartFromFirebase()
        auth = FirebaseAuth.getInstance()

        locationConfirmationAdapter = LocationConfirmationAdapter(this)
        customLocationAdapter = CustomLocationAdapter(this)
        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this)
    }
    private fun updateLocationInFirebase(location: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId != null) {
            // Define the Firebase reference where you want to store the user's location
            val userLocationReference = FirebaseDatabase.getInstance().getReference("UserLocations")

            // Store the user's location in Firebase
            userLocationReference.child(userId).setValue(location)
        }
    }

    fun getUserLocation(view: View) {
        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Get the user's location and handle location confirmation and custom input
            fusedLocationClient.lastLocation
                .addOnSuccessListener(this, OnSuccessListener<Location> { location ->
                    if (location != null) {
                        val geocoder = Geocoder(this)
                        val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                        // Check if the list is not null and not empty
                        addresses?.takeIf { it.isNotEmpty() }?.let { addressList ->
                            val address = addressList[0]
                            val obtainedLocation = address.getAddressLine(0)

                            locationConfirmationAdapter.showLocationConfirmationDialog(
                                obtainedLocation,
                                {
                                    // "Yes, set it as my location" button clicked
                                    updateLocationInFirebase(obtainedLocation)
                                    Toast.makeText(this,"Location Saved",Toast.LENGTH_LONG).show()
                                },
                                {
                                    // "No, I want to input my location" button clicked
                                    customLocationAdapter.showCustomLocationDialog { customLocation ->
                                        // Save the custom location to Firebase
                                        updateLocationInFirebase(customLocation)
                                        Toast.makeText(this,"Location Saved",Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }
                })
        }
    }





    private fun countCartFromFirebase() {
        val cartModels : MutableList<CartModel> = ArrayList()
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid
        FirebaseDatabase.getInstance()
            .getReference("Cart")
            .child(userId!!)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(cartSnapshot in snapshot.children)
                    {
                        val cartModel = cartSnapshot.getValue(CartModel::class.java)
                        cartModel!!.key = cartSnapshot.key
                        cartModels.add(cartModel)
                    }
                    cartLoadListener.onLoadCartSuccess(cartModels)
                }

                override fun onCancelled(error: DatabaseError) {
                    cartLoadListener.onLoadCartFailed(error.message)
                }
            })
    }

    private fun loadDrinkFromFirebase() {
        val drinkModels : MutableList<DrinkModel> = ArrayList()
        FirebaseDatabase.getInstance()
            .getReference("Drink")
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists())
                    {
                        for(drinkSnapshot in snapshot.children)
                        {
                            val drinkModel = drinkSnapshot.getValue(DrinkModel::class.java)
                            drinkModel!!.key = drinkSnapshot.key
                            drinkModels.add(drinkModel)
                        }
                        drinkLoadListener.onDrinkLoadSuccess(drinkModels)

                    }
                    else
                        drinkLoadListener.onDrinkLoadFailed("Drink items not exists")

                }

                override fun onCancelled(error: DatabaseError) {
                    drinkLoadListener.onDrinkLoadFailed(error.message)
                }
            })
    }

    private fun init(){
        drinkLoadListener = this
        cartLoadListener = this

        var mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
        var recycler_drink = findViewById<RecyclerView>(R.id.recycler_drink)
        
        val gridLayoutManager = GridLayoutManager(this,2)
        recycler_drink.layoutManager = gridLayoutManager
        recycler_drink.addItemDecoration(SpaceItemDecoration())

        var btnCart = findViewById<FrameLayout>(R.id.btnCart)
        var btnBack = findViewById<ImageView>(R.id.btnBack)

        btnCart.setOnClickListener{ startActivity(Intent(this,CartActivity::class.java))}
        btnBack.setOnClickListener{
            auth.signOut()
            Toast.makeText(this,"Signed Out",Toast.LENGTH_LONG).show()
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        var btnLocation = findViewById<ImageView>(R.id.btnLocation)
        btnLocation.setOnClickListener{
            getUserLocation(it)
        }

    }


    override fun onDrinkLoadSuccess(drinkModelList: List<DrinkModel>?) {
        val adapter = MyDrinkAdapter(this,drinkModelList!!,cartLoadListener)
        var recycler_drink = findViewById<RecyclerView>(R.id.recycler_drink)
        recycler_drink.adapter = adapter
    }

    override fun onDrinkLoadFailed(message: String?) {
        var mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
        Snackbar.make(mainLayout,message!!,Snackbar.LENGTH_LONG).show()
    }

    override fun onLoadCartSuccess(cartModelList: List<CartModel>) {
        var cartSum = 0
        for(cartModel in cartModelList!!)cartSum+= cartModel!!.quantity
        var badge = findViewById<NotificationBadge>(R.id.badge)
        badge!!.setNumber(cartSum)
    }

    override fun onLoadCartFailed(message: String?) {
        var mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
        Snackbar.make(mainLayout,message!!,Snackbar.LENGTH_LONG).show()
    }
}