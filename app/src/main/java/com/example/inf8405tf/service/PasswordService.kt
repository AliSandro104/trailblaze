package com.example.inf8405tf.service

import at.favre.lib.crypto.bcrypt.BCrypt
import javax.inject.Inject

class PasswordService @Inject constructor() {

    private val bcrypt = BCrypt.withDefaults()
    private val verifier = BCrypt.verifyer()

    /**
     * Hashe un mot de passe en utilisant BCrypt
     * @param plainPassword Le mot de passe en clair
     * @return Le mot de passe hashé
     */
    fun hashPassword(plainPassword: String): String {
        // Utilise un coût de 12, ce qui est un bon équilibre entre sécurité et performance
        return bcrypt.hashToString(12, plainPassword.toCharArray())
    }

    /**
     * Vérifie si un mot de passe en clair correspond au hash stocké
     * @param plainPassword Le mot de passe en clair à vérifier
     * @param hashedPassword Le hash du mot de passe stocké
     * @return true si le mot de passe correspond, false sinon
     */
    fun verifyPassword(plainPassword: String, hashedPassword: String): Boolean {
        val result = verifier.verify(plainPassword.toCharArray(), hashedPassword)
        return result.verified
    }
}
