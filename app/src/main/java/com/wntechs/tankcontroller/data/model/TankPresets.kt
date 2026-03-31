package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TankModel(
    val code: String,
    val capacity_litres: Int,
    val dimensions: TankDimensions
)

@Serializable
data class TankDimensions(
    val height_mm: Int,
    val diameter_mm: Int? = null,
    val length_mm: Int? = null,
    val breadth_mm: Int? = null
)

@Serializable
data class TankFamily(
    val family: String,
    val shape: String,
    val models: List<TankModel>
)

@Serializable
data class TankMeasurements(
    val manufacturer: String,
    val unit: String,
    val tank_families: List<TankFamily>
)
