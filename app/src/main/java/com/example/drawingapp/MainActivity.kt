package com.example.drawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.drawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var customDialog: Dialog? = null

    private var mImageButtonCurrentPaint: ImageButton? = null

    // create an activity result launcher to open an intent
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // get the returned result from the lambda and check the result code and the data returned
            if (result.resultCode == RESULT_OK && result.data != null) {
                // process the data
                // if the data is not null, set the image uri received
                binding.ivBackground.setImageURI(result.data?.data)
            }
        }

    /** create an ActivityResultLauncher with MultiplePermissions since we are requesting
     * both read and write
     */
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val perMissionName = it.key
                val isGranted = it.value
                // if permission is granted show a toast and perform operation
                if (isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()
                    // create an intent to pick image from external storage
                    val pickGalleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    // using the intent launcher created above launch the pick intent
                    openGalleryLauncher.launch(pickGalleryIntent)
                } else {
                    // Displaying another toast if permission is not granted and this time focus on
                    // Read external storage
                    if (perMissionName == Manifest.permission.READ_EXTERNAL_STORAGE)
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                }
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mImageButtonCurrentPaint = binding.llPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        binding.drawingView.setSizeForBrush(20.toFloat())

        binding.ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.ibGallery.setOnClickListener {
            requestStoragePermission()
        }

        binding.ibUndo.setOnClickListener {
            binding.drawingView.onClickUndo()
        }

        binding.ibSave.setOnClickListener {
            if(isReadStorageAllowed()){
                showCustomDialog()
                lifecycleScope.launch {
                    saveBitmapFile(getBitmapFromView(binding.flDrawingViewContainer))
                }
            }
        }
    }

    /**
     * This method is called when you want to change the size of the brush
     * and sets the brush/eraser sizes to the new values
     * depending on user selection.
     */
    private fun showBrushSizeChooserDialog() {
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size:")

        val smallBrushBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBrushBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBrushBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBrushBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBrushBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBrushBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    /**
     * This method is called when user click on any of the color selection
     */
    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()

            // Set the selected color to the paint color to use in our canvas
            binding.drawingView.setPaintColor(colorTag)

            // Set the pressed view/color background to pressed so we can see that it has been selected
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            //Set the old pressed view/color background to unpressed/default
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            //Set the current view to the old view
            mImageButtonCurrentPaint = view
        }
    }

    private fun requestStoragePermission() {
        // Check if the permission was denied and show rationale
        if (
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            // call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog(
                "Drawing App", "Drawing App " +
                        "needs to Access Your External Storage"
            )
        } else {
            // You can directly ask for the permission.
            // if it has not been denied then request for permission
            //  The registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

    }

    /**
     * We are calling this method to check the permission status
     */
    private fun isReadStorageAllowed(): Boolean {
        // Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }


    private fun getBitmapFromView(view: View): Bitmap {
        // Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        // Bind a canvas to it(the bitmap)
        val canvas = Canvas(returnedBitmap)

        // Get the view's background
        val bgDrawingView = view.background

        if (bgDrawingView != null) {
            // has background drawable, then draw it on the canvas
            bgDrawingView.draw(canvas)
        } else {
            // does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }

        // draw the view on the canvas
        view.draw(canvas)

        // return the bitmap
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream() // Creates a new byte array output stream.
                    // The buffer capacity is initially 32 bytes, though its size increases if necessary.

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    /**
                     * Write a compressed version of the bitmap to the specified outputstream.
                     * If this returns true, the bitmap can be reconstructed by passing a
                     * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
                     * all Formats support all bitmap configs directly, so it is possible that
                     * the returned bitmap from BitmapFactory could be in a different bitdepth,
                     * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
                     * pixels).
                     *
                     * @param format   The format of the compressed image
                     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
                     *                 small size, 100 meaning compress for max quality. Some
                     *                 formats, like PNG which is lossless, will ignore the
                     *                 quality setting
                     * @param stream   The outputstream to write the compressed data.
                     * @return true if successfully compressed to the specified stream.
                     */

                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.

                    val fo =
                        FileOutputStream(f) // Creates a file output stream to write to the file represented by the specified object.
                    fo.write(bytes.toByteArray()) // Writes bytes from the specified byte array to this file output stream.
                    fo.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.
                    result = f.absolutePath // The file absolute path is return as a result.
                    //We switch from io to ui thread to show a toast
                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showCustomDialog(){
        customDialog = Dialog(this)

        customDialog?.setContentView(R.layout.dialog_custom_progress)

        customDialog?.show()
    }

    /** Todo 3: create function to cancel dialog
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun cancelProgressDialog() {
        if (customDialog != null) {
            customDialog?.dismiss()
            customDialog = null
        }
    }
}