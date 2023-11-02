package com.example.ecommerce.adapter

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.EditText
import com.example.ecommerce.R

class CustomLocationAdapter(private val context: Context) {
    fun showCustomLocationDialog(
        onSaveLocationClick: (String) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.enter_custom_location)
        val customLocationEditText = dialog.findViewById<EditText>(R.id.customLocationEditText)
        val btnSaveCustomLocation = dialog.findViewById<Button>(R.id.btnSaveCustomLocation)

        btnSaveCustomLocation.setOnClickListener {
            val customLocation = customLocationEditText.text.toString()
            onSaveLocationClick(customLocation)
            dialog.dismiss()
        }

        dialog.show()
    }
}