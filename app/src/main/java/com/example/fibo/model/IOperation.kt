package com.example.fibo.model

data class IOperation(
    val id: Int,
    val serial: String = "",
    val correlative: Int = 0,
    val operationType: String = "",
    val documentTypeReadable: String = "",
    val operationStatus: String = "",
    val documentType: String = "",
    val operationDate: String = "",
    val emitDate: String = "",
    val client: IPerson,
//    val subsidiary: ISubsidiary,
    val discountGlobal: Double = 0.0,
    val discountPercentageGlobal: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val totalTaxed: Double = 0.0,
    val totalUnaffected: Double = 0.0,
    val totalExonerated: Double = 0.0,
    val totalIgv: Double = 0.0,
    val totalFree: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalToPay: Double = 0.0,
    val totalPayed: Double = 0.0,
    val operationDetailSet: List<IOperationDetail> = emptyList(),
    val cashFlowSet: List<ICashFlow> = emptyList()
)