package com.SaludUnificada.Esu.mapper;

import com.SaludUnificada.Esu.dto.request.TurnoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.CentroSalud;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.CentroSaludRepositorio;
import com.SaludUnificada.Esu.servicio.IPacienteServicio;
import com.SaludUnificada.Esu.servicio.IProfesionalServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TurnoMapper {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private IPacienteServicio pacienteServicio;

    @Autowired
    private IProfesionalServicio profesionalServicio;

    @Autowired
    private PacienteMapper pacienteMapper;

    @Autowired
    private ProfesionalMapper profesionalMapper;

    @Autowired
    private CentroSaludRepositorio centroSaludRepositorio;

    public Turno paraEntidad(TurnoDtoRequest dto) {
        Turno turno = new Turno();
        turno.setFechaHora(dto.getFechaHora());
        turno.setMotivoTurno(resolverMotivo(dto));
        turno.setEstado(dto.getEstado() == null || dto.getEstado().isBlank()
                ? "PENDIENTE"
                : dto.getEstado().trim().toUpperCase());
        turno.setCodigoVerificacion(generarCodigoVerificacion());
        turno.setObraSocial(dto.getObraSocial());
        turno.setNumeroAfiliado(dto.getNumeroAfiliado());
        turno.setCopagoMonto(dto.getCopagoMonto());
        turno.setNumeroBono(dto.getNumeroBono());
        turno.setEsPrimeraVez(Boolean.TRUE.equals(dto.getEsPrimeraVez()));
        turno.setConsultorioNumero(dto.getConsultorioNumero());
        turno.setPlanta(dto.getPlanta());

        if (dto.getPacienteId() != null) {
            Paciente paciente = pacienteServicio.obtenerEntidadPacientePorId(dto.getPacienteId());
            turno.setPaciente(paciente);
        }

        if (dto.getProfesionalId() != null) {
            Profesional profesional = profesionalServicio.obtenerEntidadProfesionalPorId(dto.getProfesionalId());
            turno.setProfesional(profesional);
        }

        if (dto.getCentroSaludId() != null) {
            CentroSalud centro = centroSaludRepositorio.findById(dto.getCentroSaludId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Centro de salud no encontrado con ID: " + dto.getCentroSaludId()));
            turno.setCentroSalud(centro);
        }

        return turno;
    }

    public TurnoDtoResponse paraDto(Turno turno) {
        TurnoDtoResponse dto = new TurnoDtoResponse();
        dto.setId(turno.getId());
        dto.setFechaHora(turno.getFechaHora());
        if (turno.getFechaHora() != null) {
            dto.setFecha(turno.getFechaHora().toLocalDate().toString());
            dto.setHora(turno.getFechaHora().format(FORMATO_HORA));
        }
        dto.setEstado(turno.getEstado());
        dto.setMotivoTurno(turno.getMotivoTurno());
        dto.setMotivoConsulta(turno.getMotivoTurno());
        dto.setCodigoVerificacion(turno.getCodigoVerificacion());
        dto.setObraSocial(turno.getObraSocial());
        dto.setNumeroAfiliado(turno.getNumeroAfiliado());
        dto.setCopagoMonto(turno.getCopagoMonto());
        dto.setNumeroBono(turno.getNumeroBono());
        dto.setEsPrimeraVez(turno.getEsPrimeraVez());
        dto.setConsultorioNumero(turno.getConsultorioNumero());
        dto.setPlanta(turno.getPlanta());
        dto.setJustificativoCancelacion(turno.getJustificativoCancelacion());
        dto.setHoraCheckIn(turno.getHoraCheckIn());
        dto.setHoraLlamado(turno.getHoraLlamado());
        dto.setFechaCreacion(turno.getFechaCreacion());

        if (turno.getPaciente() != null) {
            Paciente paciente = turno.getPaciente();
            dto.setPacienteId(paciente.getId());
            dto.setPacienteNombre(paciente.getNombre() + " " + paciente.getApellido());
            dto.setPacienteDni(paciente.getDni());
            dto.setPacienteTelefono(paciente.getTelefono());
            dto.setPaciente(pacienteMapper.paraDto(paciente));
        }

        if (turno.getProfesional() != null) {
            Profesional profesional = turno.getProfesional();
            dto.setProfesionalId(profesional.getId());
            dto.setProfesionalNombre(profesional.getNombre() + " " + profesional.getApellido());
            if (!profesional.getEspecialidades().isEmpty()) {
                Especialidad especialidad = profesional.getEspecialidades().iterator().next();
                dto.setEspecialidadNombre(especialidad.getNombre());
            }
            dto.setProfesional(profesionalMapper.paraDto(profesional));
        }

        if (turno.getCentroSalud() != null) {
            dto.setCentroSaludId(turno.getCentroSalud().getId());
            dto.setCentroSaludNombre(turno.getCentroSalud().getNombre());
        }

        return dto;
    }

    private String resolverMotivo(TurnoDtoRequest dto) {
        if (dto.getMotivoTurno() != null && !dto.getMotivoTurno().isBlank()) {
            return dto.getMotivoTurno().trim();
        }
        if (dto.getMotivoConsulta() != null && !dto.getMotivoConsulta().isBlank()) {
            return dto.getMotivoConsulta().trim();
        }
        return null;
    }

    private String generarCodigoVerificacion() {
        int aleatorio = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ESU-" + LocalDate.now().getYear() + "-" + aleatorio;
    }
}
