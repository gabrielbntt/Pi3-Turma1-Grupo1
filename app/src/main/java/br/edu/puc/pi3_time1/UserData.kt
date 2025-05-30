package br.edu.puc.pi3_time1

import java.security.Timestamp

data class UserData (
    val email: String = "",
    val name: String = "",
    val hasEmailVerified: Boolean = false,
    val uid: String = "",
    val imei: String = ""
)
