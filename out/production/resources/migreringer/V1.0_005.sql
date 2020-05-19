alter table SAKSBEHANDLER DROP CONSTRAINT saksbehandler_pkey;
alter TABLE SAKSBEHANDLER ALTER COLUMN SAKSBEHANDLERID DROP NOT NULL;
alter table saksbehandler add primary key (epost);
