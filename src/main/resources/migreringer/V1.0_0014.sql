drop table if exists NYE_OG_FERDIGSTILTE;
create table if not exists NYE_OG_FERDIGSTILTE
(
    behandlingType VARCHAR(100) NOT NULL,
    fagsakYtelseType VARCHAR(100) NOT NULL,
    dato           DATE         NOT NULL,
    ferdigstilte   jsonb,
    nye            jsonb,
    primary key (behandlingType, fagsakYtelseType, dato),
    unique (behandlingType, fagsakYtelseType, dato)
);
