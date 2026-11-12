package tn.govoyage.mobile.lite.ui.circuit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tn.govoyage.mobile.lite.R
import tn.govoyage.mobile.lite.databinding.ItemCircuitBinding
import tn.govoyage.mobile.lite.model.Circuit

/** Liste les circuits proposes et le carnet personnel du voyageur. */
class CircuitAdapter(
    private var circuits: MutableList<Circuit>,
    private val avecActions: Boolean,
    private val onClique: (Circuit) -> Unit,
    private val onModifier: (Circuit) -> Unit,
    private val onSupprimer: (Circuit) -> Unit
) : RecyclerView.Adapter<CircuitAdapter.CircuitViewHolder>() {

    inner class CircuitViewHolder(private val binding: ItemCircuitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(circuit: Circuit) {
            binding.tvDureeEstimation.text = "${circuit.dureeJours} j  ·  ${circuit.estimationTnd} TND estimés"
            binding.tvNomCircuit.text = circuit.nom
            binding.tvItineraire.text = circuit.etapes.joinToString(" → ") { it.nom }
            binding.boutonVoirCircuit.text = if (circuit.modele) {
                binding.root.context.getString(R.string.item_circuit_voir_modele)
            } else {
                binding.root.context.getString(R.string.item_circuit_voir_circuit)
            }
            binding.groupeActions.visibility = if (avecActions) ViewGroup.VISIBLE else ViewGroup.GONE

            binding.root.setOnClickListener {
                onClique(circuit)
            }
            binding.boutonVoirCircuit.setOnClickListener {
                onClique(circuit)
            }
            binding.boutonModifier.setOnClickListener {
                onModifier(circuit)
            }
            binding.boutonSupprimer.setOnClickListener {
                onSupprimer(circuit)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircuitViewHolder {
        val binding = ItemCircuitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CircuitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CircuitViewHolder, position: Int) {
        holder.bind(circuits[position])
    }

    override fun getItemCount() = circuits.size

    /** Remplace la liste affichee, comme au TP5. */
    fun updateList(nouvelleListe: List<Circuit>) {
        circuits.clear()
        circuits.addAll(nouvelleListe)
        notifyDataSetChanged()
    }
}
