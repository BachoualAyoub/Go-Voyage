package tn.govoyage.mobile.ui.sejour

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tn.govoyage.mobile.databinding.ItemHotelBinding
import tn.govoyage.mobile.model.Hotel
import tn.govoyage.mobile.ui.imageDestination

/** Liste des hotels proposes pour une recherche de sejour. */
class HotelAdapter(
    private var hotels: MutableList<Hotel>,
    private val onReserver: (Hotel) -> Unit
) : RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {

    inner class HotelViewHolder(private val binding: ItemHotelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** Remplit une carte avec la photo locale et le tarif calcule. */
        fun bind(hotel: Hotel) {
            binding.imageHotel.setImageResource(imageDestination(hotel.image))
            binding.tvNomHotel.text = hotel.nom
            binding.tvLieuHotel.text = hotel.ville
            binding.tvDescriptionHotel.text = hotel.description
            binding.tvTarifHotel.text = "${hotel.tarifNuit} TND / nuit"
            binding.tvTotalHotel.text = "${hotel.total} TND pour le sejour"
            binding.tvDisponibilite.text = if (hotel.reservable) {
                "${hotel.disponibles} chambres libres"
            } else {
                "Indisponible pour ces dates"
            }
            binding.boutonReserver.isEnabled = hotel.reservable
            binding.boutonReserver.setOnClickListener { onReserver(hotel) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val binding = ItemHotelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HotelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        holder.bind(hotels[position])
    }

    override fun getItemCount() = hotels.size

    /** Remplace les offres apres une recherche ou un retour au cache. */
    fun updateList(nouvelleListe: List<Hotel>) {
        hotels.clear()
        hotels.addAll(nouvelleListe)
        notifyDataSetChanged()
    }
}
