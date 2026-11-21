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

    // Simula v4 y comprueba que migrar a v5 conserva la OT sin inventarle un propietario.
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
