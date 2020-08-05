drop table if exists DRIFTSMELDINGER;
create table if not exists DRIFTSMELDINGER
(
    ID      VARCHAR(100) NOT NULL PRIMARY KEY,
    dato    timestamp         NOT NULL,
    melding VARCHAR(4000) NOT NULL ,
    aktiv   BOOLEAN default false NOT NULL
);
