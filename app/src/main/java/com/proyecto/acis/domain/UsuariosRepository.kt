package com.proyecto.acis.domain

import com.proyecto.acis.data.Perfil
import com.proyecto.acis.data.Usuario
import kotlinx.coroutines.flow.StateFlow

/** Resultado explícito para que la interfaz distinga validación, autorización y duplicados. */
enum class ResultadoRegistro { CREADO, CORREO_DUPLICADO, NOMBRE_TECNICO_DUPLICADO, NO_AUTORIZADO, DATOS_INVALIDOS }

/** Contrato de cuentas: la interfaz no necesita conocer SQLite ni operaciones de órdenes. */
interface UsuariosRepository {
    // La lista es de solo lectura y se observa para refrescar la administración tras una escritura.
    val usuarios: StateFlow<List<Usuario>>
    // Recibe al actor para que la capa de datos confirme autorización al guardar.
    suspend fun registrar(actor: String, nombre: String, correo: String, perfil: Perfil, clave: String): ResultadoRegistro
    suspend fun actualizarAcceso(actor: String, correo: String, perfil: Perfil, activo: Boolean): Boolean
}
