
create table if not exists AVDELING
(
    ID            serial                                 NOT NULL PRIMARY KEY,
    DATA          jsonb                                  NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    VENT_AARSAK   VARCHAR(100) DEFAULT '-'               NOT NULL
);

create table if not exists OPPGAVE_EVENT_LOGG
(
    ID            serial                                 NOT NULL PRIMARY KEY,
    DATA          jsonb                                  NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    VENT_AARSAK   VARCHAR(100) DEFAULT '-'               NOT NULL
);

create table if not exists RESERVASJON_EVENT_LOGG
(
    ID            serial                                 NOT NULL PRIMARY KEY,
    DATA          jsonb                                  NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    VENT_AARSAK   VARCHAR(100) DEFAULT '-'               NOT NULL
);

create table if not exists EVENTMOTTAK_FEILLOGG
(
    ID            serial                                 NOT NULL PRIMARY KEY,
    DATA          jsonb                                  NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    VENT_AARSAK   VARCHAR(100) DEFAULT '-'               NOT NULL
);


create table if not exists BEHANDLING_PROSESS_EVENTS_K9
(
    ID   VARCHAR(100) NOT NULL PRIMARY KEY,
    DATA jsonb        NOT NULL
);

create table if not exists OPPGAVE
(
    ID   VARCHAR(100) NOT NULL PRIMARY KEY,
    DATA jsonb        NOT NULL
);
