package tn.govoyage.mobile.lite.ui.destination

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tn.govoyage.mobile.lite.R
import tn.govoyage.mobile.lite.databinding.ItemDestinationBinding
import tn.govoyage.mobile.lite.model.Destination

/** Traduit les categories du catalogue en libelles francais. */
fun libelleAmbiance(ambiance: String) = when (ambiance) {
    "MER" -> "Bord de mer"
    "CULTURE" -> "Culture"
    "NATURE" -> "Nature"
    else -> ambiance
}

/** Liste le catalogue de destinations dans l'ecran Explorer. */
class DestinationAdapter(
    private var destinations: MutableList<Destination>,
    private val onItemClicked: (Destination) -> Unit
) : RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder>() {

    inner class DestinationViewHolder(private val binding: ItemDestinationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(destination: Destination) {
            binding.tvAmbiance.text = libelleAmbiance(destination.ambiance)
            binding.tvNom.text = destination.nom
            binding.tvLieu.text = "${destination.ville} · ${destination.region}"
            binding.tvAccroche.text = destination.accroche
            binding.tvTarif.text = "${destination.tarifJourTnd} TND / jour  ·  Voir la destination"

            binding.root.setOnClickListener {
                onItemClicked(destination)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        val binding = ItemDestinationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DestinationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        holder.bind(destinations[position])
    }

    override fun getItemCount() = destinations.size

    /** Remplace la liste affichee, comme au TP5. */
    fun updateList(nouvelleListe: List<Destination>) {
        destinations.clear()
        destinations.addAll(nouvelleListe)
        notifyDataSetChanged()
    }
}
