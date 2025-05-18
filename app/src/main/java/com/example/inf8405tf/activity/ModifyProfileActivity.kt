package com.example.inf8405tf.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.example.inf8405tf.R
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.service.FileManagerService
import com.example.inf8405tf.service.PasswordService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ModifyProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var passwordService: PasswordService

    @Inject
    lateinit var fileManagerService: FileManagerService

    private lateinit var editUsername: EditText
    private lateinit var editEmail: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var imageProfile: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSelectImage: Button
    private lateinit var editCurrentPassword: EditText
    private lateinit var genderSpinner: Spinner

    private var profileBitmap: Bitmap? = null
    private val PICK_IMAGE_REQUEST = 1
    private var newPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modify_profile)

        // Récupérer les références des composants UI pour les utiliser plus tard
        editUsername = findViewById(R.id.editUsername)
        editEmail = findViewById(R.id.editEmail)
        editNewPassword = findViewById(R.id.editNewPassword)
        editCurrentPassword = findViewById(R.id.editCurrentPassword)
        imageProfile = findViewById(R.id.imageProfile)
        genderSpinner = findViewById(R.id.spinnerNewGender)
        btnSave = findViewById(R.id.btnSave)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnCancel = findViewById(R.id.btnCancel)

        setupGenderSpinner()
        loadUserProfile()

        // Sélectionneur d'image
        btnSelectImage.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }

            // Préparation du fichier temporaire pour l'image prise avec la camera
            // https://www.tutorialspoint.com/java/io/file_createtempfile_directory.htm
            val photoFile = File.createTempFile("IMG_", ".jpg", getExternalFilesDir("Pictures"))

            // https://developer.android.com/reference/androidx/core/content/FileProvider
            newPhotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)

            // https://developer.android.com/training/sharing/send
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, newPhotoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Création du sélectionneur d'images customized incluant l'option camera (celui par défaut n'inclut pas la caméra)
            val chooser = Intent.createChooser(galleryIntent, "Choisir une photo")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            startActivityForResult(chooser, PICK_IMAGE_REQUEST)
        }

        btnSave.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val newPassword = editNewPassword.text.toString().trim()
            val currentPassword = editCurrentPassword.text.toString().trim()
            val selectedGender = genderSpinner.selectedItem.toString()

            // S'assure que tous les champs obligatoires sont remplis au cas ou l'utilisateur efface les champs par accident
            if (username.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedGender == "--- Genre ---") { // Empêche de choisir le placeholder "--- Genre ---"
                Toast.makeText(this, "Veuillez sélectionner un genre valide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateProfile(username, email, newPassword, currentPassword, selectedGender)
        }

        // L'utilisateur peut changer d'avis et retourner à la main activity sans rien changer
        btnCancel.setOnClickListener {
            finish()
        }
    }

    // Setup du dropdown pour le genre
    private fun setupGenderSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_options,
            android.R.layout.simple_spinner_dropdown_item
        )
        genderSpinner.adapter = adapter
    }

    // Montre les données de l'utilisateur qui sont dans la base de données
    private fun loadUserProfile() {
        lifecycleScope.launch {
            val userDao = database.userDao()
            val user = withContext(Dispatchers.IO) { userDao.getAuthenticatedUser() }

            user?.let {
                runOnUiThread {
                    editUsername.setText(it.username)
                    editEmail.setText(it.email)
                    // Charge la photo de profil si l'utilisateur en avait mis une avant
                    if (!it.profilePicturePath.isNullOrEmpty()) {
                        val uri = Uri.parse(it.profilePicturePath)
                        imageProfile.setImageURI(uri)
                    }
                }

                val genderOptions = resources.getStringArray(R.array.gender_options)
                val genderIndex = genderOptions.indexOf(it.gender)

                // Choisit le bon genre dans le dropdown en fonction de la valeur qui est dans la base de données pour cet utilisateur
                if (genderIndex >= 0) {
                    genderSpinner.setSelection(genderIndex)
                }
            }
        }
    }

    private fun updateProfile(username: String, email: String, password: String, currentPassword: String, gender: String) {
        lifecycleScope.launch {
            val userDao = database.userDao()
            val user = withContext(Dispatchers.IO) { userDao.getAuthenticatedUser() }

            // Vérification du mot de passe actuel si l'utilisateur veut modifier son mot de passe pour des raisons de sécurité
            if (user != null && password.isNotEmpty() && !passwordService.verifyPassword(currentPassword, user.passwordHash)) {
                runOnUiThread {
                    Toast.makeText(this@ModifyProfileActivity, "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Si le mot de passe est renseigné, on appelle le service pour hasher le nouveau mot de passe
            // Sinon, on reprend le hash du mot de passe actuel
            val newPasswordHash = if (password.isNotEmpty()) passwordService.hashPassword(password) else user?.passwordHash

            // Modification de la photo de profil si la variable profileBitmap est renseignée
            var profileUri: String? = user?.profilePicturePath
            profileBitmap?.let {
                profileUri = fileManagerService.saveProfileImage(it, System.currentTimeMillis()).toString()
            }

            val updatedUser = newPasswordHash?.let {
                user?.copy(
                    username = username,
                    email = email,
                    passwordHash = it,
                    profilePicturePath = profileUri,
                    gender = gender
                )
            }

            withContext(Dispatchers.IO) {
                if (updatedUser != null) {
                    userDao.updateUser(updatedUser)
                }
            }

            runOnUiThread {
                Toast.makeText(this@ModifyProfileActivity, "Profil modifié avec succès", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val imageUri = data?.data ?: newPhotoUri // Si image de la camera est null, c'est l'image de la galerie qui est utilisée
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
