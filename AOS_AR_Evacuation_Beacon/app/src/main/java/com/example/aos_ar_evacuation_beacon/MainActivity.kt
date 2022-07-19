package com.example.aos_ar_evacuation_beacon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aos_ar_evacuation_beacon.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
   private lateinit var binding: ActivityMainBinding

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      binding = ActivityMainBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)

      /*
      binding.bottomNavigation.setOnItemSelectedListener {
         when (it.itemId) {
            R.id.localizationItem -> {
               val intent = Intent(this, LocalizationActivity.javaClass)
               startActivity(intent)
               overridePendingTransition(0, 0)
               true
            }

            R.id.navigationItem -> {
               val intent = Intent(this, ARActivity.javaClass)
               startActivity(intent)
               overridePendingTransition (0, 0)
               true
            }
         }
         false

      }
      */

   }
}