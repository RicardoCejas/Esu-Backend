package com.SaludUnificada.Esu.servicio;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;

@Component
public class CatalogoEspecialidadesNormalizador {

    public static final List<String> ESPECIALIDADES_CANONICAS_CONSULTORIO = List.of(
            "Pediatría",
            "Clínica Médica",
            "Traumatología",
            "Ginecología y Obstetricia",
            "Psiquiatría",
            "Urología",
            "Neurología",
            "Cardiología",
            "Hemoterapia",
            "Fisiatría",
            "Oftalmología",
            "Diabetología",
            "Cirugía General",
            "Psicología",
            "Odontología",
            "Trabajo Social",
            "Nutrición",
            "Fonoaudiología",
            "Kinesiología",
            "Fisioterapia",
            "Psicomotricidad",
            "Psicopedagogía",
            "Dermatología",
            "Endocrinología",
            "Flebología",
            "Gastroenterología",
            "Oncología",
            "Otorrinolaringología (ORL)"
    );

    public static final List<String> CATEGORIAS_SERVICIOS = List.of(
            "Laboratorio",
            "Diagnóstico por Imágenes",
            "Vacunatorio",
            "Junta de Discapacidad"
    );

    public static final List<String> MODALIDADES_ESTUDIOS = List.of(
            "Laboratorio de Análisis Clínicos",
            "Radiografía Digital",
            "Ecografía General",
            "Ecografía Mamaria"
    );

    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        // Clínica Médica
        ALIASES.put("clinica medica", "Clínica Médica");
        ALIASES.put("clínica medica", "Clínica Médica");
        ALIASES.put("clinica médica", "Clínica Médica");
        ALIASES.put("clinica medica / medicina general", "Clínica Médica");
        ALIASES.put("clínica médica / medicina general", "Clínica Médica");
        ALIASES.put("medicina general", "Clínica Médica");

        // Pediatría
        ALIASES.put("pediatria", "Pediatría");
        ALIASES.put("pediatria y neonatologia", "Pediatría");
        ALIASES.put("pediatría y neonatología", "Pediatría");

        // Traumatología
        ALIASES.put("traumatologia", "Traumatología");
        ALIASES.put("traumatologia y ortopedia", "Traumatología");
        ALIASES.put("traumatología y ortopedia", "Traumatología");

        // Ginecología y Obstetricia
        ALIASES.put("ginecologia", "Ginecología y Obstetricia");
        ALIASES.put("ginecología", "Ginecología y Obstetricia");
        ALIASES.put("toco ginecologia", "Ginecología y Obstetricia");
        ALIASES.put("toco ginecología", "Ginecología y Obstetricia");
        ALIASES.put("tocoginecologia", "Ginecología y Obstetricia");
        ALIASES.put("tocoginecología", "Ginecología y Obstetricia");
        ALIASES.put("toco-ginecología", "Ginecología y Obstetricia");
        ALIASES.put("toco-ginecologia", "Ginecología y Obstetricia");
        ALIASES.put("obstetricia", "Ginecología y Obstetricia");

        // Cirugía General
        ALIASES.put("cirugia", "Cirugía General");
        ALIASES.put("cirugía", "Cirugía General");
        ALIASES.put("cirugia general", "Cirugía General");
        ALIASES.put("cirugía general y laparoscópica", "Cirugía General");
        ALIASES.put("cirugia general y laparoscopica", "Cirugía General");

        // Psicología
        ALIASES.put("psicologia", "Psicología");
        ALIASES.put("psicologia adultos y niños", "Psicología");
        ALIASES.put("psicología adultos y niños", "Psicología");

        // Psicomotricidad
        ALIASES.put("psicomotricista", "Psicomotricidad");

        // Cardiología y Hemoterapia
        ALIASES.put("cardiologia", "Cardiología");
        ALIASES.put("cardiologia clinica", "Cardiología");
        ALIASES.put("cardiología clínica", "Cardiología");
        ALIASES.put("hemoterapia", "Hemoterapia");

