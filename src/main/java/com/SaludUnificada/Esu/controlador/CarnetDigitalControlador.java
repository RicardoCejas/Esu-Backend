package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.servicio.CarnetDigitalServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class CarnetDigitalControlador {

    private final CarnetDigitalServicio carnetDigitalServicio;

    @GetMapping(value = "/{id}/carnet-digital/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("@securityPolicy.canAccessPatient(#id)")
    public ResponseEntity<byte[]> obtenerQrCredencial(@PathVariable Long id) {
        byte[] qrBytes = carnetDigitalServicio.generarQrCarnetDigital(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("inline", "credencial_qr_" + id + ".png");
        return ResponseEntity.ok().headers(headers).body(qrBytes);
    }

    @PostMapping("/carnet-digital/verificar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> verificarQr(@RequestBody Map<String, String> payload) {
        String qrContent = payload != null ? payload.get("qrContent") : null;
        return ResponseEntity.ok(carnetDigitalServicio.verificarQrCarnet(qrContent));
    }
}
