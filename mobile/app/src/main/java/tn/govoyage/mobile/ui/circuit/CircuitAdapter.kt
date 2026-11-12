package tn.govoyage.mobile.ui.circuit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tn.govoyage.mobile.R
import tn.govoyage.mobile.databinding.ItemCircuitBinding
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.ui.imageCircuit

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
            binding.imageCircuit.setImageResource(imageCircuit(circuit))
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

    fun updateList(newList: List<Circuit>) {
        this.circuits.clear()
        this.circuits.addAll(newList)
        notifyDataSetChanged()
    }
}
