package com.keronei.kmlguage

data class EngineData(
    val rpm: Int,
    val instantSpeed: Int,
    val instantConsumptionPerKm: Double,
    val instantConsumptionPerHour: Double,
    val currentConsumptionPerKm: Double,
    val currentConsumedLitres: Double,
    val currentTripDistance: Double,
    val approximateRemainingTank: Double,
    val currentTripTime: String,
    val currentAverageSpeed: Double
)
