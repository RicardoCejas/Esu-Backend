package com.SaludUnificada.Esu.config;

import com.SaludUnificada.Esu.entidad.Cie10;
import com.SaludUnificada.Esu.repositorio.Cie10Repositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Cie10DataInitializer implements CommandLineRunner {

    @Autowired
    private Cie10Repositorio cie10Repositorio;

    @Override
    public void run(String... args) {
        if (cie10Repositorio.count() > 0) {
            return;
        }

        List<Cie10> catalogo = List.of(
            // Infecciones y Respiratorio
            new Cie10("J00", "Rinofaringitis aguda [resfriado común]", "Enfermedades del sistema respiratorio"),
            new Cie10("J01", "Sinusitis aguda", "Enfermedades del sistema respiratorio"),
            new Cie10("J02.9", "Faringitis aguda, no especificada", "Enfermedades del sistema respiratorio"),
            new Cie10("J03.9", "Amigdalitis aguda, no especificada", "Enfermedades del sistema respiratorio"),
            new Cie10("J06.9", "Infección aguda de las vías respiratorias superiores, no especificada", "Enfermedades del sistema respiratorio"),
            new Cie10("J18.9", "Neumonía, no especificada", "Enfermedades del sistema respiratorio"),
            new Cie10("J20.9", "Bronquitis aguda, no especificada", "Enfermedades del sistema respiratorio"),
            new Cie10("J45.9", "Asma, no especificada", "Enfermedades del sistema respiratorio"),
            new Cie10("J44.9", "Enfermedad pulmonar obstructiva crónica [EPOC], no especificada", "Enfermedades del sistema respiratorio"),

            // Cardiovascular
            new Cie10("I10", "Hipertensión esencial (primaria)", "Enfermedades del sistema circulatorio"),
            new Cie10("I11.9", "Enfermedad cardíaca hipertensiva sin insuficiencia cardíaca", "Enfermedades del sistema circulatorio"),
            new Cie10("I20.9", "Angina de pecho, no especificada", "Enfermedades del sistema circulatorio"),
            new Cie10("I21.9", "Infarto agudo del miocardio, sin otra especificación", "Enfermedades del sistema circulatorio"),
            new Cie10("I50.9", "Insuficiencia cardíaca, no especificada", "Enfermedades del sistema circulatorio"),

            // Endocrino / Metabólico
            new Cie10("E11.9", "Diabetes mellitus tipo 2 sin mención de complicación", "Enfermedades endocrinas y metabólicas"),
            new Cie10("E10.9", "Diabetes mellitus tipo 1 sin mención de complicación", "Enfermedades endocrinas y metabólicas"),
            new Cie10("E03.9", "Hipotiroidismo, no especificado", "Enfermedades endocrinas y metabólicas"),
            new Cie10("E66.9", "Obesidad, no especificada", "Enfermedades endocrinas y metabólicas"),
            new Cie10("E78.5", "Hiperlipidemia, no especificada", "Enfermedades endocrinas y metabólicas"),

            // Digestivo
            new Cie10("K21.9", "Enfermedad por reflujo gastroesofágico sin esofagitis", "Enfermedades del sistema digestivo"),
            new Cie10("K29.7", "Gastritis, no especificada", "Enfermedades del sistema digestivo"),
            new Cie10("K52.9", "Gastroenteritis y colitis no infecciosas, no especificadas", "Enfermedades del sistema digestivo"),
            new Cie10("K58.9", "Síndrome del intestino irritable sin diarrea", "Enfermedades del sistema digestivo"),

            // Osteomuscular / Dolores
            new Cie10("M54.5", "Lumbago no especificado", "Enfermedades del sistema osteomuscular"),
            new Cie10("M54.2", "Cervicalgia", "Enfermedades del sistema osteomuscular"),
            new Cie10("M25.5", "Dolor articular", "Enfermedades del sistema osteomuscular"),
            new Cie10("M79.1", "Mialgia", "Enfermedades del sistema osteomuscular"),

            // Neurológico / Psicológico
            new Cie10("G43.9", "Migraña, no especificada", "Enfermedades del sistema nervioso"),
            new Cie10("G44.2", "Cefalea tensional", "Enfermedades del sistema nervioso"),
            new Cie10("F41.1", "Trastorno de ansiedad generalizada", "Trastornos mentales y del comportamiento"),
            new Cie10("F32.9", "Episodio depresivo, no especificado", "Trastornos mentales y del comportamiento"),

            // Dermatología y Alergias
            new Cie10("L20.9", "Dermatitis atópica, no especificada", "Enfermedades de la piel"),
            new Cie10("L50.9", "Urticaria, no especificada", "Enfermedades de la piel"),
            new Cie10("T78.4", "Alergia no especificada", "Traumatismos y envenenamientos"),

            // Factores que influyen en el estado de salud (Rutina / Prevención)
            new Cie10("Z00.0", "Examen médico general", "Factores que influyen en el estado de salud"),
            new Cie10("Z01.0", "Examen de ojos y de la visión", "Factores que influyen en el estado de salud"),
            new Cie10("Z23", "Necesidad de inmunización contra enfermedad bacteriana única", "Factores que influyen en el estado de salud"),
            new Cie10("Z76.0", "Emisión de prescripción médica de repetición", "Factores que influyen en el estado de salud")
        );

        cie10Repositorio.saveAll(catalogo);
        System.out.println("✅ Catálogo CIE-10 inicializado con éxito (" + catalogo.size() + " registros cargados).");
    }
}
