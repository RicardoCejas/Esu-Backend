package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.TurnoPublicoDtoRequest;
import com.SaludUnificada.Esu.dto.response.TurnoDtoResponse;
import com.SaludUnificada.Esu.entidad.Paciente;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.entidad.Turno;
import com.SaludUnificada.Esu.entidad.Usuario;
import com.SaludUnificada.Esu.error.ReglaNegocioException;
import com.SaludUnificada.Esu.mapper.TurnoMapper;
import com.SaludUnificada.Esu.repositorio.PacienteRepositorio;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.RecordatorioTurnoRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.SaludUnificada.Esu.dto.response.PacienteDtoResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnoPublicoReservaTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Cordoba");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T13:00:00Z"), ZONE);

    @Mock private TurnoRepositorio turnoRepositorio;
    @Mock private TurnoMapper turnoMapper;
    @Mock private PacienteRepositorio pacienteRepositorio;
    @Mock private ProfesionalRepositorio profesionalRepositorio;
    @Mock private RecordatorioTurnoRepositorio recordatorioTurnoRepositorio;

    private TurnoServicio turnoServicio;
    private Profesional profesional;
    private LocalDateTime fechaFutura;

    @BeforeEach
    void setUp() {
        turnoServicio = new TurnoServicio(
                turnoRepositorio,
                turnoMapper,
                pacienteRepositorio,
                profesionalRepositorio,
                recordatorioTurnoRepositorio,
                CLOCK,
                ZONE,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                30
        );

        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Dr. Juan");
        profesional.setApellido("Perez");

        // Fecha futura dentro de la jornada: 2026-10-15 10:00 AM
        fechaFutura = LocalDateTime.of(2026, 10, 15, 10, 0);
    }

    @Test
    @DisplayName("Idempotencia: si ya existe una reserva con el mismo idempotencyKey, retorna el turno existente")
    void reservarTurnoPublico_idempotencia_retornaTurnoPrevio() {
        TurnoPublicoDtoRequest request = TurnoPublicoDtoRequest.builder()
                .fechaHora(fechaFutura)
                .profesionalId(1L)
                .dni("38123456")
                .nombre("Maria")
                .apellido("Lopez")
                .telefono("3549112233")
                .idempotencyKey("idem-key-abc-123")
                .build();

        Paciente pacPrevio = new Paciente();
        pacPrevio.setId(10L);
        pacPrevio.setDni("38123456");

        Turno previo = new Turno();
        previo.setId(77L);
        previo.setIdempotencyKey("idem-key-abc-123");
        previo.setProfesional(profesional);
        previo.setFechaHora(fechaFutura);
        previo.setPaciente(pacPrevio);

        TurnoDtoResponse responseDto = new TurnoDtoResponse();
        responseDto.setId(77L);
        PacienteDtoResponse pacDto = new PacienteDtoResponse();
        pacDto.setId(10L);
        pacDto.setDni("38123456");
        pacDto.setTelefono("3549112233");
        pacDto.setEmail("maria@gmail.com");
        responseDto.setPaciente(pacDto);

        when(turnoRepositorio.findByIdempotencyKey("idem-key-abc-123")).thenReturn(Optional.of(previo));
        when(turnoMapper.paraDto(previo)).thenReturn(responseDto);

        TurnoDtoResponse result = turnoServicio.reservarTurnoPublico(request, null);

        assertNotNull(result);
        assertEquals(77L, result.getId());
        verify(turnoRepositorio, never()).save(any(Turno.class));
    }

    @Test
    @DisplayName("Turnero público: reserva sin autenticación con DNI de paciente registrado asocia el paciente y confirma reserva")
    void reservarTurnoPublico_pacienteExistenteSinAutenticacion_confirmaReservaAsociandoPaciente() {
        TurnoPublicoDtoRequest request = TurnoPublicoDtoRequest.builder()
                .fechaHora(fechaFutura)
                .profesionalId(1L)
                .dni("38123456")
                .nombre("Maria")
                .apellido("Lopez")
                .telefono("3549112233")
                .email("familiar@gmail.com")
                .build();

        when(profesionalRepositorio.findByIdForUpdate(1L)).thenReturn(Optional.of(profesional));
        when(turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(eq(1L), any(), any())).thenReturn(false);

        Paciente pacienteRegistrado = new Paciente();
        pacienteRegistrado.setId(55L);
        pacienteRegistrado.setDni("38123456");

        when(pacienteRepositorio.findByDni("38123456")).thenReturn(Optional.of(pacienteRegistrado));
        when(turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(eq(55L), any(), any())).thenReturn(false);

        Turno guardado = new Turno();
        guardado.setId(101L);
        when(turnoRepositorio.save(any(Turno.class))).thenReturn(guardado);
        TurnoDtoResponse responseDto = new TurnoDtoResponse();
        responseDto.setId(101L);
        when(turnoMapper.paraDto(guardado)).thenReturn(responseDto);

        TurnoDtoResponse result = turnoServicio.reservarTurnoPublico(request, null);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        verify(turnoRepositorio).save(argThat(t -> t.getPaciente().getId().equals(55L)));
    }

    @Test
    @DisplayName("Paciente autenticado con DNI coincidente: asocia la reserva a su perfil verificado")
    void reservarTurnoPublico_pacienteAutenticado_confirmaReserva() {
        TurnoPublicoDtoRequest request = TurnoPublicoDtoRequest.builder()
                .fechaHora(fechaFutura)
                .profesionalId(1L)
                .dni("38123456")
                .nombre("Maria")
                .apellido("Lopez")
                .telefono("3549112233")
                .build();

        when(profesionalRepositorio.findByIdForUpdate(1L)).thenReturn(Optional.of(profesional));
        when(turnoRepositorio.existsByProfesionalIdAndFechaHoraAndEstadoNot(eq(1L), any(), any())).thenReturn(false);

        Paciente pacienteAutenticado = new Paciente();
        pacienteAutenticado.setId(55L);
        pacienteAutenticado.setDni("38123456");
        Usuario usuario = new Usuario();
        usuario.setEmail("maria@gmail.com");
        pacienteAutenticado.setUsuario(usuario);

        when(pacienteRepositorio.findByUsuarioEmail("maria@gmail.com")).thenReturn(Optional.of(pacienteAutenticado));
        when(turnoRepositorio.existsByPacienteIdAndFechaHoraAndEstadoNot(eq(55L), any(), any())).thenReturn(false);

        Turno guardado = new Turno();
        guardado.setId(100L);
        when(turnoRepositorio.save(any(Turno.class))).thenReturn(guardado);
        TurnoDtoResponse responseDto = new TurnoDtoResponse();
        responseDto.setId(100L);
        when(turnoMapper.paraDto(guardado)).thenReturn(responseDto);

        TurnoDtoResponse result = turnoServicio.reservarTurnoPublico(request, "maria@gmail.com");

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(turnoRepositorio).save(argThat(t -> t.getPaciente().getId().equals(55L)));
    }

    @Test
    @DisplayName("Fase 2 Idempotencia: rechazar recuperación si los parámetros de profesional difieren")
    void reservarTurnoPublico_idempotencia_distintoProfesional_lanzaExcepcion() {
        TurnoPublicoDtoRequest request = TurnoPublicoDtoRequest.builder()
                .fechaHora(fechaFutura)
                .profesionalId(1L)
                .dni("38123456")
                .nombre("Maria")
                .apellido("Lopez")
                .telefono("3549112233")
                .idempotencyKey("idem-key-abc-123")
                .build();

        Profesional otroProf = new Profesional();
        otroProf.setId(99L); // Profesional distinto

        Turno previo = new Turno();
        previo.setId(77L);
        previo.setIdempotencyKey("idem-key-abc-123");
        previo.setProfesional(otroProf);
        previo.setFechaHora(fechaFutura);

        when(turnoRepositorio.findByIdempotencyKey("idem-key-abc-123")).thenReturn(Optional.of(previo));

        assertThrows(IllegalStateException.class, () -> turnoServicio.reservarTurnoPublico(request, null));
    }

    @Test
    @DisplayName("Fase 2 Idempotencia: rechazar si un tercero anónimo intenta reutilizar la clave con otro DNI")
    void reservarTurnoPublico_idempotencia_distintoDniAnonimo_lanzaAccessDenied() {
        TurnoPublicoDtoRequest request = TurnoPublicoDtoRequest.builder()
                .fechaHora(fechaFutura)
                .profesionalId(1L)
                .dni("38999999") // DNI diferente
                .nombre("Intruso")
                .apellido("Perez")
                .telefono("3549112233")
                .idempotencyKey("idem-key-abc-123")
                .build();

        Paciente pacOriginal = new Paciente();
        pacOriginal.setId(10L);
        pacOriginal.setDni("38123456");

        Turno previo = new Turno();
        previo.setId(77L);
        previo.setIdempotencyKey("idem-key-abc-123");
        previo.setProfesional(profesional);
        previo.setFechaHora(fechaFutura);
        previo.setPaciente(pacOriginal);

        when(turnoRepositorio.findByIdempotencyKey("idem-key-abc-123")).thenReturn(Optional.of(previo));

        assertThrows(AccessDeniedException.class, () -> turnoServicio.reservarTurnoPublico(request, null));
    }
}

