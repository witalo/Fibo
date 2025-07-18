package com.example.fibo.model

data class IOperation(
    val id: Int,
    val serial: String = "", //Serie Factura
    val correlative: Int = 0, //Numero Factura
    val operationType: String = "", //Tipo Operacion operationType = "0101",
    val documentTypeReadable: String = "",
    val operationStatus: String = "", //Estado Opercion operationStatus = "01",
    val operationAction: String = "", //Accion de la operacion operationAction = "E",
    val documentType: String = "", //Tipo de documento documentType = "01",
    val currencyType: String = "", //Moneda  currencyType = "PEN",
    val operationDate: String = "", //Fecha de operacion
    val emitDate: String = "", //Fecha de emision
    val emitTime: String = "", //Hora emision
    val userId: Int = 0, //Usuario ID
    val client: IPerson, //Person
    val supplier: ISupplier? = null, //Supplier (para compras)
    val subsidiaryId: Int, //subsidiary ID
    val discountGlobal: Double = 0.0, //Descuento Global
    val discountPercentageGlobal: Double = 0.0, //Porcentaje del descuento global
    val discountForItem: Double = 0.0,  //Total descuento por item la suma total de descuentos de los items
    val totalDiscount: Double = 0.0,  // Suma de todos los descuentos (global + por ítem)
    val totalTaxed: Double = 0.0, // Operaciones gravadas después de descuentos
    val totalUnaffected: Double = 0.0, // Operaciones inafectas
    val totalExonerated: Double = 0.0, // Operaciones exoneradas
    val totalIgv: Double = 0.0, // IGV calculado sobre totalTaxed
    val totalFree: Double = 0.0, // Operaciones gratuitas
    val totalAmount: Double = 0.0, // Suma de bases imponibles + IGV
    val totalToPay: Double = 0.0, // Total a pagar (debe ser igual a totalAmount)
    val totalPayed: Double = 0.0, // Total pagado
    val operationDetailSet: List<IOperationDetail> = emptyList()
)