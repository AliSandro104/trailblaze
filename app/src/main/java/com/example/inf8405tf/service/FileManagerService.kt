package com.example.inf8405tf.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class FileManagerService @Inject constructor(@ApplicationContext private val context: Context) {

    // Répertoire privé de l'application pour stocker les images
    // Ce dossier est automatiquement créé s'il n'existe pas déjà
    // Il est uniquement accessible par l’application avec le mode privé
    private val imagesDir by lazy {
        context.getDir("images", Context.MODE_PRIVATE)
    }

    /**
     * Sauvegarde une image de profil sous forme de fichier JPEG dans le dossier privé de l'app
     *
     * @param bitmap L'image de profil à sauvegarder
     * @param username L'identifiant de l'utilisateur (utilisé pour nommer le fichier)
     * @return L'URI qui pointe vers le fichier sauvegardé
     */
    fun saveProfileImage(bitmap: Bitmap, username: Long): Uri {
        val fileName = "profile_$username.jpg"
        val file = File(imagesDir, fileName)

        // convertit le bitmap en fichier JPEG avec qualité = 100%
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        return Uri.fromFile(file)
    }
}
