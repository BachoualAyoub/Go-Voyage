package tn.govoyage.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.navigation.NavigationView
import tn.govoyage.mobile.databinding.ActivityMainBinding
import tn.govoyage.mobile.ui.auth.LoginActivity
import tn.govoyage.mobile.viewModel.AuthViewModel

/** Espace connecte : tiroir lateral et hote des fragments de voyage. */
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

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        // Le NavHostFragment est deja ajoute par FragmentContainerView : on lit son
        // controleur au lieu de findNavController, sa vue n'existe pas encore ici.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        // Les trois ecrans de haut niveau ouvrent le tiroir sans fleche retour.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_circuits, R.id.nav_compte), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // La deconnexion du tiroir passe par une confirmation, comme en TP3.
        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(navView)
            when (item.itemId) {
                R.id.nav_deconnexion -> {
                    confirmerDeconnexion()
                    true
                }
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // Session fermee (deconnexion ou jeton refuse) : retour a la connexion.
        authViewModel.session.observe(this) { session ->
            if (session == null) revenirConnexion()
        }
    }

    /** Dialogue de confirmation, identique au TP3. */
    private fun confirmerDeconnexion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.deconnexion_titre)
            .setMessage(R.string.deconnexion_message)
            .setPositiveButton(R.string.deconnexion_oui) { dialog, which ->
                authViewModel.deconnecter()
            }
            .setNegativeButton(R.string.deconnexion_non) { dialog, which ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_logout)
            .show()
    }

    /** Revient au formulaire de connexion et vide la pile d'ecrans. */
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
