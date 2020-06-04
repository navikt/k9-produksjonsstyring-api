create table if not exists SISTE_BEHANDLINGER
(
    ID   VARCHAR(100) NOT NULL PRIMARY KEY,
    DATA jsonb        NOT NULL
);
