package tn.govoyage.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import tn.govoyage.mobile.databinding.ActivityMainBinding
import tn.govoyage.mobile.ui.auth.LoginActivity
import tn.govoyage.mobile.viewModel.AuthViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val authViewModel: AuthViewModel by viewModels {
        viewModelFactory {
            initializer { AuthViewModel((application as GoVoyageApplication).auth) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        installerNavigation()
        observerDeconnexion()
    }


    private fun installerNavigation() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_destinations, R.id.nav_circuits,
                R.id.nav_compte
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_deconnexion -> {
                    drawerLayout.closeDrawer(navView)
                    confirmerDeconnexion()
                    true
                }
                R.id.nav_home -> {
                    revenirAccueil()
                    drawerLayout.closeDrawer(navView)
                    true
                }
                else -> {
                    val navigue = NavigationUI.onNavDestinationSelected(item, navController)
                    if (navigue) drawerLayout.closeDrawer(navView)
                    navigue
                }
            }
        }
    }


    private fun revenirAccueil() {
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.nav_home, true)
            .setLaunchSingleTop(true)
            .build()
        navController.navigate(R.id.nav_home, null, options)
    }


    private fun observerDeconnexion() {
        authViewModel.compte.observe(this) { compte ->
            if (compte == null) revenirConnexion()
        }
    }


    private fun confirmerDeconnexion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.deconnexion_titre)
            .setMessage(R.string.deconnexion_message)
            .setPositiveButton(R.string.deconnexion_oui) { dialog, _ ->
                authViewModel.deconnecter()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.deconnexion_non) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_logout)
            .show()
    }


    private fun revenirConnexion() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
