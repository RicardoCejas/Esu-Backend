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

    public List<PlantillaEvolucionDtoResponse> buscarPorTermino(String termino) {
        if (termino == null || termino.isBlank()) {
            return listarPlantillas(null);
        }
        return plantillaRepositorio.findByActivoTrueAndTituloContainingIgnoreCase(termino.trim())
                .stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
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

    @Transactional
    public PlantillaEvolucionDtoResponse actualizarPlantilla(Long id, PlantillaEvolucionDtoRequest request) {
        PlantillaEvolucion p = plantillaRepositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoExcepcion("Plantilla no encontrada con ID: " + id));

        p.setTitulo(request.getTitulo());
        p.setEstructuraAnamnesis(request.getEstructuraAnamnesis());
        p.setEstructuraExamenFisico(request.getEstructuraExamenFisico());
        p.setTratamientoSugerido(request.getTratamientoSugerido());
        if (request.getEsPublica() != null) {
            p.setEsPublica(request.getEsPublica());
        }

        if (request.getEspecialidadId() != null) {
            Especialidad esp = especialidadRepositorio.findById(request.getEspecialidadId())
                .orElseThrow(() -> new NoEncontradoExcepcion("Especialidad no encontrada con ID: " + request.getEspecialidadId()));
            p.setEspecialidad(esp);
        }

        PlantillaEvolucion guardada = plantillaRepositorio.save(p);
        return mapearADto(guardada);
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
                Especialidad pediatria = obtenerOCrearEspecialidad("Pediatria");
                Especialidad clinica = obtenerOCrearEspecialidad("Clinica Medica");
                Especialidad cardiologia = obtenerOCrearEspecialidad("Cardiologia");

                PlantillaEvolucion p1 = new PlantillaEvolucion();
                p1.setTitulo("Control de Crecimiento y Desarrollo Infantil");
                p1.setEspecialidad(pediatria);
                p1.setEstructuraAnamnesis("Paciente en seguimiento de puericultura y desarrollo psicomotor. Alimentacion actual: seno materno / formula. Sueno continuo. Inmunizaciones completas para la edad segun calendario nacional.");
                p1.setEstructuraExamenFisico("Vigil, reactivo, tono muscular conservado. Fontanela anterior normotensa. Auscultacion cardiopulmonar s/r agregados. Abdomen blando, depresible, no visceromegalias. Percentiles peso/edad y talla/edad en rango.");
                p1.setTratamientoSugerido("Continuar lactancia/alimentacion complementaria. Pautas de alarma respiratoria y digestiva. Proximo control mensual.");
                p1.setEsPublica(true);
                p1.setActivo(true);
                p1.setFechaCreacion(LocalDateTime.now());
                plantillaRepositorio.save(p1);

                PlantillaEvolucion p2 = new PlantillaEvolucion();
                p2.setTitulo("Consulta General del Adulto - Seguimiento");
                p2.setEspecialidad(clinica);
                p2.setEstructuraAnamnesis("Paciente adulto concurre a consulta de control clinico de rutina. No refiere disnea ni dolor precordial. Habitos fisiologicos conservados.");
                p2.setEstructuraExamenFisico("Buen estado general, orientado temporo-espacialmente. Normohidratado. Murmullo vesicular conservado bilateralmente. R1 y R2 normofonicos. Abdomen blando, indoloro a la palpacion profunda.");
                p2.setTratamientoSugerido("Solicitud de laboratorio de rutina y orina completa. Mantener dieta hiposodica y actividad fisica regular aerobica.");
                p2.setEsPublica(true);
                p2.setActivo(true);
                p2.setFechaCreacion(LocalDateTime.now());
                plantillaRepositorio.save(p2);

                PlantillaEvolucion p3 = new PlantillaEvolucion();
                p3.setTitulo("Evaluacion Cardiovascular e Hipertension Arterial");
                p3.setEspecialidad(cardiologia);
                p3.setEstructuraAnamnesis("Paciente con antecedentes de HTA de larga data. Registro ambulatorio con cifras tensionales limitrofes. Niega palpitaciones, mareos ortostaticos o dolor toracico anginoso.");
                p3.setEstructuraExamenFisico("Ruidos cardiacos ritmicos y regulares. Silencios libres, sin soplos agregados. Pulsos perifericos presentes y simetricos. Sin edemas perifericos en miembros inferiores ni ingurgitacion yugular.");
                p3.setTratamientoSugerido("Ajuste posologico de antihipertensivo (Enalapril / Losartan). Indicacion de ECG basal y ecocardiograma Doppler color. Control tensional bisemanal.");
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
}
