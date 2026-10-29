package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.PacienteMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import com.SaludUnificada.Esu.repositorio.RecetaDigitalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PacienteServicio implements IPacienteServicio {

    @Autowired
    private PacienteRepositorio pacienteRepositorio;

    @Autowired
    private PacienteMapper pacienteMapper;

    @Autowired(required = false)
    private TurnoRepositorio turnoRepositorio;

    @Autowired(required = false)
    private RecetaDigitalRepositorio recetaRepositorio;

    @Autowired(required = false)
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Override
    public PacienteDtoResponse crearPaciente(PacienteDtoRequest pacienteDto) {
        String dniLimpio = (pacienteDto.getDni() != null) ? pacienteDto.getDni().replaceAll("[^0-9a-zA-Z]", "").trim() : "";
        if (pacienteRepositorio.existsByDni(dniLimpio)) {
            throw new ReglaNegocioException("Ya existe un paciente registrado con el DNI ingresado: " + dniLimpio);
        }
        pacienteDto.setDni(dniLimpio);
        if (pacienteDto.getTelefono() != null) {
            pacienteDto.setTelefono(pacienteDto.getTelefono().trim());
        }
        Paciente paciente = pacienteMapper.paraEntidad(pacienteDto);
        Paciente pacienteGuardado = pacienteRepositorio.save(paciente);
        return pacienteMapper.paraDto(pacienteGuardado);
    }

    @Override
    public PacienteDtoResponse obtenerPacientePorId(Long id) {
        Paciente paciente = obtenerEntidadPacientePorId(id);
        return pacienteMapper.paraDto(paciente);
    }

    @Override
    public List<PacienteDtoResponse> listarTodos() {
        return pacienteRepositorio.findAll().stream()
                .map(pacienteMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PacienteDtoResponse> buscarPacientesPaginados(Pageable pageable) {
        return pacienteRepositorio.findAll(pageable)
                .map(pacienteMapper::paraDto);
    }

    @Override
    public long contarPacientes() {
        return pacienteRepositorio.count();
    }

    @Override
    public void eliminarPaciente(Long id) {
        if (pacienteRepositorio.existsById(id)) {
            pacienteRepositorio.deleteById(id);
        } else {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + id);
        }
    }

    @Override
    public PacienteDtoResponse buscarPorDni(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede ser nulo o vacio");
        }
        String dniNormalizado = dni.replaceAll("[^0-9a-zA-Z]", "").trim();
        Paciente paciente = pacienteRepositorio.findByDni(dniNormalizado)
                .or(() -> pacienteRepositorio.findByDni(dni.trim()))
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con DNI: " + dni));
        return pacienteMapper.paraDto(paciente);
    }

    @Override
    public List<PacienteDtoResponse> buscarPorApellidoParcial(String apellido) {
        return pacienteRepositorio.findByApellidoContainingIgnoreCase(apellido).stream()
                .map(pacienteMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public Paciente obtenerEntidadPacientePorId(Long id) {
        return pacienteRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + id));
    }

    @Override
    public PacienteDtoResponse obtenerPerfilAutenticado(String email) {
        Paciente paciente = pacienteRepositorio.findByUsuarioEmail(email)
                .orElseThrow(() -> new NoEncontradoExcepcion("El usuario autenticado no tiene un perfil de paciente vinculado."));
        return pacienteMapper.paraDto(paciente);
    }

    @Override
    @Transactional
    public PacienteDtoResponse actualizarDatosContacto(Long id, String telefono, String obraSocial, String nroAfiliado) {
        Paciente paciente = obtenerEntidadPacientePorId(id);
        if (telefono != null) paciente.setTelefono(telefono.replaceAll("[^0-9+ ]", "").trim());
        if (obraSocial != null && !obraSocial.isBlank()) paciente.setObraSocial(obraSocial.trim());
        if (nroAfiliado != null && !nroAfiliado.isBlank()) paciente.setNroAfiliado(nroAfiliado.trim());
        Paciente guardado = pacienteRepositorio.save(paciente);
        return pacienteMapper.paraDto(guardado);
    }

    @Override
    @Transactional
    public void fusionarPacientes(Long pacienteOrigenId, Long pacienteDestinoId, String motivo, String emailAdmin) {
        if (pacienteOrigenId == null || pacienteDestinoId == null || pacienteOrigenId.equals(pacienteDestinoId)) {
            throw new IllegalArgumentException("Los IDs de paciente origen y destino deben ser distintos y no nulos.");
        }
        Paciente origen = pacienteRepositorio.findById(pacienteOrigenId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente origen no encontrado con ID: " + pacienteOrigenId));
        Paciente destino = pacienteRepositorio.findById(pacienteDestinoId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente destino no encontrado con ID: " + pacienteDestinoId));

        if (turnoRepositorio != null) {
            List<Turno> turnos = turnoRepositorio.findByPacienteIdOrderByFechaHoraDesc(pacienteOrigenId);
            for (Turno t : turnos) {
                t.setPaciente(destino);
            }
            turnoRepositorio.saveAll(turnos);
        }

        if (recetaRepositorio != null) {
            List<RecetaDigital> recetas = recetaRepositorio.findByPacienteIdOrderByFechaEmisionDesc(pacienteOrigenId);
            for (RecetaDigital r : recetas) {
                r.setPaciente(destino);
            }
            recetaRepositorio.saveAll(recetas);
        }

        if (historiaClinicaRepositorio != null) {
            List<HistoriaClinica> historias = historiaClinicaRepositorio.findByPacienteId(pacienteOrigenId);
            for (HistoriaClinica hc : historias) {
                hc.setPaciente(destino);
            }
            historiaClinicaRepositorio.saveAll(historias);
        }

        if (origen.getUsuario() != null) {
            origen.getUsuario().setEstadoActivo(false);
        }
        origen.setNombre("[FUSIONADO #" + destino.getId() + "] " + origen.getNombre());
        origen.setDni("FUSIONADO-" + origen.getId() + "-" + origen.getDni());
        origen.setNroAfiliado("FUSIONADO-" + origen.getId() + "-" + origen.getNroAfiliado());
        pacienteRepositorio.save(origen);
    }
}