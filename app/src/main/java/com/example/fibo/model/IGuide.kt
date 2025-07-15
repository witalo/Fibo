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

data class IGuideData(
    val id: Int,
    val subsidiary: ISubsidiary,
    val client: IPerson,
    val documentType: String,
    val documentTypeReadable: String,
    val serial: String,
    val correlative: Int,
    val emitDate: String,
    val guideModeTransferReadable: String?,
    val guideReasonTransferReadable: String?,
    val operationDetailSet: List<IGuideDetail>,
    val relatedDocuments: List<IRelatedDocument>,
    val transferDate: String?,
    val totalWeight: String?,
    val weightMeasurementUnit: IWeightMeasurementUnit?,
    val quantityPackages: String?,
    val transportationCompany: IPerson?,
    val mainDriver: IPerson?,
    val mainVehicle: IVehicle?,
    val othersDrivers: List<IPerson>,
    val othersVehicles: List<IVehicle>,
    val receiver: IPerson?,
    val guideOrigin: IGuideLocation?,
    val guideArrival: IGuideLocation?
)

data class IGuideDetail(
    val productName: String,
    val quantity: String,
    val description: String
)

data class IWeightMeasurementUnit(
    val shortName: String
)

data class IGuideLocation(
    val district: IDistrict,
    val address: String,
    val serial: String
)

data class IDistrict(
    val id: String,
    val description: String
) 