package com.SaludUnificada.Esu.config;

import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.repositorio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class EsuInitialDataSeeder implements CommandLineRunner {

    private final UsuarioRepositorio usuarioRepositorio;
    private final Environment environment;
    private final PacienteRepositorio pacienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final EspecialidadRepositorio especialidadRepositorio;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private CentroSaludRepositorio centroSaludRepositorio;

    @Autowired(required = false)
    private DisponibilidadProfesionalRepositorio disponibilidadRepositorio;

    @Autowired(required = false)
    private TurnoRepositorio turnoRepositorio;

    @Autowired(required = false)
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Autowired(required = false)
    private RecetaDigitalRepositorio recetaDigitalRepositorio;

    @Autowired(required = false)
    private EstudioAdjuntoRepositorio estudioAdjuntoRepositorio;

    @Value("${app.demo.seed-accounts:false}")
    private boolean seedDemoAccounts;

    public static final String[] ESPECIALIDADES_CANONICAS = {
            "Pediatría", "Clínica Médica", "Traumatología", "Ginecología y Obstetricia",
            "Psiquiatría", "Urología", "Neurología", "Cardiología", "Hemoterapia",
            "Fisiatría", "Oftalmología", "Diabetología", "Cirugía General", "Psicología",
            "Odontología", "Trabajo Social", "Nutrición", "Fonoaudiología", "Kinesiología",
            "Fisioterapia", "Psicomotricidad", "Psicopedagogía", "Dermatología",
            "Endocrinología", "Flebología", "Gastroenterología", "Oncología", "Otorrinolaringología (ORL)"
    };

    @Override
    @Transactional
    public void run(String... args) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isLocalOrDev = activeProfiles.contains("local") || activeProfiles.contains("dev");

        if (!isLocalOrDev && !seedDemoAccounts) {
            log.info("Carga de cuentas semilla omitida (app.demo.seed-accounts=false).");
            return;
        }

        try {
            ejecutarSeederInterno();
        } catch (Exception ex) {
            log.warn("Aviso en EsuInitialDataSeeder (no bloqueante para el arranque del servidor): {}", ex.getMessage(), ex);
        }
    }

    private void ejecutarSeederInterno() {

        log.info("Iniciando verificación y sincronización idempotente de centros, especialidades, profesionales y cuentas...");

        // 1. Sincronizar los 4 centros de salud activos (y desactivar antiguos)
        sincronizarCentrosSalud();

        // 2. Administrador general y cuentas institucionales de los centros
        crearUsuarioSiNoExiste("admin@esu.salud.gob.ar", "Admin123!", "ADMIN");
        crearUsuarioSiNoExiste("admin@esu.gob.ar", "Admin123!", "ADMIN");
        crearUsuarioSiNoExiste("crespo.institucional@esu.salud.gob.ar", "Crespo123!", "RECEPCIONISTA");
        crearUsuarioSiNoExiste("cabrera.institucional@esu.salud.gob.ar", "Cabrera123!", "RECEPCIONISTA");
        crearUsuarioSiNoExiste("diagnostico.institucional@esu.salud.gob.ar", "Diag123!", "RECEPCIONISTA");
        crearUsuarioSiNoExiste("mariaisabel.institucional@esu.salud.gob.ar", "MariaIsabel123!", "RECEPCIONISTA");
        crearUsuarioSiNoExiste("recepcion@esu.gob.ar", "Recepcion123!", "RECEPCIONISTA");
        crearUsuarioSiNoExiste("farmacia@esu.gob.ar", "Farmacia123!", "FARMACIA");

        // 3. Catálogo canónico de 28 especialidades de consultorio
        Map<String, Especialidad> mapaEspecialidades = new HashMap<>();
        for (String nombreEsp : ESPECIALIDADES_CANONICAS) {
            Especialidad esp = sincronizarEspecialidad(nombreEsp, "CONSULTA");
            mapaEspecialidades.put(nombreEsp, esp);
        }

        // 4. Profesionales médicos multi-centro con agendas no solapadas
        sincronizarProfesionalesYAgendas(mapaEspecialidades);

        // 5. Pacientes de prueba
        Paciente juan = crearPacienteSiNoExiste("Juan", "Pérez", "35123456", "AF-100234", "Apross", "juan.perez@salud.gob.ar", "Paciente123!", 1L, "Hospital Provincial Dr. Aurelio Crespo");
        Paciente ana = crearPacienteSiNoExiste("Ana", "Torres", "28987654", "AF-802341", "PAMI", "ana.torres@salud.gob.ar", "Paciente123!", 2L, "Clínica Cabrera");
        Paciente roberto = crearPacienteSiNoExiste("Roberto", "Gómez", "22345678", "AF-504932", "OSECAC", "roberto.gomez@salud.gob.ar", "Paciente123!", 5L, "Sanatorio María Isabel");

        // Cuenta desarrollador
        sincronizarCuentaDev(mapaEspecialidades.get("Clínica Médica"));

        // 6. Turnos, evoluciones y recetas de prueba
        sincronizarTurnosYDatosClinicosPrueba(juan, ana, roberto);

        log.info("Sincronización de centros, especialidades y datos semilla completada con éxito.");
    }

    private void sincronizarCentrosSalud() {
        if (centroSaludRepositorio == null) return;

        sincronizarCentro(1L, "Hospital Provincial Dr. Aurelio Crespo",
                "HOSPITAL_PUBLICO",
                "Sarmiento 1250, Cruz del Eje, Córdoba", "03549-422222",
                true, true, true);

        sincronizarCentro(2L, "Clínica Cabrera",
                "CLINICA_PRIVADA",
                "San Martín 450, Cruz del Eje, Córdoba", "03549-423333",
                true, true, true);

        sincronizarCentro(3L, "Centro de Diagnóstico por Imágenes Cruz del Eje",
                "CENTRO_DIAGNOSTICO",
                "Mitre 820, Cruz del Eje, Córdoba", "03549-424444",
                true, false, true);

        sincronizarCentro(5L, "Sanatorio María Isabel",
                "SANATORIO_PRIVADO",
                "Pte. Perón 320, Cruz del Eje, Córdoba", "03549-425555",
                true, true, false);

        // Desactivar centro antiguo ID 4 (Dispensario Santa Rosa) si existiera en BD
        if (centroSaludRepositorio.existsById(4L)) {
            CentroSalud c4 = centroSaludRepositorio.findById(4L).get();
            c4.setActivo(false);
            c4.setPermiteConsultas(false);
            c4.setPermiteEstudios(false);
            centroSaludRepositorio.save(c4);
        }
    }

    private void sincronizarCentro(Long id, String nombre, String tipo, String direccion, String telefono,
                                   boolean activo, boolean permiteConsultas, boolean permiteEstudios) {
        CentroSalud c = centroSaludRepositorio.findById(id).orElseGet(() -> {
            CentroSalud nuevo = new CentroSalud();
            nuevo.setId(id);
            return nuevo;
        });
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setDireccion(direccion);
        c.setTelefono(telefono);
        c.setActivo(activo);
        c.setPermiteConsultas(permiteConsultas);
        c.setPermiteEstudios(permiteEstudios);
        centroSaludRepositorio.save(c);
    }

    private Especialidad sincronizarEspecialidad(String nombre, String categoria) {
        Especialidad esp = especialidadRepositorio.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Especialidad nueva = new Especialidad();
            nueva.setNombre(nombre);
            return nueva;
        });
        esp.setNombre(nombre);
        esp.setNombreCanonico(nombre);
        esp.setCategoria(categoria);
        return especialidadRepositorio.save(esp);
    }

    private void sincronizarProfesionalesYAgendas(Map<String, Especialidad> esps) {
        List<DocSeed> doctores = List.of(
                // 1. Clínica Médica
                new DocSeed("María Elena", "Fernández", "MP 28.451", "maria.fernandez@esu.salud.gob.ar", "Clínica Médica",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(1L, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                new DocSeed("Roberto Carlos", "Martínez", "MP 19.324", "roberto.martinez@esu.salud.gob.ar", "Clínica Médica",
                        Set.of(1L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                new DocSeed("Florencia", "Castro", "MP 31.220", "florencia.castro@esu.salud.gob.ar", "Clínica Médica",
                        Set.of(2L, 5L),
                        List.of(new DispSeed(2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                                new DispSeed(2L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(5L, DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                // 2. Pediatría
                new DocSeed("Laura Silvina", "Gómez", "MP 33.109", "laura.gomez@esu.salud.gob.ar", "Pediatría",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(1L, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)))),

                new DocSeed("Esteban", "Álvarez", "MP 24.890", "esteban.alvarez@esu.salud.gob.ar", "Pediatría",
                        Set.of(1L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                // 3. Cardiología
                new DocSeed("Alejandro", "Rossi", "MP 15.670", "alejandro.rossi@esu.salud.gob.ar", "Cardiología",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(5L, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                new DocSeed("Mariana", "Lucero", "MP 27.810", "mariana.lucero@esu.salud.gob.ar", "Cardiología",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                // 4. Traumatología
                new DocSeed("Valeria", "Benítez", "MP 29.544", "valeria.benitez@esu.salud.gob.ar", "Traumatología",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                new DocSeed("Hugo", "Acosta", "MP 18.905", "hugo.acosta@esu.salud.gob.ar", "Traumatología",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)))),

                // 5. Ginecología y Obstetricia
                new DocSeed("Silvina", "Torres", "MP 22.188", "silvina.torres@esu.salud.gob.ar", "Ginecología y Obstetricia",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(1L, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                new DocSeed("Fernando", "Molina", "MP 25.430", "fernando.molina@esu.salud.gob.ar", "Ginecología y Obstetricia",
                        Set.of(1L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                // 6. Oftalmología
                new DocSeed("Gustavo", "Navarro", "MP 20.401", "gustavo.navarro@esu.salud.gob.ar", "Oftalmología",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(5L, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                // 7. Odontología
                new DocSeed("Patricia", "Morales", "MP 12.873", "patricia.morales@esu.salud.gob.ar", "Odontología",
                        Set.of(1L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(1L, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                // 8. Dermatología
                new DocSeed("Beatriz", "Suárez", "MP 26.790", "beatriz.suarez@esu.salud.gob.ar", "Dermatología",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                // 9. Neurología
                new DocSeed("Claudio", "Peralta", "MP 16.920", "claudio.peralta@esu.salud.gob.ar", "Neurología",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                // 10. Urología
                new DocSeed("Javier", "Domínguez", "MP 21.340", "javier.dominguez@esu.salud.gob.ar", "Urología",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(5L, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                // 11. Cirugía General
                new DocSeed("Martín", "Vargas", "MP 17.512", "martin.vargas@esu.salud.gob.ar", "Cirugía General",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)))),

                // 12. Gastroenterología
                new DocSeed("Marcela", "Ibarra", "MP 23.411", "marcela.ibarra@esu.salud.gob.ar", "Gastroenterología",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                // 13. Endocrinología
                new DocSeed("Gabriela", "Sánchez", "MP 28.190", "gabriela.sanchez@esu.salud.gob.ar", "Endocrinología",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                                new DispSeed(2L, DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                // 14. Kinesiología
                new DocSeed("Gonzalo", "Romero", "MP 9.420", "gonzalo.romero@esu.salud.gob.ar", "Kinesiología",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(1L, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                // 15. Nutrición
                new DocSeed("Romina", "Paredes", "MP 11.230", "romina.paredes@esu.salud.gob.ar", "Nutrición",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)))),

                // 16. Otorrinolaringología (ORL)
                new DocSeed("Andrés", "Correa", "MP 19.880", "andres.correa@esu.salud.gob.ar", "Otorrinolaringología (ORL)",
                        Set.of(1L, 2L),
                        List.of(new DispSeed(1L, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(2L, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)))),

                // 17. Psiquiatría
                new DocSeed("Carolina", "Ramos", "MP 27.810-B", "carolina.ramos@esu.salud.gob.ar", "Psiquiatría",
                        Set.of(1L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                                new DispSeed(5L, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))),

                // 18. Psicología
                new DocSeed("Lucía", "Albarracín", "MP 14.390", "lucia.albarracin@esu.salud.gob.ar", "Psicología",
                        Set.of(1L, 2L, 5L),
                        List.of(new DispSeed(1L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                                new DispSeed(2L, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                                new DispSeed(5L, DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(18, 0))))
        );

        for (DocSeed ds : doctores) {
            try {
                Especialidad esp = esps.get(ds.especialidad);
                Set<Especialidad> espSet = esp != null ? Set.of(esp) : Set.of();
                Profesional prof = crearOActualizarProfesional(ds.nombre, ds.apellido, ds.matricula, ds.email, "Medico123!", espSet, ds.centrosIds);

                if (disponibilidadRepositorio != null && prof != null) {
                    for (DispSeed disp : ds.disponibilidades) {
                        crearDisponibilidadSiNoExiste(prof, disp.centroId, disp.dia, disp.inicio, disp.fin, 30);
                    }
                }
            } catch (Exception ex) {
                log.warn("Aviso no fatal sincronizando agenda de doctor {}: {}", ds.matricula, ex.getMessage());
            }
        }
    }

    private void crearDisponibilidadSiNoExiste(Profesional profesional, Long centroId, DayOfWeek dia, LocalTime inicio, LocalTime fin, int intervalo) {
        List<DisponibilidadProfesional> existentes = disponibilidadRepositorio.findByProfesionalIdAndDiaSemanaAndActivoTrue(profesional.getId(), dia);
        boolean yaExiste = existentes.stream().anyMatch(d -> Objects.equals(d.getCentroSaludId(), centroId) && d.getHoraInicio().equals(inicio));
        if (!yaExiste) {
            DisponibilidadProfesional dp = DisponibilidadProfesional.builder()
                    .profesional(profesional)
                    .centroSaludId(centroId)
                    .diaSemana(dia)
                    .horaInicio(inicio)
                    .horaFin(fin)
                    .intervaloMinutos(intervalo)
                    .activo(true)
                    .build();
            disponibilidadRepositorio.save(dp);
        }
    }

    private Profesional crearOActualizarProfesional(String nombre, String apellido, String matricula, String email, String password, Set<Especialidad> especialidades, Set<Long> centrosIds) {
        try {
            Usuario u = crearUsuarioSiNoExiste(email, password, "PROFESIONAL");
            Optional<Profesional> opt = profesionalRepositorio.findByMatricula(matricula)
                    .or(() -> profesionalRepositorio.findByUsuarioEmail(email));

            Profesional p = opt.orElseGet(Profesional::new);
            p.setNombre(nombre);
            p.setApellido(apellido);
            p.setMatricula(matricula);
            p.setTelefono("3549-420000");
            p.setEstadoActivo(true);
            p.setUsuario(u);
            p.setEspecialidades(especialidades);
            p.setCentrosSaludIds(centrosIds);
            return profesionalRepositorio.save(p);
        } catch (Exception ex) {
            log.warn("Aviso seeder no fatal para profesional {} ({}): {}", matricula, email, ex.getMessage());
            return profesionalRepositorio.findByMatricula(matricula)
                    .or(() -> profesionalRepositorio.findByUsuarioEmail(email))
                    .orElse(null);
        }
    }

    private Usuario crearUsuarioSiNoExiste(String email, String rawPassword, String rol) {
        return usuarioRepositorio.findByEmail(email).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setRol(rol);
            u.setEstadoActivo(true);
            u.setEmailVerificado(true);
            u.setFechaDeAlta(LocalDate.now());
            return usuarioRepositorio.save(u);
        });
    }

    private Paciente crearPacienteSiNoExiste(String nombre, String apellido, String dni, String nroAfiliado, String obraSocial,
                                             String email, String password, Long refCentroId, String refCentroNombre) {
        Optional<Paciente> existente = pacienteRepositorio.findByDni(dni)
                .or(() -> pacienteRepositorio.findByUsuarioEmail(email));
        if (existente.isPresent()) {
            Paciente p = existente.get();
            p.setCentroSaludReferenciaId(refCentroId);
            p.setCentroSaludReferenciaNombre(refCentroNombre);
            return pacienteRepositorio.save(p);
        }

        Usuario u = crearUsuarioSiNoExiste(email, password, "PACIENTE");
        Paciente pac = new Paciente();
        pac.setNombre(nombre);
        pac.setApellido(apellido);
        pac.setDni(dni);
        pac.setNroAfiliado(nroAfiliado);
        pac.setObraSocial(obraSocial);
        pac.setTelefono("3549-421111");
        pac.setUsuario(u);
        pac.setCentroSaludReferenciaId(refCentroId);
        pac.setCentroSaludReferenciaNombre(refCentroNombre);
        return pacienteRepositorio.save(pac);
    }

    private void sincronizarCuentaDev(Especialidad espGeneral) {
        Usuario devUser = usuarioRepositorio.findByEmail("dev@esu.gob.ar").orElseGet(() -> {
            Usuario u = new Usuario();
            u.setEmail("dev@esu.gob.ar");
            u.setFechaDeAlta(LocalDate.now());
            return u;
        });
        devUser.setPassword(passwordEncoder.encode("Dev12345!"));
        if (devUser.getRol() == null || devUser.getRol().isBlank()) {
            devUser.setRol("ADMIN");
        }
        devUser.setEstadoActivo(true);
        devUser.setEmailVerificado(true);
        devUser = usuarioRepositorio.save(devUser);

        if (pacienteRepositorio.findByUsuarioEmail("dev@esu.gob.ar").isEmpty()) {
            Paciente devPac = new Paciente();
            devPac.setNombre("Desarrollador");
            devPac.setApellido("ESU");
            devPac.setDni("99999999");
            devPac.setNroAfiliado("DEV-99999999");
            devPac.setObraSocial("Apross");
            devPac.setTelefono("3549-420000");
            devPac.setCentroSaludReferenciaId(1L);
            devPac.setCentroSaludReferenciaNombre("Hospital Provincial Dr. Aurelio Crespo");
            devPac.setUsuario(devUser);
            pacienteRepositorio.save(devPac);
        }

        if (profesionalRepositorio.findByUsuarioEmail("dev@esu.gob.ar").isEmpty()) {
            Profesional devProf = new Profesional();
            devProf.setNombre("Carlos");
            devProf.setApellido("Desarrollador");
            devProf.setMatricula("MP-99999");
            devProf.setTelefono("3549-420000");
            devProf.setEstadoActivo(true);
            devProf.setUsuario(devUser);
            if (espGeneral != null) devProf.setEspecialidades(Set.of(espGeneral));
            devProf.setCentrosSaludIds(Set.of(1L, 2L));
            profesionalRepositorio.save(devProf);
        }
    }

    private void sincronizarTurnosYDatosClinicosPrueba(Paciente juan, Paciente ana, Paciente roberto) {
        if (turnoRepositorio == null) return;

        Profesional drGomez = profesionalRepositorio.findByMatricula("MP-45210").orElse(null);
        Profesional draRossi = profesionalRepositorio.findByMatricula("MP-38914").orElse(null);

        LocalDateTime proximoLunes10 = LocalDate.now().plusDays(3).atTime(10, 0);

        // Turno Consulta en Hospital Crespo (ID 1)
        if (drGomez != null && juan != null && !turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(juan.getId(), proximoLunes10, "CANCELADO")) {
            Turno t1 = new Turno();
            t1.setPaciente(juan);
            t1.setProfesional(drGomez);
            t1.setCentroSaludId(1L);
            t1.setCentroSaludNombre("Hospital Provincial Dr. Aurelio Crespo");
            t1.setConsultorioNumero("Consultorio 104");
            t1.setFechaHora(proximoLunes10);
            t1.setEstado("CONFIRMADO");
            t1.setTipoAtencion("CONSULTA");
            t1.setServicio("Clínica Médica");
            t1.setMotivoTurno("Control periódico por hipertensión arterial [DATO DE PRUEBA]");
            t1.setFechaCreacion(LocalDateTime.now().minusDays(1));
            turnoRepositorio.save(t1);
        }

        // Turno Estudio en Centro de Diagnóstico (ID 3) - SIN MÉDICO DE CONSULTORIO
        LocalDateTime fechaEstudioDiag = LocalDate.now().plusDays(4).atTime(9, 30);
        if (juan != null && !turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(juan.getId(), fechaEstudioDiag, "CANCELADO")) {
            Turno tEstudio = new Turno();
            tEstudio.setPaciente(juan);
            tEstudio.setProfesional(null);
            tEstudio.setCentroSaludId(3L);
            tEstudio.setCentroSaludNombre("Centro de Diagnóstico por Imágenes Cruz del Eje");
            tEstudio.setConsultorioNumero("Sala Ecografía 1");
            tEstudio.setFechaHora(fechaEstudioDiag);
            tEstudio.setEstado("CONFIRMADO");
            tEstudio.setTipoAtencion("ESTUDIO");
            tEstudio.setServicio("Ecografía General");
            tEstudio.setMotivoTurno("Ecografía abdominal programada [DATO DE PRUEBA]");
            tEstudio.setFechaCreacion(LocalDateTime.now().minusDays(2));
            turnoRepositorio.save(tEstudio);
        }

        // Turno Consulta en Sanatorio María Isabel (ID 5)
        LocalDateTime fechaSanatorio = LocalDate.now().plusDays(5).atTime(14, 30);
        if (draRossi != null && roberto != null && !turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(roberto.getId(), fechaSanatorio, "CANCELADO")) {
            Turno t2 = new Turno();
            t2.setPaciente(roberto);
            t2.setProfesional(draRossi);
            t2.setCentroSaludId(5L);
            t2.setCentroSaludNombre("Sanatorio María Isabel");
            t2.setConsultorioNumero("Consultorio Cardiología B");
            t2.setFechaHora(fechaSanatorio);
            t2.setEstado("CONFIRMADO");
            t2.setTipoAtencion("CONSULTA");
            t2.setServicio("Cardiología");
            t2.setMotivoTurno("Evaluación cardiológica integral [DATO DE PRUEBA]");
            t2.setFechaCreacion(LocalDateTime.now().minusDays(1));
            turnoRepositorio.save(t2);
        }

        // Evolución médica de prueba
        if (evolucionMedicaRepositorio != null && drGomez != null && juan != null) {
            if (evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(juan.getId()).isEmpty()) {
                EvolucionMedica ev = new EvolucionMedica();
                ev.setPaciente(juan);
                ev.setProfesional(drGomez);
                ev.setCentroSaludId(1L);
                ev.setCentroSaludNombre("Hospital Provincial Dr. Aurelio Crespo");
                ev.setFechaCreacion(LocalDateTime.now().minusDays(15));
                ev.setMotivoConsulta("[DATO DE PRUEBA / SIMULACIÓN] Control clínico ambulatorio");
                ev.setAnamnesis("[DATO DE PRUEBA] Paciente asiste a control de rutina, sin signos de alarma.");
                ev.setDiagnostico("[DATO DE PRUEBA / SIMULACIÓN] Hipertensión esencial (I10)");
                ev.setCodigoCie10("I10");
                ev.setTratamiento("Continuar enalapril 10mg diario. Hábitos higiénico dietéticos con reducción de sodio.");
                evolucionMedicaRepositorio.save(ev);
            }
        }

        // Receta digital de prueba
        if (recetaDigitalRepositorio != null && drGomez != null && juan != null) {
            if (recetaDigitalRepositorio.findByPacienteIdOrderByFechaEmisionDesc(juan.getId()).isEmpty()) {
                RecetaDigital rec = new RecetaDigital();
                rec.setPaciente(juan);
                rec.setProfesional(drGomez);
                rec.setCentroSaludEmisorId(1L);
                rec.setCentroSaludEmisorNombre("Hospital Provincial Dr. Aurelio Crespo");
                rec.setLugarRetiroSugerido("Farmacia Central - Hospital Crespo (Planta Baja)");
                rec.setFechaEmision(LocalDate.now().minusDays(5));
                rec.setFechaVencimiento(LocalDate.now().plusDays(25));
                rec.setDiagnostico("[DATO DE PRUEBA / SIMULACIÓN] Hipertensión arterial I10");
                rec.setCodigoCie10("I10");
                rec.setIndicacionesGenerales("Enalapril 10 mg comprimidos x 30. Tomar 1 comprimido por la mañana.");
                rec.setEstadoReceta("PENDIENTE");
                recetaDigitalRepositorio.save(rec);
            }
        }

        // Estudio adjunto de prueba
        if (estudioAdjuntoRepositorio != null && juan != null) {
            if (estudioAdjuntoRepositorio.findByPacienteIdOrderByFechaCargaDesc(juan.getId()).isEmpty()) {
                EstudioAdjunto est = new EstudioAdjunto();
                est.setPacienteId(juan.getId());
                est.setCentroSaludId(3L);
                est.setCentroSaludNombre("Centro de Diagnóstico por Imágenes Cruz del Eje");
                est.setProfesionalNombre("Servicio de Diagnóstico por Imágenes");
                est.setTipoEstudio("IMAGEN");
                est.setNombreArchivo("ecografia_abdominal_simulada.pdf");
                est.setNombreAlmacenado("simulado_cdi_" + UUID.randomUUID() + ".pdf");
                est.setRutaAlmacenamiento("uploads/estudios/simulado.pdf");
                est.setMimeType("application/pdf");
                est.setTamanioBytes(102400L);
                est.setChecksumSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
                est.setFechaEstudio(LocalDate.now().minusDays(10));
                est.setFechaCarga(LocalDateTime.now().minusDays(10));
                estudioAdjuntoRepositorio.save(est);
            }
        }
    }

    private static class DocSeed {
        final String nombre;
        final String apellido;
        final String matricula;
        final String email;
        final String especialidad;
        final Set<Long> centrosIds;
        final List<DispSeed> disponibilidades;

        DocSeed(String nombre, String apellido, String matricula, String email, String especialidad, Set<Long> centrosIds, List<DispSeed> disponibilidades) {
            this.nombre = nombre;
            this.apellido = apellido;
            this.matricula = matricula;
            this.email = email;
            this.especialidad = especialidad;
            this.centrosIds = centrosIds;
            this.disponibilidades = disponibilidades;
        }
    }

    private static class DispSeed {
        final Long centroId;
        final DayOfWeek dia;
        final LocalTime inicio;
        final LocalTime fin;

        DispSeed(Long centroId, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
            this.centroId = centroId;
            this.dia = dia;
            this.inicio = inicio;
            this.fin = fin;
        }
    }
}
