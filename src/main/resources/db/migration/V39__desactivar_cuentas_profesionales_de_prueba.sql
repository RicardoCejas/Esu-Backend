-- Disable the demo clinician accounts created or reactivated by V38.
-- Keep V38 unchanged because Flyway checksums protect already-migrated databases.
UPDATE usuario
SET estado_activo = 0,
    email_verificado = 0
WHERE email IN (
    'maria.fernandez@esu.salud.gob.ar',
    'roberto.martinez@esu.salud.gob.ar',
    'florencia.castro@esu.salud.gob.ar',
    'laura.gomez@esu.salud.gob.ar',
    'esteban.alvarez@esu.salud.gob.ar',
    'alejandro.rossi@esu.salud.gob.ar',
    'mariana.lucero@esu.salud.gob.ar',
    'valeria.benitez@esu.salud.gob.ar',
    'hugo.acosta@esu.salud.gob.ar',
    'silvina.torres@esu.salud.gob.ar',
    'fernando.molina@esu.salud.gob.ar',
    'gustavo.navarro@esu.salud.gob.ar',
    'patricia.morales@esu.salud.gob.ar',
    'beatriz.suarez@esu.salud.gob.ar',
    'claudio.peralta@esu.salud.gob.ar',
    'javier.dominguez@esu.salud.gob.ar',
    'martin.vargas@esu.salud.gob.ar',
    'marcela.ibarra@esu.salud.gob.ar',
    'gabriela.sanchez@esu.salud.gob.ar',
    'gonzalo.romero@esu.salud.gob.ar',
    'romina.paredes@esu.salud.gob.ar',
    'andres.correa@esu.salud.gob.ar',
    'carolina.ramos@esu.salud.gob.ar',
    'lucia.albarracin@esu.salud.gob.ar',
    'nicolas.heredia@esu.salud.gob.ar',
    'marcos.valenzuela@esu.salud.gob.ar',
    'claudia.soria@esu.salud.gob.ar',
    'juancruz.carrizo@esu.salud.gob.ar',
    'daniela.maidana@esu.salud.gob.ar',
    'sofia.pereyra@esu.salud.gob.ar',
    'pablo.godoy@esu.salud.gob.ar',
    'camila.aguero@esu.salud.gob.ar',
    'martina.bustos@esu.salud.gob.ar',
    'sergio.luna@esu.salud.gob.ar'
)
  AND rol = 'PROFESIONAL';
