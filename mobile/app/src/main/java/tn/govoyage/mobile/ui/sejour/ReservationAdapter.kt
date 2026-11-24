package tn.govoyage.mobile.ui.sejour

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tn.govoyage.mobile.databinding.ItemReservationBinding
import tn.govoyage.mobile.model.Reservation

/** Carnet des reservations affiche dans l'ecran Mes reservations. */
class ReservationAdapter(
    private var reservations: MutableList<Reservation>,
    private val onAnnuler: (Reservation) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {

    inner class ReservationViewHolder(private val binding: ItemReservationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** Affiche une reservation et son bouton d'annulation. */
        fun bind(reservation: Reservation) {
            binding.tvStatutReservation.text = reservation.statut
            binding.tvNomReservation.text = reservation.hotelNom
            binding.tvDatesReservation.text = "${reservation.ville} · ${reservation.arrivee} -> ${reservation.depart}"
            binding.tvDetailReservation.text =
                "${reservation.voyageurs} voyageurs · ${reservation.chambres} chambre(s)"
            binding.tvMontantReservation.text = "${reservation.montant} TND"
            binding.boutonAnnulerReservation.isEnabled = reservation.statut == "CONFIRMEE"
            binding.boutonAnnulerReservation.setOnClickListener { onAnnuler(reservation) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val binding = ItemReservationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReservationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(reservations[position])
    }

    override fun getItemCount() = reservations.size

    /** Remplace le carnet apres une lecture ou une annulation. */
    fun updateList(nouvelleListe: List<Reservation>) {
        reservations.clear()
        reservations.addAll(nouvelleListe)
        notifyDataSetChanged()
    }
}
