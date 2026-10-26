package com.SaludUnificada.Esu.controlador;

import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.servicio.IProfesionalServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaPrestadoresControladorTest {

    @Mock
    private IProfesionalServicio profesionalServicio;

    @InjectMocks
    private ProfesionalControlador profesionalControlador;

    @Test
    @DisplayName("GET /api/profesionales/buscar-disponibles debe responder 200 OK con los prestadores filtrados")
    void testBuscarDisponibles_Endpoint() {
        LocalDateTime proximo = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);

        ProfesionalDtoResponse dto = ProfesionalDtoResponse.builder()
                .id(1L)
                .nombre("Carlos")
                .apellido("Martínez")
                .matricula("MP-1111")
                .proximoTurnoLibre(proximo)
                .build();

        when(profesionalServicio.buscarDisponibles(1L, 2L, true)).thenReturn(List.of(dto));

        ResponseEntity<List<ProfesionalDtoResponse>> res = profesionalControlador.buscarDisponibles(1L, 2L, true);

        assertNotNull(res);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
        assertEquals(proximo, res.getBody().get(0).getProximoTurnoLibre());
        verify(profesionalServicio).buscarDisponibles(1L, 2L, true);
    }
}
