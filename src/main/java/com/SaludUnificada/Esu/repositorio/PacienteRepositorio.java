package com.SaludUnificada.Esu.repositorio;



import com.SaludUnificada.Esu.entidad.Paciente;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;



import java.util.List;

import java.util.Optional;



@Repository

public interface PacienteRepositorio extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDni(String dni);
    boolean existsByDni(String dni);



    Optional<Paciente> findByUsuarioEmail(String email);



    boolean existsByUsuarioEmail(String email);




    List<Paciente> findByApellidoContainingIgnoreCase(String apellido);

}

