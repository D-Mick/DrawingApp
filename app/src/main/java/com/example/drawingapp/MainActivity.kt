package com.example.drawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.drawingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var mImageButtonCurrentPaint: ImageButton? = null
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
    }

    /**
     * This method is called when you want to change the size of the brush
     * and sets the brush/eraser sizes to the new values
     * depending on user selection.
     */
    private fun showBrushSizeChooserDialog(){
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
        if(view !== mImageButtonCurrentPaint){
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
}