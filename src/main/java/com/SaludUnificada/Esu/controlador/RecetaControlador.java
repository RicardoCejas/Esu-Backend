package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.request.RecetaDigitalDtoRequest;
import com.SaludUnificada.Esu.dto.response.RecetaDigitalDtoResponse;
import com.SaludUnificada.Esu.entidad.RecetaDigital;
import com.SaludUnificada.Esu.mapper.ClinicaMapper;
import com.SaludUnificada.Esu.seguridad.AutorizacionServicio;
import com.SaludUnificada.Esu.servicio.RecetaDigitalServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Recetas Digitales", description = "Emisión, validación y descarga oficial de recetas médicas digitales")
public class RecetaControlador {

    @Autowired
    private RecetaDigitalServicio recetaServicio;

    @Autowired
    private AutorizacionServicio autorizacionServicio;

    @Autowired
    private ClinicaMapper clinicaMapper;

    @PostMapping("/recetas")
    @Operation(summary = "Emitir nueva receta médica digital")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<RecetaDigitalDtoResponse> emitirReceta(@Valid @RequestBody RecetaDigitalDtoRequest request) {
        return new ResponseEntity<>(
                clinicaMapper.paraDto(recetaServicio.emitirReceta(request)),
                HttpStatus.CREATED);
    }

    @PostMapping("/recetas/validar")
    @Operation(summary = "Validar requisitos previos para prescripción médica")
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMIN')")
    public ResponseEntity<Map<String, Object>> validarPrescripcion(@RequestBody RecetaDigitalDtoRequest request) {
        return ResponseEntity.ok(recetaServicio.validarPrescripcion(request));
    }

    @GetMapping("/recetas/{id}")
    @Operation(summary = "Obtener datos de una receta médica")
    public ResponseEntity<RecetaDigitalDtoResponse> obtenerReceta(@PathVariable Long id) {
        RecetaDigital receta = recetaServicio.obtenerPorId(id);
        if (receta.getPaciente() != null && receta.getPaciente().getId() != null) {
            autorizacionServicio.verificarAccesoAPaciente(receta.getPaciente().getId());
        }
        return ResponseEntity.ok(clinicaMapper.paraDto(receta));
    }

    @GetMapping("/recetas/{id}/pdf")
    @Operation(summary = "Descargar receta médica oficial en PDF con código QR y Hash SHA-256")
    public ResponseEntity<byte[]> descargarRecetaPdf(@PathVariable Long id) {
        RecetaDigital receta = recetaServicio.obtenerPorId(id);
        if (receta.getPaciente() != null && receta.getPaciente().getId() != null) {
            autorizacionServicio.verificarAccesoAPaciente(receta.getPaciente().getId());
        }
        byte[] pdfBytes = recetaServicio.generarPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receta_digital_" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/public/verificar/{hash}")
    @Operation(summary = "Endpoint público para verificar autenticidad e integridad mediante QR o Hash")
    public ResponseEntity<Map<String, Object>> verificarDocumentoPublico(@PathVariable String hash) {
        return ResponseEntity.ok(recetaServicio.verificarDocumentoPublico(hash));
    }
}
