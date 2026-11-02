package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.CentroSaludDtoRequest;
import com.SaludUnificada.Esu.dto.request.ConsultorioFisicoDtoRequest;
import com.SaludUnificada.Esu.dto.response.CentroSaludDtoResponse;
import com.SaludUnificada.Esu.dto.response.ConsultorioFisicoDtoResponse;
import com.SaludUnificada.Esu.entidad.CentroSalud;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentroSaludServicio {

    private final CentroSaludRepositorio centroSaludRepositorio;
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
                .permiteEstudios(request.getPermiteEstudios() == null || request.getPermiteEstudios())
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
    public List<ConsultorioFisicoDtoResponse> listarConsultorios(Long centroSaludId) {
        List<ConsultorioFisico> lista = (centroSaludId != null)
                ? consultorioRepositorio.findByCentroSaludIdOrderByNumeroAsc(centroSaludId)
                : consultorioRepositorio.findByActivoTrueOrderByCentroSaludIdAscNumeroAsc();
        return lista.stream().map(this::paraDtoConsultorio).collect(Collectors.toList());
    }

    @Transactional
    public ConsultorioFisicoDtoResponse crearConsultorio(ConsultorioFisicoDtoRequest request) {
        if (request.getCentroSaludId() == null) {
            throw new IllegalArgumentException("El ID del centro de salud es obligatorio");
        }
        if (!centroSaludRepositorio.existsById(request.getCentroSaludId())) {
            throw new NoEncontradoExcepcion("Centro de salud no encontrado con ID: " + request.getCentroSaludId());
        }
        if (request.getNumero() == null || request.getNumero().isBlank()) {
            throw new IllegalArgumentException("El número de consultorio es obligatorio");
        }

        ConsultorioFisico consultorio = ConsultorioFisico.builder()
                .centroSaludId(request.getCentroSaludId())
                .numero(request.getNumero().trim())
                .planta(request.getPlanta() != null && !request.getPlanta().isBlank() ? request.getPlanta().trim() : "Planta Baja")
                .especialidadAsignada(request.getEspecialidadAsignada() != null ? request.getEspecialidadAsignada().trim() : null)
                .activo(request.getActivo() == null || request.getActivo())
                .build();

        return paraDtoConsultorio(consultorioRepositorio.save(consultorio));
    }

    @Transactional
    public void eliminarConsultorio(Long id) {
        if (!consultorioRepositorio.existsById(id)) {
            throw new NoEncontradoExcepcion("Consultorio físico no encontrado con ID: " + id);
        }
        consultorioRepositorio.deleteById(id);
    }

    private CentroSaludDtoResponse paraDto(CentroSalud entidad) {
        boolean permiteConsultas = entidad.getPermiteConsultas() != null ? entidad.getPermiteConsultas() : true;
        boolean permiteEstudios = entidad.getPermiteEstudios() != null ? entidad.getPermiteEstudios() : true;

        List<String> estudios = permiteEstudios && normalizador != null
                ? normalizador.obtenerEstudiosDisponiblesPorCentro(entidad.getId(), entidad.getNombre())
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
