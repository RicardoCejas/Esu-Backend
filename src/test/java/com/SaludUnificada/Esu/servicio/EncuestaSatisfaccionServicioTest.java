package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.EncuestaSatisfaccionDtoRequest;
import com.SaludUnificada.Esu.dto.response.EncuestaSatisfaccionDtoResponse;
import com.SaludUnificada.Esu.entidad.EncuestaSatisfaccion;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.repositorio.EncuestaSatisfaccionRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EncuestaSatisfaccionServicioTest {

    @Mock
    private EncuestaSatisfaccionRepositorio encuestaRepositorio;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    private EncuestaSatisfaccionServicio encuestaServicio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        encuestaServicio = new EncuestaSatisfaccionServicio(encuestaRepositorio, turnoRepositorio);
    }

    @Test
    void registrarEncuestaPersisteEvaluacionSatisfactoriamente() {
        Usuario usuario = new Usuario();
        usuario.setEmail("paciente@correo.com");

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);

        Turno turno = new Turno();
        turno.setId(100L);
        turno.setEstado("ATENDIDO");
        turno.setPaciente(paciente);

        EncuestaSatisfaccionDtoRequest request = EncuestaSatisfaccionDtoRequest.builder()
                .puntuacion(5)
                .tiempoEsperaPercibido("ADECUADO")
                .comentario("Muy buena atención.")
                .build();

        when(turnoRepositorio.findById(100L)).thenReturn(Optional.of(turno));
        when(encuestaRepositorio.existsByTurnoId(100L)).thenReturn(false);
        when(encuestaRepositorio.save(any(EncuestaSatisfaccion.class))).thenAnswer(inv -> {
            EncuestaSatisfaccion e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EncuestaSatisfaccionDtoResponse response = encuestaServicio.registrarEncuesta(100L, request, "paciente@correo.com");

        assertNotNull(response);
        assertEquals(5, response.getPuntuacion());
        assertEquals("ADECUADO", response.getTiempoEsperaPercibido());
        verify(encuestaRepositorio).save(any(EncuestaSatisfaccion.class));
    }

    @Test
    void registrarEncuestaRechazaTurnoNoAtendido() {
        Turno turno = new Turno();
        turno.setId(101L);
        turno.setEstado("PENDIENTE");

        EncuestaSatisfaccionDtoRequest request = EncuestaSatisfaccionDtoRequest.builder()
                .puntuacion(4)
                .tiempoEsperaPercibido("EXCELENTE")
                .build();

        when(turnoRepositorio.findById(101L)).thenReturn(Optional.of(turno));

        assertThrows(IllegalStateException.class, () -> encuestaServicio.registrarEncuesta(101L, request, "paciente@correo.com"));
        verify(encuestaRepositorio, never()).save(any());
    }

    @Test
    void registrarEncuestaRechazaEncuestaDuplicada() {
        Turno turno = new Turno();
        turno.setId(102L);
        turno.setEstado("ATENDIDO");

        EncuestaSatisfaccionDtoRequest request = EncuestaSatisfaccionDtoRequest.builder()
                .puntuacion(3)
                .tiempoEsperaPercibido("ADECUADO")
                .build();

        when(turnoRepositorio.findById(102L)).thenReturn(Optional.of(turno));
        when(encuestaRepositorio.existsByTurnoId(102L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> encuestaServicio.registrarEncuesta(102L, request, "paciente@correo.com"));
        verify(encuestaRepositorio, never()).save(any());
    }

    @Test
    void obtenerMetricasSatisfaccionCalculaPromedioGlobal() {
        when(encuestaRepositorio.obtenerPromedioPuntuacionGlobal()).thenReturn(4.85);
        when(encuestaRepositorio.contarTotalEncuestas()).thenReturn(42L);

        Map<String, Object> metricas = encuestaServicio.obtenerMetricasSatisfaccion();

        assertNotNull(metricas);
        assertEquals(42L, metricas.get("totalEncuestas"));
        assertEquals(4.85, metricas.get("promedioGlobalEstrellas"));
        assertEquals("SATISFACTORIO", metricas.get("npsEstimado"));
    }
}
