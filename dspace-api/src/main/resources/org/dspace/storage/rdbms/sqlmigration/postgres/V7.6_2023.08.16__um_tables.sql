--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE SEQUENCE proxies_seq;
create table proxies (proxies_id integer NOT NULL DEFAULT nextval('proxies_seq'), depositor_id uuid, proxy_id uuid, handle varchar(100));


CREATE SEQUENCE umrestricted_seq;
create table umrestricted (id integer NOT NULL DEFAULT nextval('umrestricted_seq'), item_id varchar(250), release_date varchar(250));


create sequence consolidatedstatstable_seq;
create table consolidatedstatstable (consolidatedstatstable_id integer NOT NULL DEFAULT nextval('consolidatedstatstable_seq'), colldt varchar(250), collid integer, collname text, handle varchar(250), title text, publisher text, inside_count integer, out_count integer,  bit_count integer,  bitum integer, bitnonum integer, itemuminside integer, itemnonuminside integer, itemumoutside integer, itemnonumoutside integer, bitreferer text, itemoutsidereferer text, collid_uuid character varying(250));

create table individual_stats (id SERIAL PRIMARY KEY, email varchar(200), enabled varchar(10));


create table admin_stats ( collemail varchar (200) primary key );
