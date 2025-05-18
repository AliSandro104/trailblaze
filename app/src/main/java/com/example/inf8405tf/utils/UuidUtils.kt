package com.example.inf8405tf.utils

import jakarta.inject.Inject
import javax.inject.Singleton

@Singleton
class UuidUtils @Inject constructor() {

    fun generateUUID(): String {
        return java.util.UUID.randomUUID().toString()
    }
}