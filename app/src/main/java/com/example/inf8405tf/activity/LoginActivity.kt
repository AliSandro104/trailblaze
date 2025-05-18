package com.example.inf8405tf.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.inf8405tf.MainActivity
import com.example.inf8405tf.R
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.service.PasswordService
import com.example.inf8405tf.utils.UserSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var passwordService: PasswordService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialise sharedPreferences pour pouvoir l'utiliser pour stocker le username de l'utilisateur connecté
        UserSession.init(this)

        // Verifie si l'utilisateur est déjà authentifié
        lifecycleScope.launch {
            try {
                val authenticatedUser = withContext(Dispatchers.IO) {
                    database.userDao().getAuthenticatedUser()
                }

                if (authenticatedUser != null) {
                    // Enregistre le username dans les SharedPreferences pour l'utiliser plus tard
                    UserSession.saveUsername(authenticatedUser.username)

                    // Navigue vers MainActivity si l'utilisateur est déjà authentifié
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    // Si l'utilisateur n'est pas authentifié, affiche l'écran de connexion
                    setContentView(R.layout.activity_login)
                    setupUI()
                }
            } catch (e: Exception) {
                // Gestion d'erreurs si jamais la base de données retourne une erreur
                Log.e("LoginActivity", "Erreur de la base de données lors du démarrage: ${e.message}")
            }
        }
    }

    private fun setupUI() {
        val editUsername = findViewById<EditText>(R.id.editUsername)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val password = editPassword.text.toString().trim()

            // S'assure que tous les champs obligatoires sont remplis
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authenticateUser(username, password)
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun authenticateUser(username: String, password: String) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                database.userDao().getUserByUsername(username)
            }

            // Valide les entrées de l'utilisateur et retourne un message Toast assez générique
            // pour ne pas que l'utilisateur commence à brute force le mot de passe s'il découvre un username valide
            if (user == null || !passwordService.verifyPassword(password, user.passwordHash)) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Utilisateur ou mot de passe incorrect", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Met à jour l'utilisateur authentifié dans la base de données
            withContext(Dispatchers.IO) {
                database.userDao().updateUser(user.copy(authenticated = true))
            }

            // Enregistre le username dans les SharedPreferences pour l'utiliser plus tard
            UserSession.saveUsername(user.username)

            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }
}
