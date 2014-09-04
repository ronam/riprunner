--
-- Table structure for table `callouts`
--

CREATE TABLE `callouts` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `calltime` datetime NOT NULL,
  `calltype` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `latitude` DECIMAL(10,6) NOT NULL,  
  `longitude` DECIMAL(10,6) NOT NULL,  
  `units` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `status` int(11) NOT NULL DEFAULT 0,
  `updatetime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `call_key` varchar(64) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- ALTER TABLE callouts ADD COLUMN `call_key` varchar(64) COLLATE utf8_unicode_ci NOT NULL;

CREATE TABLE  `user_accounts` (
`id` INT( 11 ) NOT NULL AUTO_INCREMENT PRIMARY KEY,
`firehall_id` varchar(80) COLLATE utf8_unicode_ci NOT NULL,
`user_id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
`user_pwd` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
`mobile_phone` varchar(25) COLLATE utf8_unicode_ci NOT NULL,
`access` INT( 11 ) NOT NULL DEFAULT 0,
`updatetime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- ALTER TABLE user_accounts ADD COLUMN `mobile_phone` varchar(25) COLLATE utf8_unicode_ci NOT NULL AFTER user_pwd;
-- ALTER TABLE user_accounts ADD COLUMN `access` INT( 11 ) NOT NULL DEFAULT 0 AFTER mobile_phone;

CREATE TABLE `login_attempts` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `useracctid` INT(11) NOT NULL,
    `time` VARCHAR(30) NOT NULL
) ENGINE = INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- ALTER TABLE login_attempts ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

CREATE TABLE `callouts_response` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `calloutid` int(11) NOT NULL,
  `useracctid` int(11) NOT NULL,
  `responsetime` datetime NOT NULL,
  `latitude` DECIMAL(10,6) NOT NULL,  
  `longitude` DECIMAL(10,6) NOT NULL,  
  `status` int(11) NOT NULL DEFAULT 0,
  `updatetime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE  `devicereg` (
`id` INT( 11 ) NOT NULL AUTO_INCREMENT PRIMARY KEY ,
`registration_id` TEXT NOT NULL,
`firehall_id` varchar(80) COLLATE utf8_unicode_ci NOT NULL,
`user_id` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
`updatetime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
