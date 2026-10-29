package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.EstudioAdjuntoDtoResponse;
import com.SaludUnificada.Esu.entidad.EstudioAdjunto;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.EstudioAdjuntoRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstudioAdjuntoServicio {

    private final EstudioAdjuntoRepositorio estudioRepositorio;
    private final PacienteRepositorio pacienteRepositorio;

    @Value("${esu.uploads.estudios.dir:uploads/estudios}")
    private String directorioCargas;

    private static final long MAX_FILE_SIZE = 15L * 1024 * 1024;
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Set<String> EXTENSIONES_PROHIBIDAS = Set.of(
            "exe", "bat", "cmd", "sh", "msi", "jar", "bin", "com", "vbs", "js", "ps1", "scr", "pif"
    );

    @Transactional
    public EstudioAdjuntoDtoResponse almacenarEstudio(
            MultipartFile archivo,
            Long pacienteId,
            Long evolucionId,
            String tipoEstudio,
            LocalDate fechaEstudio
    ) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo adjunto no puede estar vacío.");
        }

        if (archivo.getSize() > MAX_FILE_SIZE) {
            throw new ReglaNegocioException("El tamaño del archivo excede el límite máximo permitido de 15MB.");
        }

        if (!pacienteRepositorio.existsById(pacienteId)) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId);
        }

        String nombreOriginal = StringUtils.cleanPath(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo");
        String extension = extraerExtension(nombreOriginal);

        if (EXTENSIONES_PROHIBIDAS.contains(extension)) {
            throw new ReglaNegocioException("Seguridad: Carga de archivos ejecutables o potencialmente dañinos prohibida (.exe, .bat, etc.).");
        }

        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ReglaNegocioException("Formato no soportado. Extensiones permitidas: PDF, PNG, JPG, JPEG.");
        }

        try {
            byte[] bytes = archivo.getBytes();
            String sha256 = calcularSha256(bytes);

            Path directorioDestino = Paths.get(directorioCargas).toAbsolutePath().normalize();
            if (!Files.exists(directorioDestino)) {
                Files.createDirectories(directorioDestino);
            }

            String nombreAlmacenado = UUID.randomUUID().toString() + "." + extension;
            Path rutaArchivo = directorioDestino.resolve(nombreAlmacenado);
            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            EstudioAdjunto entidad = new EstudioAdjunto();
            entidad.setPacienteId(pacienteId);
            entidad.setEvolucionId(evolucionId);
            entidad.setNombreArchivo(nombreOriginal);
            entidad.setNombreAlmacenado(nombreAlmacenado);
            entidad.setRutaAlmacenamiento(rutaArchivo.toString());
            entidad.setMimeType(archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream");
            entidad.setTamanioBytes(archivo.getSize());
            entidad.setChecksumSha256(sha256);
            entidad.setTipoEstudio(tipoEstudio != null ? tipoEstudio.toUpperCase().trim() : "INFORME");
            entidad.setFechaEstudio(fechaEstudio != null ? fechaEstudio : LocalDate.now());

            EstudioAdjunto guardado = estudioRepositorio.save(entidad);
            return mapearADto(guardado);
        } catch (IOException e) {
            throw new RuntimeException("Error al almacenar el archivo en disco: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Resource descargarArchivo(Long estudioId) {
        EstudioAdjunto entidad = estudioRepositorio.findById(estudioId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Estudio médico adjunto no encontrado con ID: " + estudioId));

        try {
            Path baseDir = Paths.get(directorioCargas).toAbsolutePath().normalize();
            Path ruta = Paths.get(entidad.getRutaAlmacenamiento()).toAbsolutePath().normalize();

            if (!ruta.startsWith(baseDir)) {
                throw new SecurityException("Acceso no autorizado: la ruta del archivo se encuentra fuera del directorio permitido.");
            }

            Resource resource = new UrlResource(ruta.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new NoEncontradoExcepcion("El archivo físico del estudio no se encuentra disponible en disco.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ruta de archivo inválida: " + e.getMessage(), e);
        }
    }

    public void validarAccesoEstudio(EstudioAdjunto estudio, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Debe autenticarse para acceder a los estudios.");
        }
        boolean isStaff = authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_MEDICO") ||
                a.getAuthority().equals("ROLE_PROFESIONAL"));

        if (!isStaff) {
            Paciente paciente = pacienteRepositorio.findByUsuarioEmail(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no está vinculado a un perfil de paciente."));
            if (!paciente.getId().equals(estudio.getPacienteId())) {
                throw new AccessDeniedException("No posee autorización para descargar estudios médicos pertenecientes a otro paciente.");
            }
        }
    }

    @Transactional(readOnly = true)
    public EstudioAdjunto obtenerPorId(Long estudioId) {
        return estudioRepositorio.findById(estudioId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Estudio médico adjunto no encontrado con ID: " + estudioId));
    }

    @Transactional(readOnly = true)
    public List<EstudioAdjuntoDtoResponse> listarPorPaciente(Long pacienteId) {
        return estudioRepositorio.findByPacienteIdOrderByFechaCargaDesc(pacienteId)
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EstudioAdjuntoDtoResponse> listarPorEvolucion(Long evolucionId) {
        return estudioRepositorio.findByEvolucionIdOrderByFechaCargaDesc(evolucionId)
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    private String extraerExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx > 0 && idx < filename.length() - 1) {
            return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private String calcularSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no soportado", e);
        }
    }

    private EstudioAdjuntoDtoResponse mapearADto(EstudioAdjunto entidad) {
        return new EstudioAdjuntoDtoResponse(
                entidad.getId(),
                entidad.getPacienteId(),
                entidad.getEvolucionId(),
                entidad.getNombreArchivo(),
                entidad.getMimeType(),
                entidad.getTamanioBytes(),
                entidad.getChecksumSha256(),
                entidad.getTipoEstudio(),
                entidad.getFechaEstudio(),
                entidad.getFechaCarga(),
                "/api/estudios/" + entidad.getId() + "/descargar"
        );
    }
    @Transactional
    public void eliminarEstudio(Long estudioId, Authentication auth) {
        EstudioAdjunto entidad = obtenerPorId(estudioId);
        validarAccesoEstudio(entidad, auth);
        try {
            Path ruta = Paths.get(entidad.getRutaAlmacenamiento()).toAbsolutePath().normalize();
            Files.deleteIfExists(ruta);
        } catch (Exception ignored) {
        }
        estudioRepositorio.delete(entidad);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEspacioOcupadoPorPaciente(Long pacienteId) {
        List<EstudioAdjunto> lista = estudioRepositorio.findByPacienteIdOrderByFechaCargaDesc(pacienteId);
        long totalBytes = lista.stream().mapToLong(e -> e.getTamanioBytes() != null ? e.getTamanioBytes() : 0L).sum();
        double totalMb = Math.round((totalBytes / (1024.0 * 1024.0)) * 100.0) / 100.0;
        Map<String, Object> res = new HashMap<>();
        res.put("pacienteId", pacienteId);
        res.put("totalEstudios", lista.size());
        res.put("totalBytes", totalBytes);
        res.put("totalMegabytes", totalMb);
        return res;
    }
}
