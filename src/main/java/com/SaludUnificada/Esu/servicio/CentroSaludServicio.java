package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.CentroSaludDtoRequest;
import com.SaludUnificada.Esu.dto.request.ConsultorioFisicoDtoRequest;
import com.SaludUnificada.Esu.dto.request.ConsultorioFisicoUpdateDtoRequest;
import com.SaludUnificada.Esu.dto.response.CentroSaludDtoResponse;
import com.SaludUnificada.Esu.dto.response.ConsultorioFisicoDtoResponse;
import com.SaludUnificada.Esu.entidad.CentroSalud;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.ConsultorioFisico;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.CentroSaludRepositorio;
import com.SaludUnificada.Esu.repositorio.ConsultorioFisicoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentroSaludServicio {

    private final CentroSaludRepositorio centroSaludRepositorio;
    private final com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio profesionalRepositorio;
    private final ConsultorioFisicoRepositorio consultorioRepositorio;
    private final CatalogoEspecialidadesNormalizador normalizador;

    @Transactional(readOnly = true)
    public List<CentroSaludDtoResponse> listarTodos() {
        return centroSaludRepositorio.findByActivoTrueOrderByIdAsc().stream()
                .map(this::paraDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CentroSaludDtoResponse obtenerPorId(Long id) {
        CentroSalud centro = centroSaludRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + id));
        return paraDto(centro);
    }

    @Transactional
    public CentroSaludDtoResponse crearCentro(CentroSaludDtoRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del centro de salud es obligatorio");
        }
        String nombreLimpio = request.getNombre().trim();
        if (centroSaludRepositorio.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new ReglaNegocioException("Ya existe un centro de salud registrado con el nombre: " + nombreLimpio);
        }

        boolean permiteEstudios = request.getPermiteEstudios() == null || request.getPermiteEstudios();
        int cantidadPlantas = validarCantidadPlantas(request.getCantidadPlantas(), 1);
        String estudiosHabilitados = resolverSerializacionEstudiosAlta(permiteEstudios, request.getEstudiosDisponibles());

        CentroSalud centro = CentroSalud.builder()
                .nombre(nombreLimpio)
                .tipo(request.getTipo() != null && !request.getTipo().isBlank() ? request.getTipo().trim().toUpperCase() : "CLINICA")
                .direccion(request.getDireccion() != null ? request.getDireccion().trim() : "")
                .telefono(request.getTelefono() != null ? request.getTelefono().trim() : null)
                .ciudad(request.getCiudad() != null && !request.getCiudad().isBlank() ? request.getCiudad().trim() : "Cruz del Eje")
                .horarioAtencion(request.getHorarioAtencion() != null ? request.getHorarioAtencion().trim() : null)
                .disponibleGuardia(Boolean.TRUE.equals(request.getDisponibleGuardia()))
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .permiteConsultas(request.getPermiteConsultas() == null || request.getPermiteConsultas())
                .permiteEstudios(permiteEstudios)
                .cantidadPlantas(cantidadPlantas)
                .estudiosHabilitados(estudiosHabilitados)
                .activo(true)
                .build();

        return paraDto(centroSaludRepositorio.save(centro));
    }

    @Transactional
    public CentroSaludDtoResponse actualizarCentro(Long id, CentroSaludDtoRequest request) {
        CentroSalud centro = centroSaludRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + id));

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            String nuevoNombre = request.getNombre().trim();
            if (!centro.getNombre().equalsIgnoreCase(nuevoNombre) && centroSaludRepositorio.existsByNombreIgnoreCase(nuevoNombre)) {
                throw new ReglaNegocioException("Ya existe otro centro de salud con el nombre: " + nuevoNombre);
            }
            centro.setNombre(nuevoNombre);
        }
        if (request.getTipo() != null && !request.getTipo().isBlank()) {
            centro.setTipo(request.getTipo().trim().toUpperCase());
        }
        if (request.getDireccion() != null && !request.getDireccion().isBlank()) {
            centro.setDireccion(request.getDireccion().trim());
        }
        if (request.getTelefono() != null) {
            centro.setTelefono(request.getTelefono().trim());
        }
        if (request.getCiudad() != null && !request.getCiudad().isBlank()) {
            centro.setCiudad(request.getCiudad().trim());
        }
        if (request.getHorarioAtencion() != null) {
            centro.setHorarioAtencion(request.getHorarioAtencion().trim());
        }
        if (request.getDisponibleGuardia() != null) {
            centro.setDisponibleGuardia(request.getDisponibleGuardia());
        }
        if (request.getLatitud() != null) {
            centro.setLatitud(request.getLatitud());
        }
        if (request.getLongitud() != null) {
            centro.setLongitud(request.getLongitud());
        }
        if (request.getPermiteConsultas() != null) {
            centro.setPermiteConsultas(request.getPermiteConsultas());
        }
        if (request.getPermiteEstudios() != null) {
            centro.setPermiteEstudios(request.getPermiteEstudios());
        }
        if (request.getCantidadPlantas() != null) {
            int nuevasPlantas = validarCantidadPlantas(request.getCantidadPlantas(), 1);
            validarPlantasCompatiblesConConsultorios(id, nuevasPlantas);
            centro.setCantidadPlantas(nuevasPlantas);
        }
        if (request.getEstudiosDisponibles() != null) {
            centro.setEstudiosHabilitados(normalizador.serializarEstudiosHabilitados(request.getEstudiosDisponibles()));
        }

        return paraDto(centroSaludRepositorio.save(centro));
    }

    @Transactional
    public void eliminarCentro(Long id) {
        if (!centroSaludRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + id);
        }
        centroSaludRepositorio.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse> listarEspecialidadesPorCentro(Long centroId) {
        if (centroId == null) return List.of();

        List<Profesional> profesionales = profesionalRepositorio.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getEstadoActivo()))
                .filter(p -> (p.getCentrosSaludIds() != null && p.getCentrosSaludIds().contains(centroId)) ||
                             (p.getUsuario() != null && centroId.equals(p.getUsuario().getCentroSaludId())))
                .toList();

        java.util.Map<Long, com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse> mapa = new java.util.LinkedHashMap<>();
        for (Profesional p : profesionales) {
            if (p.getEspecialidades() != null) {
                for (Especialidad esp : p.getEspecialidades()) {
                    if (esp != null && !mapa.containsKey(esp.getId())) {
                        mapa.put(esp.getId(), new com.SaludUnificada.Esu.dto.response.EspecialidadDtoResponse(
                                esp.getId(),
                                esp.getNombre(),
                                esp.getDescripcion() != null ? esp.getDescripcion() : "Atención médica especializada en " + esp.getNombre() + ".",
                                esp.getCategoria() != null ? esp.getCategoria() : "CONSULTA",
                                esp.getNombreCanonico() != null ? esp.getNombreCanonico() : esp.getNombre()
                        ));
                    }
                }
            }
        }
        return new java.util.ArrayList<>(mapa.values());
    }

    @Transactional(readOnly = true)
    public List<ConsultorioFisicoDtoResponse> listarConsultorios(Long centroSaludId) {
        List<ConsultorioFisico> lista = (centroSaludId != null)
                ? consultorioRepositorio.findByCentroSaludIdOrderByNumeroAsc(centroSaludId)
                : consultorioRepositorio.findByActivoTrueOrderByCentroSaludIdAscNumeroAsc();
        return lista.stream().map(this::paraDtoConsultorio).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsultorioFisicoDtoResponse obtenerConsultorioPorId(Long id) {
        ConsultorioFisico consultorio = consultorioRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Consultorio físico no encontrado con ID: " + id));
        return paraDtoConsultorio(consultorio);
    }

    @Transactional
    public ConsultorioFisicoDtoResponse crearConsultorio(ConsultorioFisicoDtoRequest request) {
        if (request.getCentroSaludId() == null) {
            throw new IllegalArgumentException("El ID del centro de salud es obligatorio");
        }
        CentroSalud centro = centroSaludRepositorio.findById(request.getCentroSaludId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + request.getCentroSaludId()));
        if (request.getNumero() == null || request.getNumero().isBlank()) {
            throw new IllegalArgumentException("El número de consultorio es obligatorio");
        }

        String numeroLimpio = request.getNumero().trim();
        if (consultorioRepositorio.existsByCentroSaludIdAndNumeroIgnoreCase(request.getCentroSaludId(), numeroLimpio)) {
            throw new ReglaNegocioException("Ya existe un consultorio físico con el número/código " + numeroLimpio + " en esta sede.");
        }

        String plantaValidada = validarYNormalizarPlanta(centro, request.getPlanta());
        String especialidadLimpia = request.getEspecialidadAsignada() != null && !request.getEspecialidadAsignada().isBlank()
                ? request.getEspecialidadAsignada().trim()
                : null;

        ConsultorioFisico consultorio = ConsultorioFisico.builder()
                .centroSaludId(request.getCentroSaludId())
                .numero(numeroLimpio)
                .planta(plantaValidada)
                .especialidadAsignada(especialidadLimpia)
                .activo(request.getActivo() == null || request.getActivo())
                .build();

        return paraDtoConsultorio(consultorioRepositorio.save(consultorio));
    }

    @Transactional
    public ConsultorioFisicoDtoResponse actualizarConsultorio(Long id, ConsultorioFisicoUpdateDtoRequest request) {
        ConsultorioFisico consultorio = consultorioRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Consultorio físico no encontrado con ID: " + id));
        CentroSalud centro = centroSaludRepositorio.findById(consultorio.getCentroSaludId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + consultorio.getCentroSaludId()));

        if (request.getNumero() == null || request.getNumero().isBlank()) {
            throw new IllegalArgumentException("El número o código del consultorio es obligatorio");
        }
        String nuevoNumero = request.getNumero().trim();
        if (!consultorio.getNumero().equalsIgnoreCase(nuevoNumero)
                && consultorioRepositorio.existsByCentroSaludIdAndNumeroIgnoreCase(consultorio.getCentroSaludId(), nuevoNumero)) {
            throw new ReglaNegocioException("Ya existe otro consultorio físico con el número/código " + nuevoNumero + " en esta sede.");
        }

        consultorio.setNumero(nuevoNumero);
        consultorio.setPlanta(validarYNormalizarPlanta(centro, request.getPlanta() != null ? request.getPlanta() : consultorio.getPlanta()));
        consultorio.setEspecialidadAsignada(
                request.getEspecialidadAsignada() != null && !request.getEspecialidadAsignada().isBlank()
                        ? request.getEspecialidadAsignada().trim()
                        : null
        );
        if (request.getActivo() != null) {
            consultorio.setActivo(request.getActivo());
        }

        return paraDtoConsultorio(consultorioRepositorio.save(consultorio));
    }

    @Transactional
    public ConsultorioFisicoDtoResponse cambiarEstadoConsultorio(Long id, boolean activo) {
        ConsultorioFisico consultorio = consultorioRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Consultorio físico no encontrado con ID: " + id));
        consultorio.setActivo(activo);
        return paraDtoConsultorio(consultorioRepositorio.save(consultorio));
    }

    @Transactional
    public void eliminarConsultorio(Long id) {
        if (!consultorioRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Consultorio físico no encontrado con ID: " + id);
        }
        consultorioRepositorio.deleteById(id);
    }

    private static final Pattern PATRON_PISO_ORDINAL = Pattern.compile("^(\\d+)\\s*[°º]?\\s*piso$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_PISO_PREFIJO = Pattern.compile("^piso\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);

    private int validarCantidadPlantas(Integer raw, int fallback) {
        int valor = raw != null ? raw : fallback;
        if (valor < 1 || valor > 30) {
            throw new ReglaNegocioException("La cantidad de plantas físicas debe estar entre 1 y 30.");
        }
        return valor;
    }

    private String resolverSerializacionEstudiosAlta(boolean permiteEstudios, List<String> estudiosSolicitados) {
        if (!permiteEstudios) {
            return "";
        }
        if (estudiosSolicitados != null) {
            return normalizador != null ? normalizador.serializarEstudiosHabilitados(estudiosSolicitados) : "";
        }
        return normalizador != null
                ? normalizador.serializarEstudiosHabilitados(CatalogoEspecialidadesNormalizador.MODALIDADES_ESTUDIOS)
                : "";
    }

    private void validarPlantasCompatiblesConConsultorios(Long centroId, int nuevasPlantas) {
        List<ConsultorioFisico> existentes = consultorioRepositorio.findByCentroSaludIdOrderByNumeroAsc(centroId);
        for (ConsultorioFisico c : existentes) {
            Integer nivel = extraerIndicePlanta(c.getPlanta());
            if (nivel != null && nivel >= nuevasPlantas) {
                throw new ReglaNegocioException(
                        "No se puede reducir la sede a " + nuevasPlantas + " planta(s) mientras el Consultorio "
                                + c.getNumero() + " siga asignado en " + c.getPlanta() + "."
                );
            }
        }
    }

    private String validarYNormalizarPlanta(CentroSalud centro, String plantaRaw) {
        int plantas = (centro.getCantidadPlantas() != null && centro.getCantidadPlantas() >= 1)
                ? centro.getCantidadPlantas()
                : 1;
        String limpia = (plantaRaw != null && !plantaRaw.isBlank()) ? plantaRaw.trim() : "Planta Baja";
        if ("Planta Baja".equalsIgnoreCase(limpia)) {
            return "Planta Baja";
        }
        if ("Planta Única".equalsIgnoreCase(limpia) || "Planta Unica".equalsIgnoreCase(limpia)) {
            return "Planta Única";
        }
        Integer nivel = extraerIndicePlanta(limpia);
        if (nivel == null) {
            throw new ReglaNegocioException("Formato de planta inválido. Seleccioná Planta Baja o un piso habilitado (ej: 1° Piso).");
        }
        if (nivel == 0) {
            return "Planta Baja";
        }
        if (nivel < 0 || nivel >= plantas) {
            String limite = plantas <= 1 ? "Planta Baja" : (plantas - 1) + "° Piso";
            throw new ReglaNegocioException(
                    "La planta " + limpia + " excede las " + plantas + " plantas habilitadas para esta sede (máximo: " + limite + ")."
            );
        }
        return nivel + "° Piso";
    }

    private Integer extraerIndicePlanta(String planta) {
        if (planta == null || planta.isBlank()) {
            return 0;
        }
        String limpia = planta.trim();
        if ("Planta Baja".equalsIgnoreCase(limpia)
                || "Planta Única".equalsIgnoreCase(limpia)
                || "Planta Unica".equalsIgnoreCase(limpia)) {
            return 0;
        }
        Matcher mOrd = PATRON_PISO_ORDINAL.matcher(limpia);
        if (mOrd.matches()) {
            return Integer.parseInt(mOrd.group(1));
        }
        Matcher mPref = PATRON_PISO_PREFIJO.matcher(limpia);
        if (mPref.matches()) {
            return Integer.parseInt(mPref.group(1));
        }
        return null;
    }

    private CentroSaludDtoResponse paraDto(CentroSalud entidad) {
        boolean permiteConsultas = entidad.getPermiteConsultas() != null ? entidad.getPermiteConsultas() : true;
        boolean permiteEstudios = entidad.getPermiteEstudios() != null ? entidad.getPermiteEstudios() : true;
        int cantidadPlantas = (entidad.getCantidadPlantas() != null && entidad.getCantidadPlantas() >= 1)
                ? entidad.getCantidadPlantas()
                : 1;

        List<String> estudios = normalizador != null
                ? normalizador.resolverEstudiosCentro(
                        entidad.getId(),
                        entidad.getNombre(),
                        permiteEstudios,
                        entidad.getEstudiosHabilitados())
                : Collections.emptyList();

        return CentroSaludDtoResponse.builder()
                .id(entidad.getId())
                .nombre(entidad.getNombre())
                .tipo(entidad.getTipo())
                .direccion(entidad.getDireccion())
                .telefono(entidad.getTelefono())
                .ciudad(entidad.getCiudad())
                .horarioAtencion(entidad.getHorarioAtencion())
                .disponibleGuardia(entidad.getDisponibleGuardia())
                .latitud(entidad.getLatitud())
                .longitud(entidad.getLongitud())
                .activo(entidad.getActivo())
                .permiteConsultas(permiteConsultas)
                .permiteEstudios(permiteEstudios)
                .cantidadPlantas(cantidadPlantas)
                .estudiosDisponibles(estudios)
                .build();
    }

    private ConsultorioFisicoDtoResponse paraDtoConsultorio(ConsultorioFisico entidad) {
        return ConsultorioFisicoDtoResponse.builder()
                .id(entidad.getId())
                .centroSaludId(entidad.getCentroSaludId())
                .numero(entidad.getNumero())
                .planta(entidad.getPlanta())
                .especialidadAsignada(entidad.getEspecialidadAsignada())
                .activo(entidad.getActivo())
                .build();
    }
}
