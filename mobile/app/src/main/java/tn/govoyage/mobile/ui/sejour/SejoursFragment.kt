package tn.govoyage.mobile.ui.sejour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import java.time.LocalDate
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentSejoursBinding
import tn.govoyage.mobile.viewModel.SejourViewModel

/** Recherche d'hotels et confirmation de reservation, avec cache local. */
class SejoursFragment : Fragment() {

    private var _binding: FragmentSejoursBinding? = null
    private val binding get() = _binding!!

    private val sejourViewModel: SejourViewModel by viewModels {
        viewModelFactory {
            initializer {
                SejourViewModel((requireActivity().application as GoVoyageApplication).sejours)
            }
        }
    }

    private lateinit var hotelAdapter: HotelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSejoursBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preparerDates()
        preparerListe()
        observerEtat()
        installerBoutons()
    }

    /** Propose deux periodes eloignees, valables pour la demonstration. */
    private fun preparerDates() {
        val debut = LocalDate.now().plusDays(30)
        binding.editArrivee.setText(debut.toString())
        binding.editDepart.setText(debut.plusDays(3).toString())
    }

    /** Installe la liste des offres. */
    private fun preparerListe() {
        hotelAdapter = HotelAdapter(mutableListOf()) { hotel ->
            sejourViewModel.reserver(
                hotel,
                binding.editArrivee.text.toString(),
                binding.editDepart.text.toString(),
                lireEntier(binding.editVoyageurs.text.toString(), 2),
                lireEntier(binding.editChambres.text.toString(), 1)
            ) {
                findNavController().navigate(R.id.action_nav_sejours_to_nav_reservations)
            }
        }
        binding.listeHotels.layoutManager = LinearLayoutManager(requireContext())
        binding.listeHotels.adapter = hotelAdapter

        sejourViewModel.hotels.observe(viewLifecycleOwner) { liste ->
            hotelAdapter.updateList(liste)
            binding.texteAucunHotel.visibility = if (liste.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** Affiche le chargement, les messages et la lecture hors ligne. */
    private fun observerEtat() {
        sejourViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.barreChargementSejours.visibility =
                if (etat.chargement) View.VISIBLE else View.GONE
            binding.boutonRechercher.isEnabled = !etat.chargement
            binding.carteErreurSejours.visibility =
                if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.texteErreurSejours.text = etat.erreur
            binding.texteHorsLigneSejours.visibility =
                if (etat.horsLigne) View.VISIBLE else View.GONE
            if (etat.information.isNotEmpty()) {
                Toast.makeText(requireContext(), etat.information, Toast.LENGTH_SHORT).show()
                sejourViewModel.effacerMessage()
            }
        }
    }

    /** Relie la recherche et l'acces au carnet. */
    private fun installerBoutons() {
        binding.boutonRechercher.setOnClickListener {
            sejourViewModel.rechercher(
                binding.editArrivee.text.toString(),
                binding.editDepart.text.toString(),
                lireEntier(binding.editVoyageurs.text.toString(), 2),
                lireEntier(binding.editChambres.text.toString(), 1)
            )
        }
        binding.boutonVoirReservations.setOnClickListener {
            findNavController().navigate(R.id.action_nav_sejours_to_nav_reservations)
        }
    }

    /** Lit un effectif en gardant une valeur plausible si la saisie est vide. */
    private fun lireEntier(texte: String, defaut: Int): Int {
        return texte.trim().toIntOrNull() ?: defaut
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
