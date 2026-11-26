package tn.govoyage.mobile.data.repository

import retrofit2.HttpException
import tn.govoyage.mobile.data.remote.RetrofitInstance
import tn.govoyage.mobile.model.Circuit
import tn.govoyage.mobile.model.CircuitRequest
import tn.govoyage.mobile.model.EtapeCircuit
import tn.govoyage.mobile.model.EtapeRequest

class CircuitRepository {

    private val api = RetrofitInstance.circuitApi

    suspend fun lister(): List<Circuit> {
        val reponse = api.lister()
        if (!reponse.isSuccessful) throw HttpException(reponse)
        return reponse.body().orEmpty()
    }

    suspend fun detail(id: Long): Circuit {
        val reponse = api.detail(id)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        return reponse.body() ?: throw IllegalStateException("Circuit introuvable.")
    }

    suspend fun sauver(circuit: Circuit?, nom: String, etapes: List<EtapeCircuit>): Circuit {
        val demande = CircuitRequest(
            nom = nom.trim(),
            budgetTnd = null,
            etapes = etapes.map { EtapeRequest(it.destinationId, it.jours) },
            version = circuit?.version
        )
        val reponse = if (circuit == null) api.creer(demande) else api.modifier(circuit.id, demande)
        if (!reponse.isSuccessful) throw HttpException(reponse)
        return reponse.body() ?: throw IllegalStateException("Reponse circuit vide.")
    }

    suspend fun supprimer(id: Long) {
        val reponse = api.supprimer(id)
        if (!reponse.isSuccessful) throw HttpException(reponse)
    }
}