        // Oftalmología y Odontología
        ALIASES.put("oftalmologia", "Oftalmología");
        ALIASES.put("odontologia", "Odontología");
        ALIASES.put("odontologia integral", "Odontología");
        ALIASES.put("odontología integral", "Odontología");

        // Otorrinolaringología
        ALIASES.put("orl", "Otorrinolaringología (ORL)");
        ALIASES.put("otorrinolaringologia", "Otorrinolaringología (ORL)");
        ALIASES.put("otorrinolaringología", "Otorrinolaringología (ORL)");
    }

    public String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }
        String limpio = nombre.trim();
        String claveBusqueda = limpio.toLowerCase(Locale.ROOT);

        if (ALIASES.containsKey(claveBusqueda)) {
            return ALIASES.get(claveBusqueda);
        }

        for (String canonica : ESPECIALIDADES_CANONICAS_CONSULTORIO) {
            if (canonica.equalsIgnoreCase(limpio)) {
                return canonica;
            }
        }

        return limpio;
    }

    public List<String> resolverSeparacion(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        if (raw.contains("–") || raw.contains("-")) {
            String[] partes = raw.split("[–\\-]");
            if (partes.length == 2 && partes[0].trim().equalsIgnoreCase("Cardiología") && partes[1].trim().equalsIgnoreCase("Hemoterapia")) {
                return List.of("Cardiología", "Hemoterapia");
            }
        }
        return List.of(normalizarNombre(raw));
    }

    public boolean esEspecialidadConsultorio(String nombre) {
        if (nombre == null) return false;
        String normalizado = normalizarNombre(nombre);
        return ESPECIALIDADES_CANONICAS_CONSULTORIO.stream().anyMatch(e -> e.equalsIgnoreCase(normalizado));
    }

    public boolean esCategoriaServicio(String nombre) {
        if (nombre == null) return false;
        return CATEGORIAS_SERVICIOS.stream().anyMatch(c -> c.equalsIgnoreCase(nombre.trim()));
    }

    public boolean esModalidadEstudio(String nombre) {
        if (nombre == null) return false;
        String s = nombre.trim().toLowerCase();
        return s.contains("laboratorio") || s.contains("radiograf") || s.contains("rayos") || s.contains("ecograf");
    }

    public List<String> obtenerEstudiosDisponiblesPorCentro(Long centroId, String nombreCentro) {
        String nombre = (nombreCentro != null) ? nombreCentro.toLowerCase() : "";

        if (centroId != null && (centroId == 5L || nombre.contains("maría isabel") || nombre.contains("maria isabel"))) {
            return Collections.emptyList();
        }

        if (centroId != null && (centroId == 2L || nombre.contains("cabrera"))) {
            return List.of(
                    "Laboratorio de Análisis Clínicos",
                    "Radiografía Digital",
                    "Ecografía General"
            );
        }

        // Hospital Crespo (1) y Centro de Diagnóstico (3):
        return List.of(
                "Laboratorio de Análisis Clínicos",
                "Radiografía Digital",
                "Ecografía General",
                "Ecografía Mamaria"
        );
    }

    public boolean esEstudioPermitido(Long centroId, String servicio) {
        if (servicio == null || servicio.isBlank()) return false;
        List<String> permitidos = obtenerEstudiosDisponiblesPorCentro(centroId, null);
        if (permitidos.isEmpty()) return false;

        String s = servicio.trim().toLowerCase();
        for (String p : permitidos) {
            String pLower = p.toLowerCase();
            if (pLower.contains(s) || s.contains(pLower)) {
                return true;
            }
            if (s.contains("laboratorio") && pLower.contains("laboratorio")) return true;
            if ((s.contains("radiograf") || s.contains("rayos")) && pLower.contains("radiograf")) return true;
            if (s.contains("mamaria") && pLower.contains("mamaria")) return true;
            if (s.contains("ecograf") && !s.contains("mamaria") && pLower.contains("ecografía general")) return true;
        }
        return false;
    }
}
