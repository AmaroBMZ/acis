package com.proyecto.acis.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Gestor de la base de datos SQLite local para el almacenamiento persistente de OTs sin conexión.
 */
class OrdenesDb(context: Context, nombre: String = "acis_ordenes.db") : SQLiteOpenHelper(context, nombre, null, 5) {
    // Creación de la tabla principal de órdenes de trabajo
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE ordenes (
                id TEXT PRIMARY KEY,
                local TEXT NOT NULL,
                equipo TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                fechaProgramada TEXT NOT NULL,
                tipo TEXT NOT NULL,
                prioridad TEXT NOT NULL,
                estado TEXT NOT NULL,
                tecnico TEXT NOT NULL,
                checkIn TEXT NOT NULL,
                ubicacionCheckIn TEXT NOT NULL DEFAULT '',
                checkOut TEXT NOT NULL,
                diagnostico TEXT NOT NULL,
                actividad TEXT NOT NULL,
                materiales TEXT NOT NULL,
                observacionCliente TEXT NOT NULL,
                respuestaTecnico TEXT NOT NULL,
                fechaConformidad TEXT NOT NULL,
                fotoEvidencia TEXT NOT NULL DEFAULT '',
                ultimaActualizacion INTEGER NOT NULL DEFAULT 0,
                clienteCorreo TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
        crearUsuarios(db)
    }

