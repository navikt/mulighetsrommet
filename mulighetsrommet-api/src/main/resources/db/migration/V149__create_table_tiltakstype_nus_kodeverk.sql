CREATE TABLE tiltakstype_nus_kodeverk
(
    tiltakskode tiltakskode NOT NULL,
    code           TEXT NOT NULL,
    version        TEXT NOT NULL,
    PRIMARY KEY (tiltakskode, code, version),
    FOREIGN KEY (tiltakskode) REFERENCES tiltakstype (tiltakskode) ON DELETE CASCADE,
    FOREIGN KEY (code, version) REFERENCES nus_kodeverk (code, version) ON DELETE CASCADE
);

