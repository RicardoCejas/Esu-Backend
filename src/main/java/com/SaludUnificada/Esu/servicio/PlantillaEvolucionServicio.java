package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.request.PlantillaEvolucionDtoRequest;
import com.SaludUnificada.Esu.dto.response.PlantillaEvolucionDtoResponse;
import com.SaludUnificada.Esu.entidad.Especialidad;
import com.SaludUnificada.Esu.entidad.PlantillaEvolucion;
import com.SaludUnificada.Esu.error.NoEncontradoExcepcion;
import com.SaludUnificada.Esu.repositorio.EspecialidadRepositorio;
import com.SaludUnificada.Esu.repositorio.PlantillaEvolucionRepositorio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlantillaEvolucionServicio {

    @Autowired
    private PlantillaEvolucionRepositorio plantillaRepositorio;

    @Autowired
    private EspecialidadRepositorio especialidadRepositorio;

    public List<PlantillaEvolucionDtoResponse> listarPlantillas(Long especialidadId) {
        List<PlantillaEvolucion> lista;
        if (especialidadId != null) {
            lista = plantillaRepositorio.findByEspecialidadIdAndActivoTrue(especialidadId);
        } else {
            lista = plantillaRepositorio.findByActivoTrue();
        }

        return lista.stream().map(this::mapearADto).collect(Collectors.toList());
    }

    @Transactional
    public PlantillaEvolucionDtoResponse crearPlantilla(PlantillaEvolucionDtoRequest request) {
        PlantillaEvolucion p = new PlantillaEvolucion();
        p.setTitulo(request.getTitulo());
        p.setEstructuraAnamnesis(request.getEstructuraAnamnesis());
        p.setEstructuraExamenFisico(request.getEstructuraExamenFisico());
        p.setTratamientoSugerido(request.getTratamientoSugerido());
        p.setEsPublica(request.getEsPublica() != null ? request.getEsPublica() : true);
        p.setActivo(true);
        p.setFechaCreacion(LocalDateTime.now());

        if (request.getEspecialidadId() != null) {
            Especialidad esp = especialidadRepositorio.findById(request.getEspecialidadId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Especialidad no encontrada con ID: " + request.getEspecialidadId()));
            p.setEspecialidad(esp);
        }

        PlantillaEvolucion guardada = plantillaRepositorio.save(p);
        return mapearADto(guardada);
    }

    private PlantillaEvolucionDtoResponse mapearADto(PlantillaEvolucion p) {
        return PlantillaEvolucionDtoResponse.builder()
            .id(p.getId())
            .titulo(p.getTitulo())
            .especialidadId(p.getEspecialidad() != null ? p.getEspecialidad().getId() : null)
            .especialidadNombre(p.getEspecialidad() != null ? p.getEspecialidad().getNombre() : "General")
            .estructuraAnamnesis(p.getEstructuraAnamnesis())
            .estructuraExamenFisico(p.getEstructuraExamenFisico())
            .tratamientoSugerido(p.getTratamientoSugerido())
            .esPublica(p.getEsPublica())
            .activo(p.getActivo())
            .fechaCreacion(p.getFechaCreacion())
            .build();
    }

    @PostConstruct
    public void precargarPlantillasIniciales() {
        try {
            if (plantillaRepositorio.count() == 0) {

                Especialidad pediatria = obtenerOCrearEspecialidad("Pediatría");
                Especialidad clinica = obtenerOCrearEspecialidad("Clínica Médica");
                Especialidad cardiologia = obtenerOCrearEspecialidad("Cardiología");

                PlantillaEvolucion p1 = new PlantillaEvolucion();
                p1.setTitulo("Control de Crecimiento y Desarrollo Infantil");
                p1.setEspecialidad(pediatria);
                p1.setEstructuraAnamnesis("Paciente en seguimiento de puericultura y desarrollo psicomotor. Alimentación actual: seno materno / fórmula. Sueño continuo. Inmunizaciones completas para la edad según calendario nacional.");
                p1.setEstructuraExamenFisico("Vigil, reactivo, tono muscular conservado. Fontanela anterior normotensa. Auscultación cardiopulmonar s/r agregados. Abdomen blando, depresible, no visceromegalias. Percentiles peso/edad y talla/edad en rango.");
                p1.setTratamientoSugerido("Continuar lactancia/alimentación complementaria. Pautas de alarma respiratoria y digestiva. Próximo control mensual.");
                p1.setEsPublica(true);
                p1.setActivo(true);
                p1.setFechaCreacion(LocalDateTime.now());
                plantillaRepositorio.save(p1);

                PlantillaEvolucion p2 = new PlantillaEvolucion();
                p2.setTitulo("Consulta General del Adulto - Seguimiento");
                p2.setEspecialidad(clinica);
                p2.setEstructuraAnamnesis("Paciente adulto concurre a consulta de control clínico de rutina. No refiere disnea ni dolor precordial. Hábitos fisiológicos conservados.");
                p2.setEstructuraExamenFisico("Buen estado general, orientado témporo-espacialmente. Normohidratado. Murmullo vesicular conservado bilateralmente. R1 y R2 normofonéticos. Abdomen blando, indoloro a la palpación profunda.");
                p2.setTratamientoSugerido("Solicitud de laboratorio de rutina y orina completa. Mantener dieta hiposódica y actividad física regular aeróbica.");
                p2.setEsPublica(true);
                p2.setActivo(true);
                p2.setFechaCreacion(LocalDateTime.now());
                plantillaRepositorio.save(p2);

                PlantillaEvolucion p3 = new PlantillaEvolucion();
                p3.setTitulo("Evaluación Cardiovascular e Hipertensión Arterial");
                p3.setEspecialidad(cardiologia);
                p3.setEstructuraAnamnesis("Paciente con antecedentes de HTA de larga data. Registro ambulatorio con cifras tensionales limítrofes. Niega palpitaciones, mareos ortostáticos o dolor torácico anginoso.");
                p3.setEstructuraExamenFisico("Ruidos cardíacos rítmicos y regulares. Silencios libres, sin soplos agregados. Pulsos periféricos presentes y simétricos. Sin edemas periféricos en miembros inferiores ni ingurgitación yugular.");
                p3.setTratamientoSugerido("Ajuste posológico de antihipertensivo (Enalapril / Losartán). Indicación de ECG basal y ecocardiograma Doppler color. Control tensional bisemanal.");
                p3.setEsPublica(true);
                p3.setActivo(true);
                p3.setFechaCreacion(LocalDateTime.now());
                plantillaRepositorio.save(p3);
            }
        } catch (Exception ignored) {

        }
    }

    private Especialidad obtenerOCrearEspecialidad(String nombre) {
        List<Especialidad> lista = especialidadRepositorio.findByNombreContainingIgnoreCase(nombre);
        if (!lista.isEmpty()) {
            return lista.get(0);
        }
        Especialidad e = new Especialidad();
        e.setNombre(nombre);
        return especialidadRepositorio.save(e);
    }
    public PlantillaEvolucionDtoResponse obtenerPorId(Long id) {
        PlantillaEvolucion p = plantillaRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Plantilla no encontrada con ID: " + id));
        return mapearADto(p);
    }

    @Transactional
    public void cambiarEstadoPlantilla(Long id, boolean activo) {
        PlantillaEvolucion p = plantillaRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Plantilla no encontrada con ID: " + id));
        p.setActivo(activo);
        plantillaRepositorio.save(p);
    }
}
