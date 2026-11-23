package com.proyecto.acis

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyecto.acis.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

/** Bases temporales independientes: no modifica la base utilizada por la app. */
@RunWith(AndroidJUnit4::class)
class PersistenciaTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val nombre = "test-${UUID.randomUUID()}.db"
    private lateinit var db: OrdenesDb
    private lateinit var repo: OrdenesRepository

    // Inyecta una base con nombre único para aislar los registros de cada prueba.
    @Before fun preparar() {
        db = OrdenesDb(context, nombre)
        repo = OrdenesRepository(context, db)
    }
    // Cierra y elimina únicamente la base temporal utilizada por esta prueba.
    @After fun limpiar() { db.close(); context.deleteDatabase(nombre) }

    @Test fun accesoAdministrativoSeValidaEnLaBaseYPersiste() = runBlocking {
        // Intenta escrituras directamente en SQLite para comprobar que la protección no depende del botón.
        repo.cargar()
        val admin = "admin@acis.cl"
        val cliente = CuentasPrueba.CLIENTE
        assertFalse(db.actualizarAcceso(cliente, admin, Perfil.CLIENTE, false))
        assertFalse(db.actualizarAcceso(admin, admin, Perfil.ADMINISTRADOR, false))
        assertTrue(db.actualizarAcceso(admin, cliente, Perfil.CLIENTE, false))
        assertNull(db.autenticar(cliente, CuentasPrueba.CLAVE))
        db.close()
        db = OrdenesDb(context, nombre)
        assertFalse(db.leerUsuarios().first { it.correo == cliente }.activo)
        assertTrue(db.actualizarAcceso(admin, cliente, Perfil.CLIENTE, true))
        assertNotNull(db.autenticar(cliente, CuentasPrueba.CLAVE))
        assertEquals(7, db.leer().size)
    }

    @Test fun migracionCincoAgregaAdminSinCambiarClavesNiBorrarDatos() {
        // Construye una base v5 temporal con credenciales propias y una orden que debe sobrevivir.
        val ruta = context.getDatabasePath(nombre)
        ruta.parentFile!!.mkdirs()
        val sal = Claves.nuevaSal()
        val hash = Claves.derivar("ClaveAnterior!", sal)
        SQLiteDatabase.openOrCreateDatabase(ruta, null).use { anterior ->
            anterior.execSQL("CREATE TABLE usuarios (correo TEXT PRIMARY KEY, nombre TEXT NOT NULL, perfil TEXT NOT NULL, sal TEXT NOT NULL, hash TEXT NOT NULL)")
            anterior.execSQL("CREATE TABLE ordenes (id TEXT PRIMARY KEY)")
            anterior.execSQL("INSERT INTO ordenes VALUES ('OT-CONSERVADA')")
            anterior.execSQL("INSERT INTO usuarios VALUES (?, ?, ?, ?, ?)",
                arrayOf("antiguo@acis.cl", "Anterior", "CLIENTE", sal, hash))
            anterior.version = 5
        }
        // La primera apertura mediante el helper ejecuta la migración antes de autenticar.
        assertNotNull(db.autenticar("antiguo@acis.cl", "ClaveAnterior!"))
        assertEquals(Perfil.ADMINISTRADOR, db.autenticar("admin@acis.cl", CuentasPrueba.CLAVE)?.perfil)
        db.readableDatabase.rawQuery("SELECT id FROM ordenes", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("OT-CONSERVADA", it.getString(0))
        }
        db.readableDatabase.rawQuery("SELECT sal, hash FROM usuarios WHERE correo = 'antiguo@acis.cl'", null).use {
            assertTrue(it.moveToFirst()); assertEquals(sal, it.getString(0)); assertEquals(hash, it.getString(1))
        }
        assertEquals(6, db.readableDatabase.version)
    }

    // Verifica credenciales, consulta parametrizada y conservación de cuentas al reabrir.
    @Test fun cuentasSePersistenYRechazanClaveIncorrecta() = runBlocking {
        repo.cargar()
        CuentasPrueba.usuarios.forEach {
            assertEquals(it, repo.autenticar(" ${it.correo.uppercase()} ", CuentasPrueba.CLAVE))
        }
        assertNull(repo.autenticar("cliente@acis.cl", "incorrecta"))
        assertNull(repo.autenticar("' OR 1=1 --", CuentasPrueba.CLAVE))
        db.close()
        db = OrdenesDb(context, nombre)
        assertEquals(Perfil.TECNICO, db.autenticar("tecnico@acis.cl", CuentasPrueba.CLAVE)?.perfil)
    }

    // Recorre transiciones válidas e inválidas y comprueba el cierre leyendo SQLite otra vez.
    @Test fun cicloDeOrdenRechazaSaltosYConservaDatosAlReabrir() = runBlocking {
        repo.cargar()
        val id = repo.crear("Local nuevo", "Equipo", "Mantención", "18/11/2026 10:00",
            TipoOt.PREVENTIVA, PrioridadOt.MEDIA, "cliente2@acis.cl")!!
        assertFalse(repo.aprobar(id))
        assertFalse(repo.aceptar(id))
        assertTrue(repo.asignar(id))
        assertFalse(repo.asignar(id))
        assertTrue(repo.aceptar(id))
        assertTrue(repo.iniciarVisita(id, null))
        assertTrue(repo.guardarBorrador(id, "Diagnóstico", "Actividad", ""))
        assertFalse(repo.enviarInforme(id, " ", "Actividad", ""))
        assertTrue(repo.enviarInforme(id, "Diagnóstico", "Actividad", ""))
        assertTrue(repo.observar(id, "Falta una medición"))
        assertFalse(repo.aprobar(id))
        assertTrue(repo.responderObservacion(id, "Medición entregada"))
        assertTrue(repo.aprobar(id))
        assertFalse(repo.iniciarVisita(id, null))
        db.close()
        db = OrdenesDb(context, nombre)
        val orden = db.leer().single { it.id == id }
        assertEquals(EstadoOt.CERRADA, orden.estado)
        assertEquals("cliente2@acis.cl", orden.clienteCorreo)
        assertEquals("Diagnóstico", orden.diagnostico)
        assertTrue(orden.fechaConformidad.isNotBlank())
    }

    // La marca de inicialización debe preservar una lista vacía tras borrar todos los ejemplos.
    @Test fun borrarTodasNoRecreaSemillasAlReabrir() = runBlocking {
        repo.cargar()
        assertEquals(7, repo.ordenes.value.size)
        repo.ordenes.value.forEach { assertTrue(repo.borrar(it.id)) }
        db.close()
        db = OrdenesDb(context, nombre)
        repo = OrdenesRepository(context, db)
        repo.cargar()
        assertTrue(repo.ordenes.value.isEmpty())
    }

    // Lee directamente la base para confirmar los cambios realizados por el repositorio.
    @Test fun actualizacionPersisteClienteYCampos() = runBlocking {
        repo.cargar()
        assertTrue(repo.actualizarDatos("OT-1001", "Nuevo local", "Nuevo equipo", "Descripción",
            "19/11/2026 12:00", TipoOt.CORRECTIVA, PrioridadOt.ALTA, "cliente2@acis.cl"))
        val guardada = db.leer().single { it.id == "OT-1001" }
        assertEquals("Nuevo local", guardada.local)
        assertEquals("cliente2@acis.cl", guardada.clienteCorreo)
        assertFalse(repo.borrar("inexistente"))
    }

    // Simula v4 y migra a la versión actual (v6), conservando la OT sin inventarle un propietario.
    @Test fun migracionVersionCuatroConservaOrdenesSinExponerlasAUnCliente() {
        // Construye el esquema anterior, con un registro existente, en una base temporal.
        val ruta = context.getDatabasePath(nombre)
        ruta.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(ruta, null).use { anterior ->
            anterior.execSQL("""CREATE TABLE ordenes (
                id TEXT PRIMARY KEY, local TEXT NOT NULL, equipo TEXT NOT NULL, descripcion TEXT NOT NULL,
                fechaProgramada TEXT NOT NULL, tipo TEXT NOT NULL, prioridad TEXT NOT NULL, estado TEXT NOT NULL,
                tecnico TEXT NOT NULL, checkIn TEXT NOT NULL, ubicacionCheckIn TEXT NOT NULL DEFAULT '',
                checkOut TEXT NOT NULL, diagnostico TEXT NOT NULL, actividad TEXT NOT NULL, materiales TEXT NOT NULL,
                observacionCliente TEXT NOT NULL, respuestaTecnico TEXT NOT NULL, fechaConformidad TEXT NOT NULL,
                fotoEvidencia TEXT NOT NULL DEFAULT '', ultimaActualizacion INTEGER NOT NULL DEFAULT 0)""")
            anterior.execSQL("""INSERT INTO ordenes VALUES (
                'OT-LEGACY','Local antiguo','Equipo','Descripción','18/11/2026 10:00',
                'PREVENTIVA','MEDIA','CREADA','','','','','','','','','','','',42)""")
            anterior.version = 4
        }
        val orden = db.leer().single()
        assertEquals("OT-LEGACY", orden.id)
        assertEquals("Local antiguo", orden.local)
        assertEquals(42L, orden.ultimaActualizacion)
        assertEquals("", orden.clienteCorreo)
        assertTrue(db.estaInicializada())
        assertNotNull(db.autenticar("planificador@acis.cl", CuentasPrueba.CLAVE))
    }
}
