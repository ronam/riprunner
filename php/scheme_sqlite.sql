--
-- Table structures for rip runner for the SQLite engine
--

CREATE TABLE IF NOT EXISTS callouts 
(
id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
calltime datetime NOT NULL,
calltype varchar(20) NOT NULL,
address varchar(255) NOT NULL,
latitude DECIMAL(10,6) NOT NULL,
longitude DECIMAL(10,6) NOT NULL,
units varchar(255) NOT NULL,
status INTEGER NOT NULL DEFAULT 0,
updatetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
call_key varchar(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS callouts_response 
(
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  calloutid INTEGER NOT NULL,
  useracctid INTEGER NOT NULL,
  responsetime datetime NOT NULL,
  latitude DECIMAL(10,6) NOT NULL,  
  longitude DECIMAL(10,6) NOT NULL,  
  status int(11) NOT NULL DEFAULT 0,
  updatetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS callouts_geo_tracking (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  calloutid INTEGER NOT NULL,
  useracctid INTEGER NOT NULL,
  trackingtime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  latitude DECIMAL(10,6) NOT NULL,  
  longitude DECIMAL(10,6) NOT NULL,  
  trackingstatus INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_accounts 
(
id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
firehall_id varchar(80) NOT NULL,
user_id varchar(255) NOT NULL,
user_pwd varchar(255) NOT NULL,
mobile_phone varchar(25) NOT NULL DEFAULT '',
access INTEGER NOT NULL DEFAULT 0,
updatetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS login_attempts
(
id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
useracctid INTEGER NOT NULL,
time varchar(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS devicereg 
(
id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
registration_id TEXT NOT NULL,
firehall_id varchar(80) NOT NULL,
user_id varchar(255) NOT NULL,
updatetime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trigger_history 
(
id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
triggertime datetime NOT NULL,
type INTEGER NOT NULL DEFAULT 0,
firehall_id varchar(80) NOT NULL,
hash_data TEXT NOT NULL
);

CREATE INDEX user_accounts_fhid_uid ON user_accounts (firehall_id,user_id);

CREATE INDEX callouts_id_callkey ON callouts (id,call_key);
CREATE INDEX callouts_id_status ON callouts (id,status);

CREATE INDEX callouts_response_useracctid ON callouts_response (useracctid);
CREATE INDEX callouts_response_calloutid ON callouts_response (calloutid);
CREATE INDEX callouts_response_status ON callouts_response (status);