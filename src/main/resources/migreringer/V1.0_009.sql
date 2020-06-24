drop table if exists FERDIGSTILTE_BEHANDLINGER;
create table if not exists FERDIGSTILTE_BEHANDLINGER
(
    behandlingType VARCHAR(100) NOT NULL PRIMARY KEY,
    dato           DATE         NOT NULL,
    data           jsonb,
    unique (behandlingType, dato)
);
