package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.PacienteMapper;
import com.SaludUnificada.Esu.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Autowired(required = false)
    private EvolucionMedicaRepositorio evolucionMedicaRepositorio;

    @Autowired(required = false)
    private EstudioAdjuntoRepositorio estudioAdjuntoRepositorio;

    @Autowired(required = false)
    private AlergiaPacienteRepositorio alergiaPacienteRepositorio;

    @Autowired(required = false)
    private ConsentimientoInformadoRepositorio consentimientoRepositorio;

    @Autowired(required = false)
    private VinculoFamiliarRepositorio vinculoFamiliarRepositorio;

    @Autowired(required = false)
    private AuditoriaFusionPacienteRepositorio auditoriaFusionPacienteRepositorio;

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

        String dniOrigenOriginal = origen.getDni();

        int turnosCount = 0;
        if (turnoRepositorio != null) {
            List<Turno> turnos = turnoRepositorio.findByPacienteIdOrderByFechaHoraDesc(pacienteOrigenId);
            turnosCount = turnos.size();
            for (Turno t : turnos) {
                t.setPaciente(destino);
            }
            turnoRepositorio.saveAll(turnos);
        }

        int recetasCount = 0;
        if (recetaRepositorio != null) {
            List<RecetaDigital> recetas = recetaRepositorio.findByPacienteIdOrderByFechaEmisionDesc(pacienteOrigenId);
            recetasCount = recetas.size();
            for (RecetaDigital r : recetas) {
                r.setPaciente(destino);
            }
            recetaRepositorio.saveAll(recetas);
        }

        int historiasCount = 0;
        if (historiaClinicaRepositorio != null) {
            List<HistoriaClinica> historias = historiaClinicaRepositorio.findByPacienteId(pacienteOrigenId);
            historiasCount = historias.size();
            for (HistoriaClinica hc : historias) {
                hc.setPaciente(destino);
            }
            historiaClinicaRepositorio.saveAll(historias);
        }

        int evolucionesCount = 0;
        if (evolucionMedicaRepositorio != null) {
            List<EvolucionMedica> evoluciones = evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteOrigenId);
            evolucionesCount = evoluciones.size();
            for (EvolucionMedica e : evoluciones) {
                e.setPaciente(destino);
            }
            evolucionMedicaRepositorio.saveAll(evoluciones);
        }

        int estudiosCount = 0;
        if (estudioAdjuntoRepositorio != null) {
            List<EstudioAdjunto> estudios = estudioAdjuntoRepositorio.findByPacienteIdOrderByFechaCargaDesc(pacienteOrigenId);
            estudiosCount = estudios.size();
            for (EstudioAdjunto est : estudios) {
                est.setPacienteId(destino.getId());
            }
            estudioAdjuntoRepositorio.saveAll(estudios);
        }

        int alergiasCount = 0;
        if (alergiaPacienteRepositorio != null) {
            List<AlergiaPaciente> alergias = alergiaPacienteRepositorio.findByPacienteIdOrderByFechaRegistroDesc(pacienteOrigenId);
            alergiasCount = alergias.size();
            for (AlergiaPaciente a : alergias) {
                a.setPacienteId(destino.getId());
            }
            alergiaPacienteRepositorio.saveAll(alergias);
        }

        int consentimientosCount = 0;
        if (consentimientoRepositorio != null) {
            List<ConsentimientoInformado> consentimientos = consentimientoRepositorio.findByPacienteIdOrderByFechaHoraFirmaDesc(pacienteOrigenId);
            consentimientosCount = consentimientos.size();
            for (ConsentimientoInformado c : consentimientos) {
                c.setPacienteId(destino.getId());
            }
            consentimientoRepositorio.saveAll(consentimientos);
        }

        int vinculosCount = 0;
        if (vinculoFamiliarRepositorio != null) {
            // 1. Donde el paciente origen era titular
            List<VinculoFamiliar> vinculosTitular = vinculoFamiliarRepositorio.findByTitularId(pacienteOrigenId);
            for (VinculoFamiliar v : vinculosTitular) {
                if (vinculoFamiliarRepositorio.existsByTitularIdAndFamiliarDni(destino.getId(), v.getFamiliarDni())) {
                    vinculoFamiliarRepositorio.delete(v);
                } else {
                    v.setTitular(destino);
                    vinculoFamiliarRepositorio.save(v);
                    vinculosCount++;
                }
            }

            // 2. Donde el paciente origen figuraba como familiar por DNI
            if (dniOrigenOriginal != null && !dniOrigenOriginal.isBlank()) {
                List<VinculoFamiliar> vinculosComoFamiliar = vinculoFamiliarRepositorio.findByFamiliarDni(dniOrigenOriginal);
                for (VinculoFamiliar v : vinculosComoFamiliar) {
                    if (v.getTitular() != null && v.getTitular().getId().equals(destino.getId())) {
                        vinculoFamiliarRepositorio.delete(v);
                    } else if (v.getTitular() != null && vinculoFamiliarRepositorio.existsByTitularIdAndFamiliarDni(v.getTitular().getId(), destino.getDni())) {
                        vinculoFamiliarRepositorio.delete(v);
                    } else {
                        v.setFamiliarDni(destino.getDni());
                        vinculoFamiliarRepositorio.save(v);
                        vinculosCount++;
                    }
                }
            }
        }

        if (origen.getUsuario() != null) {
            origen.getUsuario().setEstadoActivo(false);
            origen.getUsuario().revocarSesiones();
        }
        origen.setNombre("[FUSIONADO #" + destino.getId() + "] " + origen.getNombre());
        origen.setDni("FUSIONADO-" + origen.getId() + "-" + origen.getDni());
        origen.setNroAfiliado("FUSIONADO-" + origen.getId() + "-" + origen.getNroAfiliado());
        pacienteRepositorio.save(origen);

        if (auditoriaFusionPacienteRepositorio != null) {
            String detalles = String.format("Transferidos: turnos=%d, recetas=%d, historias=%d, evoluciones=%d, estudios=%d, alergias=%d, consentimientos=%d, vinculos=%d",
                    turnosCount, recetasCount, historiasCount, evolucionesCount, estudiosCount, alergiasCount, consentimientosCount, vinculosCount);
            AuditoriaFusionPaciente audit = AuditoriaFusionPaciente.builder()
                    .pacienteOrigenId(pacienteOrigenId)
                    .pacienteDestinoId(pacienteDestinoId)
                    .operadorEmail(emailAdmin != null ? emailAdmin : "SISTEMA")
                    .motivo(motivo != null ? motivo.trim() : "Fusión administrativa de registros clínicos duplicados")
                    .fechaHora(LocalDateTime.now())
                    .detalles(detalles)
                    .build();
            auditoriaFusionPacienteRepositorio.save(audit);
        }
    }
}
