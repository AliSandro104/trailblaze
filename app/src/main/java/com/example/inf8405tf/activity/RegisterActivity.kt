package com.example.inf8405tf.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.example.inf8405tf.R
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.domain.User
import com.example.inf8405tf.service.FileManagerService
import com.example.inf8405tf.service.PasswordService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var passwordService: PasswordService

    @Inject
    lateinit var fileManagerService: FileManagerService

    private var profileBitmap: Bitmap? = null
    private lateinit var imageProfile: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Récupérer les références des composants UI pour les utiliser plus tard
        imageProfile = findViewById(R.id.imageProfile)
        val editUsername = findViewById<EditText>(R.id.editUsername)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val spinnerGender = findViewById<Spinner>(R.id.spinnerGender)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)

        setupGenderSpinner(spinnerGender)

        // Sélectionneur d'image
        btnSelectImage.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }

            // Préparation du fichier temporaire pour l'image prise avec la camera
            // https://www.tutorialspoint.com/java/io/file_createtempfile_directory.htm
            val photoFile = File.createTempFile("IMG_", ".jpg", getExternalFilesDir("Pictures"))

            // https://developer.android.com/reference/androidx/core/content/FileProvider
            photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)

            // https://developer.android.com/training/sharing/send
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Création du sélectionneur d'images customized incluant l'option camera (celui par défaut n'inclut pas la caméra)
            val chooser = Intent.createChooser(galleryIntent, "Choisir une photo")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            startActivityForResult(chooser, PICK_IMAGE_REQUEST)
        }

        btnRegister.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val gender = spinnerGender.selectedItem.toString()

            // S'assure que tous les champs sont remplis
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Empêcher l'utilisateur de choisir le placeholder "--- Genre ---"
            if (gender == "--- Genre ---") {
                Toast.makeText(this, "Veuillez sélectionner un genre valide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(username, email, password, gender)
        }

        // L'utilisateur peut changer d'avis et retourner au login sans s'inscrire
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)
        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupGenderSpinner(spinner: Spinner) {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner.adapter = adapter

        // Choisir la première option par défaut, qui est un placeholder (mais l'utilisateur doit absolument choisir un vrai genre pour l'inscription)
        spinner.setSelection(0)
    }

    private fun registerUser(username: String, email: String, password: String, gender: String) {
        lifecycleScope.launch {
            val userExists = withContext(Dispatchers.IO) {
                database.userDao().getUserByUsername(username)
            }

            // On peut pas enregistrer un deuxième utilisateur avec le même username
            if (userExists != null) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "L'utilisateur existe déjà", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // On hash le mot de passe avec bCrypt
            val hashedPassword = passwordService.hashPassword(password)
            var profileUri: String? = null

            // Ajout de la photo de profil si la variable profileBitmap est renseignée. Sinon on laisse l'image par défaut qui est un placeholder.
            // C'est correct de pas avoir de photo de profil
            profileBitmap?.let {
                profileUri = fileManagerService.saveProfileImage(it, System.currentTimeMillis()).toString()
            }

            val newUser = User(
                username = username,
                email = email,
                passwordHash = hashedPassword,
                profilePicturePath = profileUri,
                creationTimestamp = Date(),
                authenticated = false,
                gender = gender
            )

            withContext(Dispatchers.IO) {
                database.userDao().insertUser(newUser)
            }

            // Retourner à la page de connexion quand l'inscription réussit
            runOnUiThread {
                Toast.makeText(this@RegisterActivity, "Inscription complétée avec succès", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val imageUri = data?.data ?: photoUri // Si l'image de la camera est null, c'est l'image de la galerie qui est utilisée
            imageUri?.let {
                try {
                    // Image est décodée en bitmap
                    // https://developer.android.com/reference/android/provider/MediaStore.Images.Media#getBitmap(android.content.ContentResolver,%20android.net.Uri)
                    val inputStream = contentResolver.openInputStream(it)
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)

                    val exif = ExifInterface(inputStream!!)
                    // L’orientation de l'image est lue grâce à la classe ExifInterface, ce qui permet de corriger la rotation si l'image est mal tournée
                    // https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

                    // En fonction de l’orientation EXIF, l'image est tournée de 90°, 180°, 270° ou pas du tout pour qu'elle soit droite à la fin
                    val rotatedBitmap = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                        else -> bitmap
                    }

                    imageProfile.setImageBitmap(rotatedBitmap)
                    profileBitmap = rotatedBitmap

                    inputStream.close()
                } catch (e: Exception) {
                    Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    // Utilise Matrix pour tourner l'image avec un certain angle
    // https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Matrix
    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

}
