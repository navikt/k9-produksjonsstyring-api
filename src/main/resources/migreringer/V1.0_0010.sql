alter table FERDIGSTILTE_BEHANDLINGER
drop column data;

alter table FERDIGSTILTE_BEHANDLINGER add column data jsonb;
