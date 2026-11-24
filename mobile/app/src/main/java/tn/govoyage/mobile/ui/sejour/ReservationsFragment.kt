package tn.govoyage.mobile.ui.sejour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import tn.govoyage.mobile.GoVoyageApplication
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.FragmentReservationsBinding
import tn.govoyage.mobile.model.Reservation
import tn.govoyage.mobile.viewModel.SejourViewModel

/** Carnet des reservations du compte connecte, consultable hors ligne. */
class ReservationsFragment : Fragment() {

    private var _binding: FragmentReservationsBinding? = null
    private val binding get() = _binding!!

    private val sejourViewModel: SejourViewModel by viewModels {
        viewModelFactory {
            initializer {
                SejourViewModel((requireActivity().application as GoVoyageApplication).sejours)
            }
        }
    }

    private lateinit var reservationAdapter: ReservationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preparerListe()
        observerEtat()
        sejourViewModel.chargerReservations()
        installerBoutons()
    }

    /** Installe la liste du carnet et son action d'annulation. */
    private fun preparerListe() {
        reservationAdapter = ReservationAdapter(mutableListOf()) { reservation ->
            confirmerAnnulation(reservation)
        }
        binding.listeReservations.layoutManager = LinearLayoutManager(requireContext())
        binding.listeReservations.adapter = reservationAdapter

        sejourViewModel.reservations.observe(viewLifecycleOwner) { liste ->
            reservationAdapter.updateList(liste)
            binding.texteAucuneReservation.visibility =
                if (liste.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** Affiche le chargement, les messages et la lecture hors ligne. */
    private fun observerEtat() {
        sejourViewModel.etat.observe(viewLifecycleOwner) { etat ->
            binding.barreChargementReservations.visibility =
                if (etat.chargement) View.VISIBLE else View.GONE
            binding.boutonActualiserReservations.isEnabled = !etat.chargement
            binding.carteErreurReservations.visibility =
                if (etat.erreur.isEmpty()) View.GONE else View.VISIBLE
            binding.texteErreurReservations.text = etat.erreur
            binding.texteHorsLigneReservations.visibility =
                if (etat.horsLigne) View.VISIBLE else View.GONE
            if (etat.information.isNotEmpty()) {
                Toast.makeText(requireContext(), etat.information, Toast.LENGTH_SHORT).show()
                sejourViewModel.effacerMessage()
            }
        }
    }

    /** Relie le rafraichissement et le retour a la recherche. */
    private fun installerBoutons() {
        binding.boutonActualiserReservations.setOnClickListener {
            sejourViewModel.chargerReservations()
        }
        binding.boutonRetourSejours.setOnClickListener {
            findNavController().navigate(R.id.action_nav_reservations_to_nav_sejours)
        }
    }

    /** Evite une annulation accidentelle. */
    private fun confirmerAnnulation(reservation: Reservation) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.sejour_confirmation_titre)
            .setMessage(getString(R.string.sejour_confirmation_message, reservation.hotelNom))
            .setPositiveButton(R.string.sejour_confirmation_oui) { dialog, _ ->
                sejourViewModel.annuler(reservation.idR)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.sejour_confirmation_non) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
