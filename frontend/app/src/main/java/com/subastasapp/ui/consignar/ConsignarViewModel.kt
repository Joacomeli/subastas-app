package com.subastasapp.ui.consignar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subastasapp.data.model.*
import com.subastasapp.data.repository.UserRepository
import com.subastasapp.ui.auth.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsignarViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _consignaciones = MutableStateFlow<UiState<List<ConsignacionResponse>>>(UiState.Idle)
    val consignaciones: StateFlow<UiState<List<ConsignacionResponse>>> = _consignaciones.asStateFlow()

    private val _crearState = MutableStateFlow<UiState<ConsignacionResponse>>(UiState.Idle)
    val crearState: StateFlow<UiState<ConsignacionResponse>> = _crearState.asStateFlow()

    private val _confirmarState = MutableStateFlow<UiState<ConsignacionResponse>>(UiState.Idle)
    val confirmarState: StateFlow<UiState<ConsignacionResponse>> = _confirmarState.asStateFlow()

    private val _notificaciones = MutableStateFlow<UiState<List<NotificacionResponse>>>(UiState.Idle)
    val notificaciones: StateFlow<UiState<List<NotificacionResponse>>> = _notificaciones.asStateFlow()

    fun cargarConsignaciones() {
        viewModelScope.launch {
            _consignaciones.value = UiState.Loading
            try {
                val response = repository.getConsignaciones()
                if (response.isSuccessful) {
                    _consignaciones.value = UiState.Success(response.body() ?: emptyList())
                } else {
                    _consignaciones.value = UiState.Error(response.message() ?: "Error desconocido")
                }
            } catch (e: Exception) {
                _consignaciones.value = UiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    fun crearConsignacion(nombre: String, descripcion: String, fotos: String?, categoria: String) {
        viewModelScope.launch {
            _crearState.value = UiState.Loading
            try {
                val request = CrearConsignacionRequest(
                    nombre = nombre,
                    descripcion = descripcion,
                    fotos = fotos,
                    categoria = categoria
                )
                val response = repository.crearConsignacion(request)
                if (response.isSuccessful && response.body() != null) {
                    _crearState.value = UiState.Success(response.body()!!)
                    cargarConsignaciones()
                } else {
                    _crearState.value = UiState.Error(response.message() ?: "Error al enviar la consignación")
                }
            } catch (e: Exception) {
                _crearState.value = UiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    fun resetCrearState() {
        _crearState.value = UiState.Idle
    }

    fun resetConfirmarState() {
        _confirmarState.value = UiState.Idle
    }

    fun confirmarConsignacion(id: Long, aceptar: Boolean, devolucionFisica: Boolean? = null) {
        viewModelScope.launch {
            _confirmarState.value = UiState.Loading
            try {
                val request = ConfirmarConsignacionRequest(
                    aceptar = aceptar,
                    devolucionFisica = devolucionFisica
                )
                val response = repository.confirmarConsignacion(id, request)
                if (response.isSuccessful && response.body() != null) {
                    _confirmarState.value = UiState.Success(response.body()!!)
                    cargarConsignaciones()
                } else {
                    _confirmarState.value = UiState.Error(response.message() ?: "Error al confirmar la propuesta")
                }
            } catch (e: Exception) {
                _confirmarState.value = UiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    fun cargarNotificaciones() {
        viewModelScope.launch {
            _notificaciones.value = UiState.Loading
            try {
                val response = repository.getNotificaciones()
                if (response.isSuccessful) {
                    _notificaciones.value = UiState.Success(response.body() ?: emptyList())
                } else {
                    _notificaciones.value = UiState.Error(response.message() ?: "Error al cargar notificaciones")
                }
            } catch (e: Exception) {
                _notificaciones.value = UiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    /** Marca todas las notificaciones como leídas (acción del botón en lista). */
    fun marcarTodasLeidas() {
        viewModelScope.launch {
            try {
                repository.marcarTodasLeidas()
                // Reflejamos el cambio sin re-fetchear: actualizamos el flag en memoria.
                val cur = _notificaciones.value
                if (cur is UiState.Success) {
                    _notificaciones.value = UiState.Success(cur.data.map { it.copy(leido = true) })
                }
            } catch (_: Exception) { /* silencioso: la próxima carga lo refleja */ }
        }
    }

    /** Marca una notificación puntual como leída (al tocar la tarjeta). */
    fun marcarLeida(id: Long) {
        viewModelScope.launch {
            try {
                repository.marcarNotificacionLeida(id)
                val cur = _notificaciones.value
                if (cur is UiState.Success) {
                    _notificaciones.value = UiState.Success(cur.data.map {
                        if (it.id == id) it.copy(leido = true) else it
                    })
                }
            } catch (_: Exception) { /* silencioso */ }
        }
    }
}
