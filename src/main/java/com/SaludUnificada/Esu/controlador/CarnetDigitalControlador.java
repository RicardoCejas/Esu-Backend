package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.servicio.CarnetDigitalServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
public class CarnetDigitalControlador {

    @Autowired
    private CarnetDigitalServicio carnetDigitalServicio;

    @GetMapping(value = "/{id}/carnet-digital/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('PACIENTE', 'RECEPCIONISTA', 'MEDICO', 'PROFESIONAL', 'ADMIN')")
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
        String qrContent = payload.get("qrContent");
        return ResponseEntity.ok(carnetDigitalServicio.verificarQrCarnet(qrContent));
    }
}
