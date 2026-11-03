--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE SEQUENCE IF NOT EXISTS proxies_seq;
create table IF NOT EXISTS proxies (proxies_id integer NOT NULL DEFAULT NEXT VALUE FOR proxies_seq, depositor_id uuid, proxy_id uuid, handle varchar(100));

CREATE SEQUENCE IF NOT EXISTS umrestricted_seq;
create table IF NOT EXISTS umrestricted (id integer NOT NULL DEFAULT NEXT VALUE FOR umrestricted_seq, item_id varchar(250), release_date varchar(250));

create sequence IF NOT EXISTS consolidatedstatstable_seq;
create table IF NOT EXISTS consolidatedstatstable (consolidatedstatstable_id integer NOT NULL DEFAULT NEXT VALUE FOR consolidatedstatstable_seq, colldt varchar(250), collid integer, collname varchar, handle varchar(250), title varchar, publisher varchar, inside_count integer, out_count integer,  bit_count integer,  bitum integer, bitnonum integer, itemuminside integer, itemnonuminside integer, itemumoutside integer, itemnonumoutside integer, bitreferer varchar, itemoutsidereferer varchar, collid_uuid character varying(250));

create table IF NOT EXISTS individual_stats (id INTEGER AUTO_INCREMENT PRIMARY KEY, email varchar(200), enabled varchar(10));

create table IF NOT EXISTS admin_stats ( collemail varchar (200) primary key );

create table IF NOT EXISTS crawlerip ( date varchar(100), ip varchar, site varchar, ipcount integer);

create table IF NOT EXISTS all_ips ( ip varchar(30), hostname varchar(250), stat_date varchar(250));
CREATE UNIQUE INDEX IF NOT EXISTS ip_idx ON all_ips (ip);

create table IF NOT EXISTS crawlers_dspace ( ip varchar);

create table IF NOT EXISTS rawstats ( colldt varchar (250), total_item_view integer, total_bit_view integer);

create table IF NOT EXISTS statsdata (item_id integer, handle varchar(250), authors varchar, title varchar, dateadded varchar,
 numofbits       integer,
 display_authors varchar, 
 display_title   varchar, 
 publisher       varchar,
 collid          integer, 
 collid_uuid     varchar(250),
 item_uuid       varchar(250));


CREATE UNIQUE INDEX IF NOT EXISTS stats_date ON statsdata (dateadded);
CREATE UNIQUE INDEX IF NOT EXISTS stats_date_handle ON statsdata (handle, dateadded);
CREATE UNIQUE INDEX IF NOT EXISTS stats_handle_idx ON statsdata (handle);

create table IF NOT EXISTS bitstreamipstatsdata
(date     varchar,
 collid   varchar(250),
 item_id  varchar(250),
 handle   varchar(250),
 isumip   integer,
 referer  varchar,
 filename varchar);
CREATE UNIQUE INDEX IF NOT EXISTS bitip_idx ON bitstreamipstatsdata (date, collid, handle, isumip);

create table IF NOT EXISTS itemipstatsdata 
(date            varchar,
 collid          varchar(250),
 item_id         varchar(250),
 handle          varchar(250),
 insideindicator integer,
 isumip          integer,
 referer         varchar);

CREATE UNIQUE INDEX IF NOT EXISTS itemip_idx ON itemipstatsdata (date, collid, handle, isumip, insideindicator);

create table IF NOT EXISTS bitspecificdata (
 collid      integer,
 colldt      varchar(250),
 handle      varchar(250),
 filename    varchar,
 bitum       integer,
 bitnonum    integer,
 total       integer,
 bitreferer  varchar,
 collid_uuid varchar(250));

CREATE UNIQUE INDEX IF NOT EXISTS bitspecificdata_colldt_idx ON bitspecificdata (colldt);
CREATE UNIQUE INDEX IF NOT EXISTS bitspecificdata_handle_filename_idx ON bitspecificdata (handle, filename);
CREATE UNIQUE INDEX IF NOT EXISTS bitspecificdata_handle_idx ON bitspecificdata (handle);

create table IF NOT EXISTS statsidanddate (
 collid      integer, 
 colldt      varchar(250), 
 collid_uuid varchar(250));

CREATE UNIQUE INDEX IF NOT EXISTS stats_1 ON statsidanddate (collid_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS stats_2 ON statsidanddate (colldt);
CREATE UNIQUE INDEX IF NOT EXISTS stats_3 ON statsidanddate (collid_uuid, colldt);

ALTER TABLE proxies ADD COLUMN uuid VARCHAR(100);






