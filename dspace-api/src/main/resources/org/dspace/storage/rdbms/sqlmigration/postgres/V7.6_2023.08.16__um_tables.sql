--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE SEQUENCE proxies_seq;
create table proxies (proxies_id integer NOT NULL DEFAULT nextval('proxies_seq'), depositor_id uuid, proxy_id uuid, handle varchar(100), uuid varchar(100));


CREATE SEQUENCE umrestricted_seq;
create table umrestricted (id integer NOT NULL DEFAULT nextval('umrestricted_seq'), item_id varchar(250), release_date varchar(250));


create sequence consolidatedstatstable_seq;
create table consolidatedstatstable (consolidatedstatstable_id integer NOT NULL DEFAULT nextval('consolidatedstatstable_seq'), colldt varchar(250), collid integer, collname text, handle varchar(250), title text, publisher text, inside_count integer, out_count integer,  bit_count integer,  bitum integer, bitnonum integer, itemuminside integer, itemnonuminside integer, itemumoutside integer, itemnonumoutside integer, bitreferer text, itemoutsidereferer text, collid_uuid character varying(250));

create table individual_stats (id SERIAL PRIMARY KEY, email varchar(200), enabled varchar(10));

create table admin_stats ( collemail varchar (200) primary key );




create table crawlerip ( date varchar(100), ip text, site text, ipcount integer);

create table all_ips ( ip varchar(30), hostname varchar(250), stat_date varchar(250));
CREATE UNIQUE INDEX ip_idx ON all_ips (ip);

create table crawlers_dspace ( ip text);

create table rawstats ( colldt varchar (250), total_item_view integer, total_bit_view integer);




create table statsdata (item_id integer, handle varchar(250), authors text, title text, dateadded text,
 numofbits       integer,
 display_authors text, 
 display_title   text, 
 publisher       text,
 collid          integer, 
 collid_uuid     varchar(250),
 item_uuid       varchar(250));


CREATE UNIQUE INDEX stats_date ON statsdata (dateadded);
CREATE UNIQUE INDEX stats_date_handle ON statsdata (handle, dateadded);
CREATE UNIQUE INDEX stats_handle_idx ON statsdata (handle);

create table bitstreamipstatsdata
(date     text,
 collid   varchar(250),
 item_id  varchar(250),
 handle   varchar(250),
 isumip   integer,
 referer  text,
 filename text);
CREATE UNIQUE INDEX bitip_idx ON bitstreamipstatsdata (date, collid, handle, isumip);

create table itemipstatsdata 
(date            text,
 collid          varchar(250),
 item_id         varchar(250),
 handle          varchar(250),
 insideindicator integer,
 isumip          integer,
 referer         text);

CREATE UNIQUE INDEX itemip_idx ON itemipstatsdata (date, collid, handle, isumip, insideindicator);

create table bitspecificdata (
 collid      integer,
 colldt      varchar(250),
 handle      varchar(250),
 filename    text,
 bitum       integer,
 bitnonum    integer,
 total       integer,
 bitreferer  text,
 collid_uuid varchar(250));


CREATE UNIQUE INDEX bitspecificdata_colldt_idx ON bitspecificdata (colldt);
CREATE UNIQUE INDEX bitspecificdata_handle_filename_idx ON bitspecificdata (handle, filename);
CREATE UNIQUE INDEX bitspecificdata_handle_idx ON bitspecificdata (handle);

create table statsidanddate (
 collid      integer, 
 colldt      varchar(250), 
 collid_uuid varchar(250));


CREATE UNIQUE INDEX stats_1 ON statsidanddate (collid_uuid);
CREATE UNIQUE INDEX stats_2 ON statsidanddate (colldt);
CREATE UNIQUE INDEX stats_3 ON statsidanddate (collid_uuid, colldt);








