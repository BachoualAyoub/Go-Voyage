package tn.govoyage.mobile.ui.destination

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.ItemDestinationBinding
import tn.govoyage.mobile.model.Destination
import tn.govoyage.mobile.ui.imageDestination

/** Traduit les categories du catalogue en libelles de navigation. */
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
            binding.imageDestination.setImageResource(imageDestination(destination.code))
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

    fun updateList(newList: List<Destination>) {
        this.destinations.clear()
        this.destinations.addAll(newList)
        notifyDataSetChanged()
    }
}
