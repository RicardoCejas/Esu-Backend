package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.repositorio.RefreshTokenRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenRevocationService {

    private final RefreshTokenRepositorio refreshTokenRepositorio;

    /**
     * Ejecuta la revocación de la familia comprometida en una transacción independiente (REQUIRES_NEW)
     * para garantizar que persista en la base de datos incluso cuando la petición rechaza el token
     * lanzando una BadCredentialsException en la transacción principal.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revocarFamiliaPorCompromiso(String tokenFamilyId) {
        if (tokenFamilyId == null || tokenFamilyId.isBlank()) return;
        log.warn("Persistiendo revocación preventiva de familia de refresh tokens comprometida familyId={} (transacción independiente REQUIRES_NEW)", tokenFamilyId);
        refreshTokenRepositorio.revocarPorTokenFamilyId(tokenFamilyId);
    }

    /**
     * Ejecuta la revocación de todas las sesiones activas en una transacción independiente (REQUIRES_NEW)
     * para garantizar que persista en la base de datos incluso cuando la petición rechaza el token
     * lanzando una BadCredentialsException en la transacción principal.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revocarTodasLasSesionesPorCompromiso(Long usuarioId) {
        log.warn("Persistiendo revocación preventiva de todas las sesiones activas para usuario ID={} (transacción independiente REQUIRES_NEW)", usuarioId);
        refreshTokenRepositorio.revocarTodosPorUsuarioId(usuarioId);
    }
}
