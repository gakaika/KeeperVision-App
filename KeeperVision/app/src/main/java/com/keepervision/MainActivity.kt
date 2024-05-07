package com.keepervision

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.keepervision.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        /*val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_camera, R.id.navigation_metrics))
        setupActionBarWithNavController(navController, appBarConfiguration)*/
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id == R.id.navigation_login_entry) {
                findViewById<ConstraintLayout>(R.id.toolbar).visibility = View.GONE
            } else {
                findViewById<ConstraintLayout>(R.id.toolbar).visibility = View.VISIBLE
            }

            if((destination.id == R.id.navigation_login_entry) || (destination.id == R.id.navigation_settings)) {
                navView.visibility = View.GONE
            }
            else {
                navView.visibility = View.VISIBLE
            }
        }

        findViewById<ImageView>(R.id.settings_gear).setOnClickListener{
            findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.navigation_settings)
        }

        /*supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setIcon(R.drawable.ic_keepervision_ball)*/
    }
}