package com.SaludUnificada.Esu.config;

import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.EspecialidadRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class EsuInitialDataSeeder implements CommandLineRunner {

    private final UsuarioRepositorio usuarioRepositorio;
    private final org.springframework.core.env.Environment environment;

    @org.springframework.beans.factory.annotation.Value("${app.demo.seed-accounts:false}")
    private boolean seedDemoAccounts;
    private final PacienteRepositorio pacienteRepositorio;
    private final ProfesionalRepositorio profesionalRepositorio;
    private final EspecialidadRepositorio especialidadRepositorio;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        java.util.List<String> activeProfiles = java.util.Arrays.asList(environment.getActiveProfiles());
        boolean isLocalOrDev = activeProfiles.contains("local") || activeProfiles.contains("dev");

        if (!isLocalOrDev && !seedDemoAccounts) {
            log.info("Carga de cuentas semilla de demostración omitida en producción (app.demo.seed-accounts=false).");
            return;
        }

        log.info("Verificando y sincronizando datos semilla de demostración académica (ejecución idempotente por registro)...");

        crearUsuarioSiNoExiste("admin@esu.gob.ar", "Admin123!", "ADMIN");
        crearUsuarioSiNoExiste("recepcion@esu.gob.ar", "Recepcion123!", "RECEPCIONISTA");
        crearUsuarioSiNoExiste("farmacia@esu.gob.ar", "Farmacia123!", "FARMACIA");

        Especialidad espGeneral = crearEspecialidadSiNoExiste("Medicina General");
        Especialidad espCardio = crearEspecialidadSiNoExiste("Cardiología");
        Especialidad espPedia = crearEspecialidadSiNoExiste("Pediatría");
        Especialidad espTrauma = crearEspecialidadSiNoExiste("Traumatología");

        crearProfesionalSiNoExiste("Carlos", "Gómez", "MP-45210", "carlos.gomez@esu.gob.ar", "Medico123!", Set.of(espGeneral));
        crearProfesionalSiNoExiste("María Elena", "Rossi", "MP-38914", "maria.rossi@esu.gob.ar", "Medico123!", Set.of(espCardio));
        crearProfesionalSiNoExiste("Lucas", "Benítez", "MP-51203", "lucas.benitez@esu.gob.ar", "Medico123!", Set.of(espPedia));

        crearPacienteSiNoExiste("Juan", "Pérez", "35123456", "AF-100234", "OSECAC", "juan.perez@salud.gob.ar", "Paciente123!");
        crearPacienteSiNoExiste("Ana", "Torres", "28987654", "AF-802341", "PAMI", "ana.torres@salud.gob.ar", "Paciente123!");

                // Cuenta Desarrollador ESU para testing y superadmin (HU-DEV)
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
            devProf.setEspecialidades(Set.of(espGeneral));
            profesionalRepositorio.save(devProf);
        }

        log.info("Sincronización de datos semilla completada.");
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

    private Especialidad crearEspecialidadSiNoExiste(String nombre) {
        return especialidadRepositorio.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Especialidad esp = new Especialidad();
            esp.setNombre(nombre);
            return especialidadRepositorio.save(esp);
        });
    }

    private void crearProfesionalSiNoExiste(String nombre, String apellido, String matricula, String email, String password, Set<Especialidad> especialidades) {
        if (profesionalRepositorio.findByMatricula(matricula).isPresent() || profesionalRepositorio.findByUsuarioEmail(email).isPresent()) {
            return;
        }
        Usuario u = crearUsuarioSiNoExiste(email, password, "PROFESIONAL");
        Profesional p = new Profesional();
        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setMatricula(matricula);
        p.setTelefono("3549-420000");
        p.setEstadoActivo(true);
        p.setUsuario(u);
        p.setEspecialidades(especialidades);
        profesionalRepositorio.save(p);
    }

    private void crearPacienteSiNoExiste(String nombre, String apellido, String dni, String nroAfiliado, String obraSocial, String email, String password) {
        if (pacienteRepositorio.findByDni(dni).isPresent() || pacienteRepositorio.existsByUsuarioEmail(email)) {
            return;
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
        pacienteRepositorio.save(pac);
    }
}
