package com.example.ideality.utils

import android.graphics.Bitmap
import android.icu.math.BigDecimal

data class CatalogueElement(
    var loc: String,
    var desc: String,
    var headerImage: Bitmap?,
    private var price: BigDecimal
) {

    private val imageList = ArrayList<Bitmap>()
    fun setPrice(price: String) {
        this.price = BigDecimal(price)
    }
    fun getPrice() = price
    fun addImage(image: Bitmap) {
        imageList.add(image)
    }
}