package com.example.aos_ar_evacuation_beacon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aos_ar_evacuation_beacon.databinding.ActivityArBinding

class ARActivity : AppCompatActivity() {
   private lateinit var binding: ActivityArBinding

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = ActivityArBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)
      setBottomNavigation()
   }

   private fun setBottomNavigation() {
      binding.bottomNavigation.selectedItemId = R.id.navigationItem
      binding.bottomNavigation.setOnItemSelectedListener {
         when (it.itemId) {
            R.id.localizationItem -> {
               val intent = Intent(this, LocalizationActivity::class.java)
               startActivity(intent)
               overridePendingTransition(0, 0)
               finish()
            }
         }
         true

      }
   }

   companion object {
      const val TAG = "ARActivity"
   }
}