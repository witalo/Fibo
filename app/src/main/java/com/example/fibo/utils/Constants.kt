package com.example.fibo.utils

object Constants {
    // URLs base para diferentes entornos
    const val BASE_API_URL_LOCAL = "http://192.168.1.245:8000"
    const val BASE_API_URL_PRODUCTION = "https://ng.tuf4ctur4.net.pe"
    
    // Configuración del entorno actual (cambiar según necesidad)
    private const val IS_PRODUCTION = true // Cambiar a true para producción
    
    // URL base actual basada en el entorno
    val BASE_API_URL = if (IS_PRODUCTION) BASE_API_URL_PRODUCTION else BASE_API_URL_LOCAL
    
    fun getPdfUrl(guideId: Int): String {
        return "$BASE_API_URL/operations/print_guide/$guideId/"
    }
    
    // Función específica para previsualizar PDFs desde URL
    fun getPreviewPdfUrl(guideId: Int): String {
        return getPdfUrl(guideId)
    }
} 