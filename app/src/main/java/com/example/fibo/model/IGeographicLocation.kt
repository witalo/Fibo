package com.example.fibo.model

data class IGeographicLocation(
    val districtId: String,
    val districtDescription: String,
    val provinceDescription: String,
    val departmentDescription: String
) {
    fun getDisplayText(): String {
        return "$districtId - $districtDescription | $provinceDescription | $departmentDescription"
    }
} 