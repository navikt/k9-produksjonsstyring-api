create table if not exists BEHANDLING_PROSESS_EVENTS_TILBAKE
(
    ID   VARCHAR(100) NOT NULL PRIMARY KEY,
    DATA jsonb        NOT NULL
);