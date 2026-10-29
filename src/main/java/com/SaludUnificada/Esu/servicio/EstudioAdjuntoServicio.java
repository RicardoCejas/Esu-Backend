package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.EspacioOcupadoDtoResponse;
import com.SaludUnificada.Esu.dto.response.EstudioAdjuntoDtoResponse;
import com.SaludUnificada.Esu.entidad.EstudioAdjunto;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.repositorio.EstudioAdjuntoRepositorio;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstudioAdjuntoServicio {

    private final EstudioAdjuntoRepositorio estudioRepositorio;
    private final PacienteRepositorio pacienteRepositorio;

    @Value("${esu.uploads.estudios.dir:uploads/estudios}")
    private String directorioCargas;

    public static final long MAX_FILE_SIZE = 15L * 1024 * 1024; // 15 MB
    public static final long MAX_CUOTA_PACIENTE_BYTES = 100L * 1024 * 1024; // 100 MB quota per patient
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
            throw new IllegalArgumentException("El archivo adjunto no puede estar vacio.");
        }

        if (archivo.getSize() > MAX_FILE_SIZE) {
            throw new ReglaNegocioException("El tamano del archivo excede el limite maximo permitido de 15MB.");
        }

        if (!pacienteRepositorio.existsById(pacienteId)) {
            throw new NoEncontradoExcepcion("Paciente no encontrado con ID: " + pacienteId);
        }

        // Validate storage quota for patient
        long espacioActual = estudioRepositorio.findByPacienteIdOrderByFechaCargaDesc(pacienteId)
                .stream()
                .mapToLong(e -> e.getTamanioBytes() != null ? e.getTamanioBytes() : 0L)
                .sum();
        if (espacioActual + archivo.getSize() > MAX_CUOTA_PACIENTE_BYTES) {
            throw new ReglaNegocioException("Se ha superado el cupo maximo de almacenamiento para el paciente (100 MB).");
        }

        String nombreOriginal = StringUtils.cleanPath(archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo");
        String extension = extraerExtension(nombreOriginal);

        if (EXTENSIONES_PROHIBIDAS.contains(extension)) {
            throw new ReglaNegocioException("Seguridad: Carga de archivos ejecutables o potencialmente daninos prohibida (.exe, .bat, etc.).");
        }

        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ReglaNegocioException("Formato no soportado. Extensiones permitidas: PDF, PNG, JPG, JPEG.");
        }

        Path rutaArchivo = null;
        try {
            // Validate content using magic bytes
            validarMagicBytes(archivo, extension);

            Path directorioDestino = Paths.get(directorioCargas).toAbsolutePath().normalize();
            if (!Files.exists(directorioDestino)) {
                Files.createDirectories(directorioDestino);
            }

            String nombreAlmacenado = UUID.randomUUID().toString() + "." + extension;
            rutaArchivo = directorioDestino.resolve(nombreAlmacenado);

            // Stream directly to disk while computing SHA-256 without loading all bytes in memory
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = archivo.getInputStream();
                 DigestInputStream dis = new DigestInputStream(in, digest);
                 OutputStream out = Files.newOutputStream(rutaArchivo)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = dis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            String sha256 = bytesToHex(digest.digest());

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
            entidad.setFechaCarga(LocalDateTime.now());

            EstudioAdjunto guardado = estudioRepositorio.save(entidad);
            return mapearADto(guardado);
        } catch (NoSuchAlgorithmException e) {
            limpiarArchivoFisico(rutaArchivo);
            throw new RuntimeException("Algoritmo criptografico no disponible: " + e.getMessage(), e);
        } catch (IOException e) {
            limpiarArchivoFisico(rutaArchivo);
            throw new RuntimeException("Error al almacenar el archivo en disco: " + e.getMessage(), e);
        } catch (Exception e) {
            limpiarArchivoFisico(rutaArchivo);
            throw e;
        }
    }

    private void validarMagicBytes(MultipartFile archivo, String extension) throws IOException {
        try (InputStream in = new BufferedInputStream(archivo.getInputStream())) {
            byte[] header = new byte[8];
            int read = in.read(header);
            if (read < 4) {
                throw new ReglaNegocioException("Archivo invalido o corrupto.");
            }

            if ("pdf".equalsIgnoreCase(extension)) {
                // PDF magic bytes: %PDF (0x25, 0x50, 0x44, 0x46)
                if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                    throw new ReglaNegocioException("El contenido del archivo no coincide con un documento PDF legitimo.");
                }
            } else if ("png".equalsIgnoreCase(extension)) {
                // PNG magic bytes: \x89PNG (0x89, 0x50, 0x4E, 0x47)
                if ((header[0] & 0xFF) != 0x89 || header[1] != 0x50 || header[2] != 0x4E || header[3] != 0x47) {
                    throw new ReglaNegocioException("El contenido del archivo no coincide con una imagen PNG valida.");
                }
            } else if ("jpg".equalsIgnoreCase(extension) || "jpeg".equalsIgnoreCase(extension)) {
                // JPEG magic bytes: FF D8 FF
                if ((header[0] & 0xFF) != 0xFF || (header[1] & 0xFF) != 0xD8 || (header[2] & 0xFF) != 0xFF) {
                    throw new ReglaNegocioException("El contenido del archivo no coincide con una imagen JPG/JPEG valida.");
                }
            }
        }
    }

    private void limpiarArchivoFisico(Path ruta) {
        if (ruta != null) {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException ex) {
                log.warn("No se pudo eliminar el archivo residual tras un fallo de almacenamiento: {}", ruta, ex);
            }
        }
    }

    public Resource descargarArchivo(Long estudioId) {
        EstudioAdjunto estudio = obtenerPorId(estudioId);
        try {
            Path basePath = Paths.get(directorioCargas).toAbsolutePath().normalize();
            Path ruta = Paths.get(estudio.getRutaAlmacenamiento()).toAbsolutePath().normalize();
            if (!ruta.startsWith(basePath)) {
                throw new SecurityException("Acceso a ruta de archivo no permitida (Path Traversal prevenido).");
            }
            Resource resource = new UrlResource(ruta.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new NoEncontradoExcepcion("El archivo fisico del estudio no se encuentra disponible en disco.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ruta de archivo invalida: " + e.getMessage(), e);
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
                    .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no esta vinculado a un perfil de paciente."));
            if (!paciente.getId().equals(estudio.getPacienteId())) {
                throw new AccessDeniedException("No posee autorizacion para descargar estudios medicos pertenecientes a otro paciente.");
            }
        }
    }

    @Transactional(readOnly = true)
    public EstudioAdjunto obtenerPorId(Long estudioId) {
        return estudioRepositorio.findById(estudioId)
                .orElseThrow(() -> new NoEncontradoExcepcion("Estudio medico adjunto no encontrado con ID: " + estudioId));
    }

    @Transactional(readOnly = true)
    public List<EstudioAdjuntoDtoResponse> listarPorPaciente(Long pacienteId) {
        return estudioRepositorio.findByPacienteIdOrderByFechaCargaDesc(pacienteId)
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarEstudiosPorPaciente(Long pacienteId) {
        return estudioRepositorio.countByPacienteId(pacienteId);
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

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
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
        Path basePath = Paths.get(directorioCargas).toAbsolutePath().normalize();
        Path ruta = Paths.get(entidad.getRutaAlmacenamiento()).toAbsolutePath().normalize();
        if (ruta.startsWith(basePath)) {
            limpiarArchivoFisico(ruta);
        }
        estudioRepositorio.delete(entidad);
    }

    @Transactional(readOnly = true)
    public EspacioOcupadoDtoResponse obtenerEspacioOcupadoPorPaciente(Long pacienteId) {
        List<EstudioAdjunto> lista = estudioRepositorio.findByPacienteIdOrderByFechaCargaDesc(pacienteId);
        long totalBytes = lista.stream().mapToLong(e -> e.getTamanioBytes() != null ? e.getTamanioBytes() : 0L).sum();
        double totalMb = Math.round((totalBytes / (1024.0 * 1024.0)) * 100.0) / 100.0;
        return new EspacioOcupadoDtoResponse(pacienteId, lista.size(), totalBytes, totalMb);
    }
}
