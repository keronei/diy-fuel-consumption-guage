package com.keronei.kmlguage

import com.keronei.kmlguage.USBDataHandler.parseTripTime

fun main() {
    val time = " 105 "

    println(parseTripTime(time.trim().toLong()))
}