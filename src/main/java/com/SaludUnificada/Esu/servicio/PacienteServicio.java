package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.PacienteDtoRequest;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import com.SaludUnificada.Esu.entidad.*;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.PacienteMapper;
import com.SaludUnificada.Esu.repositorio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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

    @Autowired(required = false)
    private com.SaludUnificada.Esu.repositorio.UsuarioRepositorio usuarioRepositorio;

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
    @Transactional
    public void eliminarPaciente(Long id) {
        Paciente paciente = pacienteRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + id));

        boolean tieneHc = historiaClinicaRepositorio != null && historiaClinicaRepositorio.countByPacienteId(id) > 0;
        boolean tieneEvoluciones = evolucionMedicaRepositorio != null && evolucionMedicaRepositorio.countByPacienteId(id) > 0;
        boolean tieneRecetas = recetaRepositorio != null && recetaRepositorio.countByPacienteId(id) > 0;

        if (tieneHc || tieneEvoluciones || tieneRecetas) {
            log.warn("Intento de eliminación física bloqueado para Paciente ID={} con registros clínicos (HC={}, Evoluciones={}, Recetas={}). Aplicando principio de inalterabilidad Ley 26.529.",
                    id, tieneHc, tieneEvoluciones, tieneRecetas);
            if (paciente.getUsuario() != null) {
                paciente.getUsuario().setEstadoActivo(false);
                paciente.getUsuario().revocarSesiones();
                if (usuarioRepositorio != null) {
                    usuarioRepositorio.save(paciente.getUsuario());
                }
            }
            throw new ReglaNegocioException("Por disposición de la Ley 26.529 de Derechos del Paciente, no es posible eliminar físicamente un paciente con historia clínica o registros médicos asociados. El usuario vinculado ha sido inhabilitado para preservar la integridad legal.");
        }

        pacienteRepositorio.delete(paciente);
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
                .or(() -> pacienteRepositorio.findByUsuarioId(id))
                .or(() -> usuarioRepositorio != null ? usuarioRepositorio.findById(id).map(com.SaludUnificada.Esu.entidad.Usuario::getPaciente).filter(java.util.Objects::nonNull) : java.util.Optional.empty())
                .orElseThrow(() -> new NoEncontradoExcepcion("Paciente no encontrado con ID: " + id));
    }

    private Paciente obtenerOCrearPacienteParaUsuario(String email) {
        return pacienteRepositorio.findByUsuarioEmail(email)
                .or(() -> {
                    if (usuarioRepositorio != null) {
                        return usuarioRepositorio.findByEmail(email)
                                .flatMap(u -> {
                                    if (u.getPaciente() != null) return Optional.of(u.getPaciente());
                                    return pacienteRepositorio.findByUsuarioId(u.getId());
                                });
                    }
                    return Optional.empty();
                })
                .orElseGet(() -> {
                    Usuario u = usuarioRepositorio != null ? usuarioRepositorio.findByEmail(email).orElse(null) : null;
                    Paciente p = new Paciente();
                    p.setUsuario(u);
                    String nombreDefault = "Paciente";
                    String apellidoDefault = "ESU";
                    if (u != null && u.getEmail() != null) {
                        String prefix = u.getEmail().split("@")[0];
                        nombreDefault = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
                    }
                    p.setNombre(nombreDefault);
                    p.setApellido(apellidoDefault);
                    String baseDni = (u != null && u.getId() != null) ? String.valueOf(u.getId()) : String.valueOf(System.currentTimeMillis() % 100000000L);
                    String defaultDni = baseDni;
                    int sufijo = 1;
                    while (pacienteRepositorio.existsByDni(defaultDni)) {
                        defaultDni = baseDni + sufijo++;
                    }
                    p.setDni(defaultDni);
                    p.setTelefono("3549000000");
                    p.setObraSocial("Particular");
                    p.setNroAfiliado("AF-" + defaultDni);
                    p.setCentroSaludReferenciaId(1L);
                    p.setCentroSaludReferenciaNombre("Hospital Provincial Dr. Aurelio Crespo");
                    return pacienteRepositorio.save(p);
                });
    }

    @Override
    public PacienteDtoResponse obtenerPerfilAutenticado(String email) {
        Paciente paciente = obtenerOCrearPacienteParaUsuario(email);

        return pacienteMapper.paraDto(paciente);
    }

    @Override
    @Transactional
    public PacienteDtoResponse actualizarDatosContacto(Long id, String telefono, String obraSocial, String nroAfiliado) {
        return actualizarDatosContacto(id, telefono, obraSocial, nroAfiliado, null);
    }

    @Override
    @Transactional
    public PacienteDtoResponse actualizarDatosContacto(Long id, String telefono, String obraSocial, String nroAfiliado, String dni) {
        Paciente paciente = obtenerEntidadPacientePorId(id);
        if (telefono != null && !telefono.isBlank()) {
            String telLimpio = telefono.replaceAll("[^0-9+\\- ]", "").trim();
            if (!telLimpio.isBlank()) {
                paciente.setTelefono(telLimpio);
            }
        }
        if (obraSocial != null && !obraSocial.isBlank()) paciente.setObraSocial(obraSocial.trim());
        if (nroAfiliado != null && !nroAfiliado.isBlank()) paciente.setNroAfiliado(nroAfiliado.trim());
        if (dni != null && !dni.isBlank()) {
            String dniLimpio = dni.replaceAll("[^0-9a-zA-Z-]", "").trim();
            if (!dniLimpio.equalsIgnoreCase(paciente.getDni())) {
                pacienteRepositorio.findByDni(dniLimpio).ifPresent(otro -> {
                    if (!otro.getId().equals(paciente.getId())) {
                        throw new IllegalArgumentException("El DNI ingresado ya pertenece a otro paciente registrado.");
                    }
                });
                paciente.setDni(dniLimpio);
            }
        }
        Paciente guardado = pacienteRepositorio.save(paciente);
        return pacienteMapper.paraDto(guardado);
    }

    @Override
    @Transactional
    public PacienteDtoResponse actualizarMiPerfilPorEmail(String email, java.util.Map<String, String> body) {
        Paciente paciente = obtenerOCrearPacienteParaUsuario(email);

        if (body == null) return pacienteMapper.paraDto(paciente);

        String tel = body.get("telefono");
        String os = body.get("obraSocial");
        String af = body.getOrDefault("nroAfiliado", body.get("numeroAfiliado"));
        String dni = body.get("dni");
        String fn = body.get("fechaNacimiento");
        String nom = body.get("nombre");
        String ape = body.get("apellido");
        String gs = body.get("grupoSanguineo");
        String centro = body.get("centroSaludReferenciaNombre");

        if (nom != null && !nom.isBlank()) paciente.setNombre(nom.trim());
        if (ape != null && !ape.isBlank()) paciente.setApellido(ape.trim());
        if (fn != null && !fn.isBlank()) {
            String fnClean = fn.trim();
            try {
                paciente.setFechaNacimiento(LocalDate.parse(fnClean));
            } catch (Exception e1) {
                try {
                    DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    paciente.setFechaNacimiento(LocalDate.parse(fnClean, dmy));
                } catch (Exception ignored) {
                }
            }
        }

        if (tel != null && !tel.isBlank()) {
            String telLimpio = tel.replaceAll("[^0-9+\\- ]", "").trim();
            if (!telLimpio.isBlank()) {
                paciente.setTelefono(telLimpio);
            }
        }
        if (os != null && !os.isBlank()) paciente.setObraSocial(os.trim());
        if (af != null && !af.isBlank()) paciente.setNroAfiliado(af.trim());
        if (gs != null && !gs.isBlank()) paciente.setGrupoSanguineo(gs.trim());
        if (centro != null && !centro.isBlank()) paciente.setCentroSaludReferenciaNombre(centro.trim());

        if (dni != null && !dni.isBlank()) {
            String dniLimpio = dni.replaceAll("[^0-9a-zA-Z-]", "").trim();
            if (!dniLimpio.equalsIgnoreCase(paciente.getDni())) {
                pacienteRepositorio.findByDni(dniLimpio).ifPresent(otro -> {
                    if (!otro.getId().equals(paciente.getId())) {
                        throw new IllegalArgumentException("El DNI ingresado ya pertenece a otro paciente registrado.");
                    }
                });
                paciente.setDni(dniLimpio);
            }
        }
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

        HistoriaClinica hcDestino = null;
        List<HistoriaClinica> historiasOrigen = new java.util.ArrayList<>();
        if (historiaClinicaRepositorio != null) {
            Optional<HistoriaClinica> hcDestinoOpt = historiaClinicaRepositorio.findFirstByPacienteIdOrderByFechaAtencionDesc(pacienteDestinoId);
            historiasOrigen.addAll(historiaClinicaRepositorio.findByPacienteId(pacienteOrigenId));
            if (hcDestinoOpt.isPresent()) {
                hcDestino = hcDestinoOpt.get();
            } else if (!historiasOrigen.isEmpty()) {
                hcDestino = historiasOrigen.remove(0);
                hcDestino.setPaciente(destino);
                hcDestino = historiaClinicaRepositorio.save(hcDestino);
            }
        }

        int evolucionesCount = 0;
        if (evolucionMedicaRepositorio != null) {
            List<EvolucionMedica> evoluciones = evolucionMedicaRepositorio.findByPacienteIdOrderByFechaCreacionDesc(pacienteOrigenId);
            evolucionesCount = evoluciones.size();
            for (EvolucionMedica e : evoluciones) {
                e.setPaciente(destino);
                if (hcDestino != null) {
                    e.setHistoriaClinica(hcDestino);
                }
            }
            evolucionMedicaRepositorio.saveAll(evoluciones);
        }

        int historiasCount = 0;
        if (historiaClinicaRepositorio != null) {
            for (HistoriaClinica hcExtra : historiasOrigen) {
                if (hcDestino != null) {
                    if ((hcDestino.getDiagnostico() == null || hcDestino.getDiagnostico().isBlank()) && hcExtra.getDiagnostico() != null) {
                        hcDestino.setDiagnostico(hcExtra.getDiagnostico());
                    }
                    if ((hcDestino.getTratamiento() == null || hcDestino.getTratamiento().isBlank()) && hcExtra.getTratamiento() != null) {
                        hcDestino.setTratamiento(hcExtra.getTratamiento());
                    }
                    if ((hcDestino.getMotivoConsulta() == null || hcDestino.getMotivoConsulta().isBlank()) && hcExtra.getMotivoConsulta() != null) {
                        hcDestino.setMotivoConsulta(hcExtra.getMotivoConsulta());
                    }
                }
                historiaClinicaRepositorio.delete(hcExtra);
                historiasCount++;
            }
            if (hcDestino != null) {
                historiaClinicaRepositorio.save(hcDestino);
                historiasCount++;
            }
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
