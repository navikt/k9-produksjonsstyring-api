create table if not exists OPPGAVE
(
    ID            VARCHAR (100)                          NOT NULL PRIMARY KEY,
    DATA          jsonb                                  NOT NULL
);

create table if not exists OPPGAVEKO
(
ID            serial                                    NOT NULL PRIMARY KEY,
DATA          jsonb                                     NOT NULL
);

create table if not exists BEHANDLING_PROSESS_EVENTS_K9
(
    ID   VARCHAR(100) NOT NULL PRIMARY KEY,
    DATA jsonb        NOT NULL
);
