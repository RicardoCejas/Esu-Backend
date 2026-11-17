package com.SaludUnificada.Esu.servicio;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CatalogoFarmacologicoServicio {

    private static final Set<String> MONODROGAS_AUTORIZADAS = Set.of(
            "amoxicilina",
            "amoxicilina + acido clavulanico",
            "azitromicina",
            "cefalexina",
            "ciprofloxacina",
            "ibuprofeno",
            "paracetamol",
            "diclofenac sodico",
            "dipirona",
            "enalapril",
            "losartan",
            "amlodipina",
            "atenolol",
            "metformina",
            "glimepirida",
            "insulina nph",
            "omeprazol",
            "pantoprazol",
            "salbutamol",
            "budesonide",
            "loratadina",
            "cetirizina",
            "clonazepam",
            "alprazolam",
            "sertralina",
            "levotiroxina",
            "atorvastatina"
    );

    public boolean esMonodrogaValida(String droga) {
        if (droga == null || droga.trim().isEmpty()) {
            return false;
        }
        String normalizada = normalizar(droga);
        return MONODROGAS_AUTORIZADAS.contains(normalizada) ||
               MONODROGAS_AUTORIZADAS.stream().anyMatch(normalizada::contains);
    }

    @Cacheable("catalogoMonodrogas")
    public List<String> getCatalogoOficial() {
        return MONODROGAS_AUTORIZADAS.stream()
                .sorted()
                .map(d -> Character.toUpperCase(d.charAt(0)) + d.substring(1))
                .collect(Collectors.toList());
    }

    public String normalizar(String texto) {
        if (texto == null) return "";
        String s = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD);
        return s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public List<String> buscarMonodrogas(String query) {
        if (query == null || query.isBlank()) {
            return getCatalogoOficial();
        }
        String qNorm = normalizar(query);
        return MONODROGAS_AUTORIZADAS.stream()
                .filter(d -> d.contains(qNorm))
                .sorted()
                .collect(Collectors.toList());
    }
}