    // Actualización progresiva del esquema de la base de datos
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ordenes ADD COLUMN fotoEvidencia TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE ordenes ADD COLUMN ultimaActualizacion INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE ordenes ADD COLUMN ubicacionCheckIn TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE ordenes ADD COLUMN clienteCorreo TEXT NOT NULL DEFAULT ''")
            crearUsuarios(db)
            // Una instalación existente no debe recuperar datos demo al quedar vacía.
            db.execSQL("INSERT OR IGNORE INTO configuracion (clave) VALUES ('inicializada')")
        }
    }

    // Crea cuentas demo y una tabla de marcas para recordar la inicialización de los datos.
    private fun crearUsuarios(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuarios (correo TEXT PRIMARY KEY, nombre TEXT NOT NULL, perfil TEXT NOT NULL, sal TEXT NOT NULL, hash TEXT NOT NULL)")
        db.execSQL("CREATE TABLE configuracion (clave TEXT PRIMARY KEY)")
        // Cada cuenta obtiene una sal distinta; SQLite guarda sal y hash, no la clave ingresada.
        CuentasPrueba.usuarios.forEach { usuario ->
            val sal = Claves.nuevaSal()
            db.insertOrThrow("usuarios", null, ContentValues().apply {
                put("correo", usuario.correo); put("nombre", usuario.nombre); put("perfil", usuario.perfil.name)
                put("sal", sal); put("hash", Claves.derivar(CuentasPrueba.CLAVE, sal))
            })
        }
    }

    // Normaliza el correo. El parámetro ? separa el valor del usuario de la consulta SQL.
    fun autenticar(correo: String, clave: String): Usuario? = readableDatabase.query(
        "usuarios", null, "correo = ?", arrayOf(correo.trim().lowercase(java.util.Locale.ROOT)), null, null, null,
    // use cierra el cursor al terminar; moveToFirst comprueba si existe una cuenta.
    ).use { cursor ->
        if (!cursor.moveToFirst() || !Claves.verificar(clave, cursor.valor("sal"), cursor.valor("hash"))) null
        else Usuario(cursor.valor("correo"), cursor.valor("nombre"), Perfil.valueOf(cursor.valor("perfil")))
    }

    // Consulta una marca persistente: una lista vacía no significa que deban recrearse las demos.
    fun estaInicializada(): Boolean = readableDatabase.rawQuery(
        "SELECT clave FROM configuracion WHERE clave = 'inicializada'", null,
    ).use { it.moveToFirst() }

    // Lee la lista completa de órdenes almacenadas en SQLite
    fun leer(): List<OrdenTrabajo> = readableDatabase.query(
        "ordenes", null, null, null, null, null, "id ASC",
    ).use { cursor ->
        // Convierte cada fila del cursor en un objeto Kotlin; los enums desconocidos tienen respaldo.
        buildList {
            while (cursor.moveToNext()) {
                add(OrdenTrabajo(
                    id = cursor.valor("id"),
                    local = cursor.valor("local"),
                    equipo = cursor.valor("equipo"),
                    descripcion = cursor.valor("descripcion"),
                    fechaProgramada = cursor.valor("fechaProgramada"),
                    tipo = TipoOt.entries.firstOrNull { it.name == cursor.valor("tipo") }
                        ?: TipoOt.SIN_ESPECIFICAR,
                    prioridad = PrioridadOt.entries.firstOrNull { it.name == cursor.valor("prioridad") }
                        ?: PrioridadOt.SIN_ESPECIFICAR,
                    estado = EstadoOt.entries.firstOrNull { it.name == cursor.valor("estado") }
                        ?: EstadoOt.CREADA,
                    tecnico = cursor.valor("tecnico"),
                    checkIn = cursor.valor("checkIn"),
                    ubicacionCheckIn = cursor.valor("ubicacionCheckIn"),
                    checkOut = cursor.valor("checkOut"),
                    diagnostico = cursor.valor("diagnostico"),
                    actividad = cursor.valor("actividad"),
                    materiales = cursor.valor("materiales"),
                    observacionCliente = cursor.valor("observacionCliente"),
                    respuestaTecnico = cursor.valor("respuestaTecnico"),
                    fechaConformidad = cursor.valor("fechaConformidad"),
                    fotoEvidencia = cursor.valor("fotoEvidencia"),
                    ultimaActualizacion = cursor.getLong(cursor.getColumnIndexOrThrow("ultimaActualizacion")),
                    clienteCorreo = cursor.valor("clienteCorreo"),
                ))
            }
        }
    }

    // Inserta una nueva orden en la base de datos
    fun insertar(orden: OrdenTrabajo): Boolean = try {
        writableDatabase.insert("ordenes", null, valores(orden)) != -1L
    } catch (_: Exception) {
        false
    }

    // Actualiza los datos de una orden existente validando el estado anterior
    fun actualizar(orden: OrdenTrabajo, estadoAnterior: EstadoOt): Boolean = try {
        writableDatabase.update(
            "ordenes", valores(orden), "id = ? AND estado = ?",
            // Solo actualiza si la fila aún mantiene el estado esperado, evitando una transición obsoleta.
            arrayOf(orden.id, estadoAnterior.name),
        ) == 1
    } catch (_: Exception) {
        false
    }

    // Elimina una orden de trabajo de la base de datos local por su identificador
    fun borrar(id: String): Boolean = try {
        writableDatabase.delete("ordenes", "id = ?", arrayOf(id)) == 1
    } catch (_: Exception) {
        false
    }

    // Guarda una lista de órdenes dentro de una transacción atómica
    fun guardarTodas(ordenes: List<OrdenTrabajo>): Boolean {
        val db = writableDatabase
        return try {
            // Agrupa inserciones y marca de inicialización: se confirman juntas o se revierten.
            db.beginTransaction()
            ordenes.forEach { orden ->
                if (db.insert("ordenes", null, valores(orden)) == -1L) error("No se pudo guardar la OT")
            }
            db.execSQL("INSERT OR IGNORE INTO configuracion (clave) VALUES ('inicializada')")
            db.setTransactionSuccessful()
            true
        } catch (_: Exception) {
            false
        } finally {
            // Sin setTransactionSuccessful, endTransaction revierte la transacción.
            if (db.inTransaction()) db.endTransaction()
        }
    }

    // Mapea un objeto OrdenTrabajo a ContentValues para operaciones SQLite
    private fun valores(orden: OrdenTrabajo) = ContentValues().apply {
        put("id", orden.id)
        put("local", orden.local)
        put("equipo", orden.equipo)
        put("descripcion", orden.descripcion)
        put("fechaProgramada", orden.fechaProgramada)
        put("tipo", orden.tipo.name)
        put("prioridad", orden.prioridad.name)
        put("estado", orden.estado.name)
        put("tecnico", orden.tecnico)
        put("checkIn", orden.checkIn)
        put("ubicacionCheckIn", orden.ubicacionCheckIn)
        put("checkOut", orden.checkOut)
        put("diagnostico", orden.diagnostico)
        put("actividad", orden.actividad)
        put("materiales", orden.materiales)
        put("observacionCliente", orden.observacionCliente)
        put("respuestaTecnico", orden.respuestaTecnico)
        put("fechaConformidad", orden.fechaConformidad)
        put("fotoEvidencia", orden.fotoEvidencia)
        put("ultimaActualizacion", orden.ultimaActualizacion)
        put("clienteCorreo", orden.clienteCorreo)
    }

    // Busca la columna por nombre y convierte un valor SQL nulo en una cadena vacía.
    private fun Cursor.valor(campo: String): String = getString(getColumnIndexOrThrow(campo)).orEmpty()
}
