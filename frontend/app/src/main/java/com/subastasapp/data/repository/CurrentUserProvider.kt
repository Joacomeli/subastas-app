package com.subastasapp.data.repository

import com.subastasapp.data.model.auction.Categoria
import com.subastasapp.data.model.auction.CurrentUser

/**
 * Provee el usuario actual para el gating de pujas (mock de esta entrega).
 *
 * El usuario mock tiene categoría ORO y un medio de pago verificado, de modo que:
 *  - puede pujar en las subastas ESPECIAL y ORO,
 *  - solo puede ver (no pujar) la subasta PLATINO (categoría mayor a la propia).
 *
 * Cuando exista login real contra el backend, este provider se alimentará del perfil del
 * usuario autenticado en lugar de un valor fijo.
 */
object CurrentUserProvider {

    private val registeredUser = CurrentUser(
        id = 1,
        nombre = "Joaquín",
        categoria = Categoria.ORO,
        tieneMedioPagoVerificado = true,
        registrado = true
    )

    /** Devuelve el usuario registrado, o el invitado si [isGuest]. */
    fun current(isGuest: Boolean): CurrentUser =
        if (isGuest) CurrentUser.GUEST else registeredUser
}
