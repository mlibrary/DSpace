--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

CREATE SEQUENCE openurltracker_seq;

CREATE TABLE OpenUrlTracker
(
  tracker_id  INTEGER PRIMARY KEY,
  tracker_url VARCHAR(1000),
  uploaddate  DATE
);
