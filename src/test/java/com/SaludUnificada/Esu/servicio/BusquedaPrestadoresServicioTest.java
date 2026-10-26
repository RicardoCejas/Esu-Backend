package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.response.ProfesionalDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.Profesional;
import com.SaludUnificada.Esu.mapper.ProfesionalMapper;
import com.SaludUnificada.Esu.repositorio.ProfesionalRepositorio;
import com.SaludUnificada.Esu.repositorio.TurnoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaPrestadoresServicioTest {

    @Mock
    private ProfesionalRepositorio profesionalRepositorio;

    @Mock
    private ProfesionalMapper profesionalMapper;

    @Mock
    private TurnoRepositorio turnoRepositorio;

    @InjectMocks
    private ProfesionalServicio profesionalServicio;

    private Profesional prof1;
    private Profesional prof2;

    @BeforeEach
    void setUp() {
        Especialidad espPediatria = new Especialidad();
        espPediatria.setId(1L);
        espPediatria.setNombre("Pediatría");

        prof1 = new Profesional();
        prof1.setId(1L);
        prof1.setNombre("Carlos");
        prof1.setApellido("Martínez");
        prof1.setMatricula("MP-1111");
        prof1.setEstadoActivo(true);
        prof1.setEspecialidades(Set.of(espPediatria));

        prof2 = new Profesional();
        prof2.setId(2L);
        prof2.setNombre("Lucía");
        prof2.setApellido("González");
        prof2.setMatricula("MP-2222");
        prof2.setEstadoActivo(true);
        prof2.setEspecialidades(Set.of(espPediatria));
    }

    @Test
    @DisplayName("Debe buscar prestadores y adjuntar la fecha del próximo turno libre disponible")
    void testBuscarDisponibles_ConProximoTurno() {
        LocalDateTime proximoProf1 = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);

        when(profesionalRepositorio.buscarPrestadoresOptimizado(1L, 2L)).thenReturn(List.of(prof1, prof2));
        when(turnoRepositorio.findPrimerTurnoDisponible(eq(1L), any())).thenReturn(proximoProf1);
        when(turnoRepositorio.findPrimerTurnoDisponible(eq(2L), any())).thenReturn(null);

        ProfesionalDtoResponse dto1 = ProfesionalDtoResponse.builder()
                .id(1L)
                .nombre("Carlos")
                .apellido("Martínez")
                .proximoTurnoLibre(proximoProf1)
                .build();

        ProfesionalDtoResponse dto2 = ProfesionalDtoResponse.builder()
                .id(2L)
                .nombre("Lucía")
                .apellido("González")
                .proximoTurnoLibre(null)
                .build();

        when(profesionalMapper.paraDto(prof1, proximoProf1)).thenReturn(dto1);
        when(profesionalMapper.paraDto(prof2, null)).thenReturn(dto2);

        List<ProfesionalDtoResponse> resultado = profesionalServicio.buscarDisponibles(1L, 2L, false);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(proximoProf1, resultado.get(0).getProximoTurnoLibre());
        assertNull(resultado.get(1).getProximoTurnoLibre());
    }

    @Test
    @DisplayName("Debe filtrar profesionales sin turnos libres cuando soloConDisponibilidad es true")
    void testBuscarDisponibles_SoloConDisponibilidad_FiltraSinCupos() {
        LocalDateTime proximoProf1 = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);

        when(profesionalRepositorio.buscarPrestadoresOptimizado(1L, null)).thenReturn(List.of(prof1, prof2));
        when(turnoRepositorio.findPrimerTurnoDisponible(eq(1L), any())).thenReturn(proximoProf1);
        // Prof 2 no tiene turnos libres
        when(turnoRepositorio.findPrimerTurnoDisponible(eq(2L), any())).thenReturn(null);

        ProfesionalDtoResponse dto1 = ProfesionalDtoResponse.builder()
                .id(1L)
                .nombre("Carlos")
                .apellido("Martínez")
                .proximoTurnoLibre(proximoProf1)
                .build();

        when(profesionalMapper.paraDto(prof1, proximoProf1)).thenReturn(dto1);

        List<ProfesionalDtoResponse> resultado = profesionalServicio.buscarDisponibles(1L, null, true);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
        assertNotNull(resultado.get(0).getProximoTurnoLibre());
    }
}
