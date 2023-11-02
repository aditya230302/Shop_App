package com.example.ecommerce

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide.init
import com.example.ecommerce.adapter.MyCartAdapter
import com.example.ecommerce.eventbus.UpdateCartEvent
import com.example.ecommerce.listener.ICartLoadListener
import com.example.ecommerce.model.CartModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder

class CartActivity : AppCompatActivity(), ICartLoadListener {

    var cartLoadListener:ICartLoadListener?=null

    val user = FirebaseAuth.getInstance().currentUser
    val userId = user?.uid

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
        loadCartFromFirebase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        init()
        loadCartFromFirebase()

    }

    private fun loadCartFromFirebase() {
        val cartModels : MutableList<CartModel> = ArrayList()
        FirebaseDatabase.getInstance()
            .getReference("Cart")
            .child(userId!!)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for(cartSnapshot in snapshot.children)
                    {
                        val cartModel = cartSnapshot.getValue(CartModel::class.java)
                        cartModel!!.key = cartSnapshot.key
                        cartModels.add(cartModel)
                    }
                    cartLoadListener!!.onLoadCartSuccess(cartModels)
                }

                override fun onCancelled(error: DatabaseError) {
                    cartLoadListener!!.onLoadCartFailed(error.message)
                }
            })
    }

    private fun init(){
        cartLoadListener = this
        val layoutManager = LinearLayoutManager(this)
        var recycler_cart = findViewById<RecyclerView>(R.id.recycler_cart)
        var btnBack = findViewById<ImageView>(R.id.btnBack)
        var btnDeleteAll = findViewById<Button>(R.id.btnDeleteAll)
        btnDeleteAll.setOnClickListener{
            deleteAllItems(it)
        }

        recycler_cart!!.layoutManager = layoutManager
        recycler_cart!!.addItemDecoration(DividerItemDecoration(this,layoutManager.orientation))
        btnBack!!.setOnClickListener{finish()}

    }
    private fun deleteAllItems(view: View) {
        showConfirmationDialog()
    }

    private fun showConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.delete_confirmation_dialog)
        val btnGotIt = dialog.findViewById<Button>(R.id.btnGotIt)
        btnGotIt.setOnClickListener {
            dialog.dismiss()
            // Add the logic here to delete all items from the cart (as previously described).
            val cartReference = FirebaseDatabase.getInstance().getReference("Cart")
            cartReference.child(userId!!).removeValue()
            EventBus.getDefault().postSticky(UpdateCartEvent())
            // After deleting, load the updated cart data
            loadCartFromFirebase()
        }
        dialog.show()
    }

    override fun onLoadCartSuccess(cartModelList: List<CartModel>) {
        var sum = 0.0
        for(cartModel in cartModelList!!){
            sum+=cartModel!!.totalPrice
        }
        var txtTotal = findViewById<TextView>(R.id.txtTotal)
        txtTotal.text = StringBuilder("$").append(sum)
        val adapter = MyCartAdapter(this,cartModelList)
        var recycler_cart = findViewById<RecyclerView>(R.id.recycler_cart)
        recycler_cart!!.adapter = adapter

    }

    override fun onLoadCartFailed(message: String?) {
        var mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
        Snackbar.make(mainLayout,message!!, Snackbar.LENGTH_LONG).show()
    }
}