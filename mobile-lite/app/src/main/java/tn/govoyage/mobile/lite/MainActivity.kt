package tn.govoyage.mobile.lite

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import tn.govoyage.mobile.lite.databinding.ActivityMainBinding
import tn.govoyage.mobile.lite.viewModel.AuthViewModel
import tn.govoyage.mobile.lite.viewModel.VoyageViewModel

/** Activite unique : tiroir lateral et hote des fragments, comme le TP5. */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // ViewModels crees par la fabrique par defaut (aucun parametre).
    private val authViewModel: AuthViewModel by viewModels()
    private val voyageViewModel: VoyageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController: NavController = findNavController(R.id.nav_host_fragment_content_main)

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

        // Toolbar et tiroir masques sur les ecrans de connexion et d'inscription.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val ecranAuth = destination.id == R.id.nav_login || destination.id == R.id.nav_register
            binding.appBarMain.barreApplication.isVisible = !ecranAuth
            if (ecranAuth) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        // La session ouvre ou ferme le carnet, et ramene a la connexion si vide.
        authViewModel.utilisateurConnecte.observe(this) { utilisateur ->
            if (utilisateur == null) {
                voyageViewModel.fermerCarnet()
                revenirConnexion()
            } else {
                voyageViewModel.ouvrirCarnet(utilisateur.id)
            }
        }
    }

    /** Dialogue de confirmation, identique au TP3. */
    private fun confirmerDeconnexion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.deconnexion_titre)
            .setMessage(R.string.deconnexion_message)
            .setPositiveButton(R.string.deconnexion_oui) { dialog, _ ->
                authViewModel.deconnecter()
            }
            .setNegativeButton(R.string.deconnexion_non) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_logout)
            .show()
    }

    /** Revient au formulaire de connexion et vide la pile d'ecrans. */
    private fun revenirConnexion() {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        if (navController.currentDestination?.id == R.id.nav_login) return
        val options = NavOptions.Builder()
            .setPopUpTo(R.id.nav_home, true)
            .build()
        navController.navigate(R.id.nav_login, null, options)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
