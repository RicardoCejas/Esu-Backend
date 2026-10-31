package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.OutboxEventoDtoResponse;
import com.SaludUnificada.Esu.servicio.OutboxScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/outbox")
@RequiredArgsConstructor
@Tag(name = "Outbox Administrativo", description = "Monitoreo y reintento manual de eventos asÃ­ncronos y notificaciones")
public class OutboxControlador {

    private final OutboxScheduler outboxScheduler;

    @GetMapping({"/fallidos", "/problematicos"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar eventos outbox fallidos o con inconsistencias")
    public ResponseEntity<List<OutboxEventoDtoResponse>> listarFallidos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        int limite = Math.min(Math.max(tamano, 1), 100);
        return ResponseEntity.ok(outboxScheduler.listarEventosConProblemas(PageRequest.of(pagina, limite)));
    }

    @PostMapping("/{id}/reintentar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reintentar manualmente un evento outbox fallido o inconsistente")
    public ResponseEntity<Map<String, Object>> reintentarEvento(@PathVariable Long id) {
        boolean resultado = outboxScheduler.reintentarEventoManual(id);
        return ResponseEntity.ok(Map.of(
                "reintentado", resultado,
                "mensaje", "El evento outbox ID " + id + " ha sido reprogramado para procesamiento inmediato."
        ));
    }
}
