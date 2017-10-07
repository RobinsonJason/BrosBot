-- Table: public.commands

-- DROP TABLE public.commands;

CREATE TABLE public.commands
(
    id SERIAL,
    mode varchar(30) NOT NULL,
    type varchar(20) NOT NULL,
    command varchar(30) NOT NULL,
    response varchar(255) NOT NULL,
    level integer NOT NULL,
    created timestamp NOT NULL,
    creator varchar(50) NOT NULL,
	enabled varchar(5) NOT NULL,
    CONSTRAINT commands_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.commands
    OWNER to postgres;
	
	
-- Table: public.modes

-- DROP TABLE public.modes;

CREATE TABLE public.modes
(
    id SERIAL,
    mode varchar(30) NOT NULL,
    created timestamp NOT NULL,
    creator varchar(50) NOT NULL,
	enabled varchar(5) NOT NULL,
    CONSTRAINT modes_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.modes
    OWNER to postgres;	