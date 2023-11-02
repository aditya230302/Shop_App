package com.example.ecommerce.listener

import android.view.View

interface IRecyclerClickListener {
    fun onItemClickListener(view: View?, position:Int)
}