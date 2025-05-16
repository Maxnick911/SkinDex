package com.example.skindex

import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.skindex.core.util.JwtUtils
import com.example.skindex.data.storage.SecureStorage
import com.example.skindex.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject lateinit var secureStorage: SecureStorage
    @Inject lateinit var jwtUtils: JwtUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)


        val host = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = host.navController

        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        val token = secureStorage.getToken()
        if (token != null && !jwtUtils.isTokenExpired(token)) {
            graph.setStartDestination(R.id.doctorProfileFragment)
        } else {
            secureStorage.clearToken()
            graph.setStartDestination(R.id.loginFragment)
        }
        navController.graph = graph

        val appBarConfig = AppBarConfiguration(
            setOf(R.id.doctorProfileFragment, R.id.classificationFragment)
        )
        setupActionBarWithNavController(navController, appBarConfig)

        val bottomNav = binding.bottomNavMenu
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.doctorProfileFragment -> {
                    navController.popBackStack(R.id.doctorProfileFragment, false)
                    navController.navigate(R.id.doctorProfileFragment,
                        null,
                        NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.nav_graph, false)
                            .build()
                    )
                    true
                }
                R.id.classificationFragment -> {
                    navController.popBackStack(R.id.classificationFragment, false)
                    navController.navigate(R.id.classificationFragment,
                        null,
                        NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.nav_graph, false)
                            .build()
                    )
                    true
                }
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, dest, _ ->
            when (dest.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    binding.bottomNavMenu.visibility = View.GONE
                }
                else -> {
                    supportActionBar?.setDisplayHomeAsUpEnabled(
                        dest.id != R.id.doctorProfileFragment &&
                                dest.id != R.id.classificationFragment
                    )
                    binding.bottomNavMenu.visibility = View.VISIBLE
                }
            }
            invalidateOptionsMenu()
            supportActionBar?.title = getString(R.string.app_name)
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add_patient -> {
                    navController.navigate(R.id.action_doctorProfileFragment_to_addPatientFragment)
                    true
                }
                R.id.action_classify -> {
                    navController.navigate(R.id.action_doctorProfileFragment_to_classificationFragment)
                    true
                }
                R.id.action_logout_menu -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                secureStorage.clearToken()
                navController.navigate(R.id.loginFragment)
                navController.popBackStack(R.id.loginFragment, false)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val destId = navController.currentDestination?.id
        val moreItem = menu?.findItem(R.id.action_more)
        moreItem?.isVisible = destId == R.id.doctorProfileFragment
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
