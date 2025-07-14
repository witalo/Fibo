package com.example.fibo.model

data class IGuide(
    val id: Int,
    val emitDate: String,
    val emitTime: String,
    val documentType: String,
    val serial: String,
    val correlative: Int,
    val subsidiary: ISubsidiary?,
    val client: IPerson?,
    val sendWhatsapp: Boolean,
    val sendClient: Boolean,
    val linkXml: String?,
    val linkCdr: String?,
    val sunatStatus: String?,
    val sunatDescription: String?,
    val operationStatus: String,
    val operationStatusReadable: String
)

data class IGuideResponse(
    val guides: List<IGuide>,
    val totalNumberOfPages: Int,
    val totalNumberOfSales: Int
) 