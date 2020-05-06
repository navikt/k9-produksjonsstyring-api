create table if not exists RESERVASJON
(
    ID   VARCHAR(100) NOT NULL PRIMARY KEY,
    DATA jsonb        NOT NULL
);