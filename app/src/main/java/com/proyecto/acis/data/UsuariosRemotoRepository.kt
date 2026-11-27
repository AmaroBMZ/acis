package com.proyecto.acis.data

import com.proyecto.acis.data.remote.ActualizarUsuarioRequest
import com.proyecto.acis.data.remote.ApiClient
import com.proyecto.acis.data.remote.RegistroUsuarioRequest
import com.proyecto.acis.data.remote.UsuarioApiDto
import com.proyecto.acis.domain.ResultadoRegistro
import com.proyecto.acis.domain.UsuariosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

/** Administra cuentas directamente en auth-users; la lista se obtiene del servidor. */
class UsuariosRemotoRepository : UsuariosRepository {
    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    override val usuarios = _usuarios.asStateFlow()

    override suspend fun cargar(token: String): Boolean {
        actualizarLista(ApiClient.auth.usuarios(bearer(token)))
        return true
    }

    override suspend fun registrar(
        actor: String,
        token: String,
        rut: String,
        nombre: String,
        correo: String,
        perfil: Perfil,
        clave: String,
    ): ResultadoRegistro = try {
        val creado = ApiClient.auth.registrar(
            bearer(token), RegistroUsuarioRequest(rut.trim(), nombre.trim(), correo.trim(), clave, perfil.name),
        )
        _usuarios.value = (_usuarios.value + creado.aDominio()).distinctBy { it.correo.lowercase() }
        ResultadoRegistro.CREADO
    } catch (error: HttpException) {
        when (error.code()) {
            403 -> ResultadoRegistro.NO_AUTORIZADO
            400 -> ResultadoRegistro.DATOS_INVALIDOS
            409 -> ResultadoRegistro.CORREO_DUPLICADO
            else -> throw error
        }
    }

    override suspend fun actualizarAcceso(
        actor: String,
        token: String,
        correo: String,
        perfil: Perfil,
        activo: Boolean,
    ): Boolean {
        val actual = _usuarios.value.firstOrNull { it.correo.equals(correo, ignoreCase = true) } ?: return false
        val id = actual.idRemoto ?: return false
        val actualizado = ApiClient.auth.actualizarUsuario(
            bearer(token), id,
            ActualizarUsuarioRequest(actual.rut, actual.nombre, actual.correo, perfil.name, activo),
        )
        reemplazar(actualizado)
        return true
    }

    private fun actualizarLista(lista: List<UsuarioApiDto>) {
        _usuarios.value = lista.map { it.aDominio() }
    }

    private fun reemplazar(dto: UsuarioApiDto) {
        val actualizado = dto.aDominio()
        _usuarios.value = _usuarios.value.map { if (it.idRemoto == actualizado.idRemoto) actualizado else it }
    }

    private fun UsuarioApiDto.aDominio() = Usuario(
        correo = email,
        nombre = nombre,
        perfil = Perfil.valueOf(rol),
        activo = activo,
        idRemoto = id,
        rut = rut,
    )

    private fun bearer(token: String) = "Bearer $token"
}
