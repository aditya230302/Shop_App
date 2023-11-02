package com.example.ecommerce.adapter

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.TextView
import com.example.ecommerce.R

class LocationConfirmationAdapter(private val context: Context) {
    fun showLocationConfirmationDialog(
        location: String,
        onSetAsLocationClick: () -> Unit,
        onInputCustomLocationClick: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.location_confirmation_dialog)
        val locationTextView = dialog.findViewById<TextView>(R.id.locationTextView)
        locationTextView.text = location
        val btnYesLocation = dialog.findViewById<Button>(R.id.btnYesLocation)
        val btnNoLocation = dialog.findViewById<Button>(R.id.btnNoLocation)

        btnYesLocation.setOnClickListener {
            onSetAsLocationClick()
            dialog.dismiss()
        }

        btnNoLocation.setOnClickListener {
            onInputCustomLocationClick()
            dialog.dismiss()
        }

        dialog.show()
    }
}