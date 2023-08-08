package com.example.drawingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.drawingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var drawingView: DrawingView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.drawingView.setSizeForBrush(20.toFloat())
    }
}