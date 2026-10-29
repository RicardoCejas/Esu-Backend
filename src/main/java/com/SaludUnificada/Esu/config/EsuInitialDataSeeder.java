package com.SaludUnificada.Esu.config;



import com.SaludUnificada.Esu.entidad.Especialidad;

import com.SaludUnificada.Esu.entidad.Paciente;

import com.SaludUnificada.Esu.entidad.Profesional;

import com.SaludUnificada.Esu.entidad.Usuario;

import com.SaludUnificada.Esu.repositorio.EspecialidadRepositorio;

import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;

import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;

import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.CommandLineRunner;

import org.springframework.core.annotation.Order;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDate;

import java.util.Set;



@Component

@Order(10)

public class EsuInitialDataSeeder implements CommandLineRunner {



    private static final Logger log = LoggerFactory.getLogger(EsuInitialDataSeeder.class);



    @Autowired

    private UsuarioRepositorio usuarioRepositorio;



    @Autowired

    private PacienteRepositorio pacienteRepositorio;



    @Autowired

    private ProfesionalRepositorio profesionalRepositorio;



    @Autowired

    private EspecialidadRepositorio especialidadRepositorio;



    @Autowired

    private PasswordEncoder passwordEncoder;



    @Override

    @Transactional

    public void run(String... args) {

        if (usuarioRepositorio.count() > 0) {

            return;

        }



        log.info("Iniciando DataSeeder: Sembrando usuarios, especialidades y profesionales iniciales...");




        Usuario admin = new Usuario();

        admin.setEmail("admin@esu.gob.ar");

        admin.setPassword(passwordEncoder.encode("Admin123!"));

        admin.setRol("ADMIN");

        admin.setEstadoActivo(true);

        admin.setFechaDeAlta(LocalDate.now());

        usuarioRepositorio.save(admin);




        Usuario recepcion = new Usuario();

        recepcion.setEmail("recepcion@esu.gob.ar");

        recepcion.setPassword(passwordEncoder.encode("Recepcion123!"));

        recepcion.setRol("RECEPCIONISTA");

        recepcion.setEstadoActivo(true);

        recepcion.setFechaDeAlta(LocalDate.now());

        usuarioRepositorio.save(recepcion);




        Especialidad espGeneral = new Especialidad(); espGeneral.setNombre("Medicina General"); espGeneral = especialidadRepositorio.save(espGeneral);

        Especialidad espCardio = new Especialidad(); espCardio.setNombre("Cardiología"); espCardio = especialidadRepositorio.save(espCardio);

        Especialidad espPedia = new Especialidad(); espPedia.setNombre("Pediatría"); espPedia = especialidadRepositorio.save(espPedia);

        Especialidad espTrauma = new Especialidad(); espTrauma.setNombre("Traumatología"); espTrauma = especialidadRepositorio.save(espTrauma);




        crearProfesional("Carlos", "Gómez", "MP-45210", "carlos.gomez@esu.gob.ar", "Medico123!", Set.of(espGeneral));

        crearProfesional("María Elena", "Rossi", "MP-38914", "maria.rossi@esu.gob.ar", "Medico123!", Set.of(espCardio));

        crearProfesional("Lucas", "Benítez", "MP-51203", "lucas.benitez@esu.gob.ar", "Medico123!", Set.of(espPedia));




        crearPaciente("Juan", "Pérez", "35123456", "AF-100234", "OSECAC", "juan.perez@salud.gob.ar", "Paciente123!");

        crearPaciente("Ana", "Torres", "28987654", "AF-802341", "PAMI", "ana.torres@salud.gob.ar", "Paciente123!");



        log.info("DataSeeder completado exitosamente: usuarios listos para demostración.");

    }



    private void crearProfesional(String nombre, String apellido, String matricula, String email, String password, Set<Especialidad> especialidades) {

        Usuario u = new Usuario();

        u.setEmail(email);

        u.setPassword(passwordEncoder.encode(password));

        u.setRol("PROFESIONAL");

        u.setEstadoActivo(true);

        u.setFechaDeAlta(LocalDate.now());

        Usuario guardado = usuarioRepositorio.save(u);



        Profesional p = new Profesional();

        p.setNombre(nombre);

        p.setApellido(apellido);

        p.setMatricula(matricula);

        p.setTelefono("3549-420000");

        p.setEstadoActivo(true);

        p.setUsuario(guardado);

        p.setEspecialidades(especialidades);

        profesionalRepositorio.save(p);

    }



    private void crearPaciente(String nombre, String apellido, String dni, String nroAfiliado, String obraSocial, String email, String password) {

        Usuario u = new Usuario();

        u.setEmail(email);

        u.setPassword(passwordEncoder.encode(password));

        u.setRol("PACIENTE");

        u.setEstadoActivo(true);

        u.setFechaDeAlta(LocalDate.now());

        Usuario guardado = usuarioRepositorio.save(u);



        Paciente pac = new Paciente();

        pac.setNombre(nombre);

        pac.setApellido(apellido);

        pac.setDni(dni);

        pac.setNroAfiliado(nroAfiliado);

        pac.setObraSocial(obraSocial);

        pac.setTelefono("3549-421111");

        pac.setUsuario(guardado);

        pacienteRepositorio.save(pac);

    }


    private void asegurarUsuarioDesarrollador() {
        try {
            Usuario dev = usuarioRepositorio.findByEmail("dev@esu.gob.ar").orElse(null);
            if (dev == null) {
                dev = new Usuario();
                dev.setEmail("dev@esu.gob.ar");
                dev.setPassword(passwordEncoder.encode("Dev12345!"));
                dev.setRol("DESARROLLADOR");
                dev.setEstadoActivo(true);
                dev.setFechaDeAlta(LocalDate.now());
                usuarioRepositorio.save(dev);
                log.info("Usuario desarrollador (dev@esu.gob.ar) sembrado en base de datos.");
            } else {
                dev.setEstadoActivo(true);
                dev.setPassword(passwordEncoder.encode("Dev12345!"));
                usuarioRepositorio.save(dev);
            }
        } catch (Exception e) {
            log.warn("No se pudo sembrar el usuario desarrollador: {}", e.getMessage());
        }
    }
    private void asegurarCentroCooperativa() {
        try {
            if (usuarioRepositorio.findByEmail("cooperativa@esu.gob.ar").isEmpty()) {
                Usuario coop = new Usuario();
                coop.setEmail("cooperativa@esu.gob.ar");
                coop.setPassword(passwordEncoder.encode("Coop12345!"));
                coop.setRol("RECEPCIONISTA");
                coop.setEstadoActivo(true);
                coop.setFechaDeAlta(LocalDate.now());
                usuarioRepositorio.save(coop);
                log.info("Cuenta institucional para Cooperativa de Cruz del Eje sembrada en base de datos.");
            }
        } catch (Exception e) {
            log.warn("No se pudo sembrar cuenta Cooperativa: {}", e.getMessage());
        }
    }
}
