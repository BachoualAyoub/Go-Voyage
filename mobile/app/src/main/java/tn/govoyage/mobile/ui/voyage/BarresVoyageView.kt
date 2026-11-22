package tn.govoyage.mobile.ui.voyage

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Petit graphique natif accessible, calcule uniquement a partir des donnees serveur. */
class BarresVoyageView(context: Context, private val valeurs: List<Pair<String,Int>>) : View(context) {
    private val pinceau=Paint(Paint.ANTI_ALIAS_FLAG)
    private var progression=0f
    private val animation=ValueAnimator.ofFloat(0f,1f).apply {
        duration=650
        addUpdateListener { progression=it.animatedValue as Float; invalidate() }
    }
    init {
        contentDescription=valeurs.joinToString(". ") { it.first+" : "+it.second+" réservations" }
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    /** Anime l'arrivee des valeurs si les animations systeme sont actives. */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if(ValueAnimator.areAnimatorsEnabled()) animation.start() else progression=1f
    }
    /** Libere l'animation lorsque l'ecran quitte la navigation. */
    override fun onDetachedFromWindow() { animation.cancel(); super.onDetachedFromWindow() }
    /** Dessine des barres dont les longueurs partagent la meme echelle. */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val maximum=valeurs.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
        for((i,v) in valeurs.withIndex()) {
            val y=35f+i*130
            pinceau.color=Color.rgb(31,76,61); pinceau.textSize=32f
            canvas.drawText(v.first+" · "+v.second,0f,y,pinceau)
            pinceau.color=Color.rgb(222,236,226)
            canvas.drawRoundRect(0f,y+20,width.toFloat(),y+38,12f,12f,pinceau)
            pinceau.color=Color.rgb(5,173,152)
            canvas.drawRoundRect(0f,y+20,width*v.second.toFloat()/maximum*progression,y+38,12f,12f,pinceau)
        }
    }
}
