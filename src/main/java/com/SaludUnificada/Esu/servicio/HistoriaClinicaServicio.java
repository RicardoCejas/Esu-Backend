package com.SaludUnificada.Esu.servicio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.SaludUnificada.Esu.dto.request.HistoriaClinicaDtoRequest;
import com.SaludUnificada.Esu.dto.response.HistoriaClinicaDtoResponse;
import com.SaludUnificada.Esu.entidad.HistoriaClinica;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.mapper.HistoriaClinicaMapper;
import com.SaludUnificada.Esu.repositorio.HistoriaClinicaRepositorio;
import com.SaludUnificada.Esu.entidad.LogAccesoHc;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.LogAccesoHcRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.UsuarioRepositorio;
import com.SaludUnificada.Esu.seguridad.OwnershipSecurityEvaluator;
import com.SaludUnificada.Esu.seguridad.RequestIpUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoriaClinicaServicio implements IHistoriaClinicaServicio {

    @Autowired
    private HistoriaClinicaRepositorio historiaClinicaRepositorio;

    @Autowired
    private HistoriaClinicaMapper historiaClinicaMapper;

    @Autowired(required = false)
    private LogAccesoHcRepositorio logAccesoHcRepositorio;

    @Autowired(required = false)
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired(required = false)
    private PacienteRepositorio pacienteRepositorio;

    @Autowired(required = false)
    private RequestIpUtil requestIpUtil;

    @Autowired(required = false)
    private OwnershipSecurityEvaluator ownershipSecurityEvaluator;

    @Override
    @Transactional
    public HistoriaClinicaDtoResponse crearHistoriaClinica(HistoriaClinicaDtoRequest dto) {
        HistoriaClinica historiaClinica = historiaClinicaMapper.paraEntidad(dto);
        HistoriaClinica guardada = historiaClinicaRepositorio.save(historiaClinica);
        return historiaClinicaMapper.paraDto(guardada);
    }

    @Override
    public HistoriaClinicaDtoResponse obtenerHistoriaClinicaPorId(Long id) {
        HistoriaClinica historiaClinica = historiaClinicaRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Historia clínica no encontrada con ID: " + id));
        return historiaClinicaMapper.paraDto(historiaClinica);
    }

    @Override
    public List<HistoriaClinicaDtoResponse> listarTodas() {
        return historiaClinicaRepositorio.findAll().stream()
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminarHistoriaClinica(Long id) {
        if (!historiaClinicaRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Historia clínica no encontrada con ID: " + id);
        }
        historiaClinicaRepositorio.deleteById(id);
    }

    @Override
    public List<HistoriaClinicaDtoResponse> buscarPorPacienteId(Long pacienteId) {
        List<HistoriaClinica> historiasClinicas = historiaClinicaRepositorio.findByPacienteId(pacienteId);

        if (historiasClinicas.isEmpty()) {
            throw new NoEncontradoExcepcion("No se encontraron historias clínicas para el paciente con ID: " + pacienteId);
        }

        return historiasClinicas.stream()
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<HistoriaClinicaDtoResponse> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return historiaClinicaRepositorio.findByFechaAtencionBetween(inicio, fin).stream()
                .filter(hc -> hc.getPaciente() != null && 
                        (ownershipSecurityEvaluator == null || ownershipSecurityEvaluator.canAccessPatient(hc.getPaciente().getId())))
                .map(historiaClinicaMapper::paraDto)
                .collect(Collectors.toList());
    }

    @Override
    public long contarPorPaciente(Long pacienteId) {
        return historiaClinicaRepositorio.countByPacienteId(pacienteId);
    }

    @Override
    public Page<HistoriaClinicaDtoResponse> listarPorPacientePaginado(Long pacienteId, Pageable pageable) {
        return historiaClinicaRepositorio.findByPacienteId(pacienteId, pageable)
                .map(historiaClinicaMapper::paraDto);
    }

    @Override
    @Transactional
    public void registrarAccesoEmergencia(Long pacienteId, String emailProfesional, String motivo) {
        if (motivo == null || motivo.trim().length() < 10) {
            throw new IllegalArgumentException("Debe especificarse un motivo clínico válido y detallado para el acceso de emergencia (mínimo 10 caracteres).");
        }
        if (pacienteRepositorio != null && !pacienteRepositorio.existsById(pacienteId)) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId);
        }
        if (usuarioRepositorio != null && logAccesoHcRepositorio != null) {
            Usuario usuario = usuarioRepositorio.findByEmail(emailProfesional)
                    .orElseThrow(() -> new NoEncontradoExcepcion("Profesional no encontrado"));
            String ip = requestIpUtil != null ? requestIpUtil.obtenerIpCliente(null) : "127.0.0.1";
            LogAccesoHc log = new LogAccesoHc(usuario.getId(), pacienteId, "ACCESO_EMERGENCIA", ip, usuario.getRol(), motivo.trim());
            logAccesoHcRepositorio.save(log);
        }
    }
}
