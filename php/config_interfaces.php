<?php
// ==============================================================
//	Copyright (C) 2014 Mark Vejvoda
//	Under GNU GPL v3.0
// ==============================================================
	
	// ----------------------------------------------------------------------
	class FireHallEmailAccount
	{
		// Indicates whether the email host should be checked for email triggers
		public $EMAIL_HOST_ENABLED;
		// The From email address that is allowed to trigger a callout.
		// Two formats are allowed:
		// 1. Full email address
		//    donotreply@focc.mycity.ca
		// 2. Domain name (all emails from domain)
		//    focc.mycity.ca
		public $EMAIL_FROM_TRIGGER;
		// Email provider connection string to check for email triggers
		public $EMAIL_HOST_CONNECTION_STRING;
		// Email address that will receive callout information
		public $EMAIL_HOST_USERNAME;
		// Email address password that will receive callout information
		public $EMAIL_HOST_PASSWORD;
		// Email should be deleted after it is received and processed.
		public $EMAIL_DELETE_PROCESSED;
	
		public function __construct($host_enabled, $from_trigger, $host_conn_str, $host_username, $host_password, $host_delete_processed) {
			$this->EMAIL_HOST_ENABLED = $host_enabled;
			$this->EMAIL_FROM_TRIGGER = $from_trigger;
			$this->EMAIL_HOST_CONNECTION_STRING = $host_conn_str;
			$this->EMAIL_HOST_USERNAME = $host_username;
			$this->EMAIL_HOST_PASSWORD = $host_password;
			$this->EMAIL_DELETE_PROCESSED = $host_delete_processed;
		}
	}
				
	// ----------------------------------------------------------------------
	class FireHallMySQL
	{
		// The name of the MySQL database Host
		public $MYSQL_HOST;
		// The name of the MySQL database
		public $MYSQL_DATABASE;
		// The username to authenticate to the MySQL database
		public $MYSQL_USER;
		// The user password to authenticate to the MySQL database
		public $MYSQL_PASSWORD;
	
		public function __construct($host, $database, $username, $password) {
			$this->MYSQL_HOST = $host;
			$this->MYSQL_DATABASE = $database;
			$this->MYSQL_USER = $username;
			$this->MYSQL_PASSWORD = $password;
		}
	}

	// ----------------------------------------------------------------------
	class FireHallSMS
	{
		// Indicates whether we should signal responders using SMS during a callout
		public $SMS_SIGNAL_ENABLED;
		// The type of SMS Gateway. Current supported types:
		// TEXTBELT, SENDHUB, EZTEXTING, TWILIO
		// To Support additional SMS Providers contact the author or implement
		// an SMS plugin class in the plugins/sms folder.
		public $SMS_GATEWAY_TYPE;
		// The recipients to send an SMS during communications such as a callout
		// This can be a ; delimited list of mobile phone #'s (set are_group and from_db to false)
		// or it can by a specific Group Name defined by the particular SMS provider (set are_group to true)
		// or you can tell the software to read the mobile phone #'s from the database (set from_db to true)
		public $SMS_RECIPIENTS;
		// If the recipient list is an SMS group name set this value to true
		public $SMS_RECIPIENTS_ARE_GROUP;
		// If the recipient list should be dynamically built from the database set this value to true
		public $SMS_RECIPIENTS_FROM_DB;
		// The Base API URL for sending SMS messages using sendhub.com
		public $SMS_PROVIDER_SENDHUB_BASE_URL;
		// The Base API URL for sending SMS messages using textbelt.com
		public $SMS_PROVIDER_TEXTBELT_BASE_URL;
		// The Base API URL for sending SMS messages using eztexting.com
		public $SMS_PROVIDER_EZTEXTING_BASE_URL;
		// The API username to use for eztexting
		public $SMS_PROVIDER_EZTEXTING_USERNAME;
		// The API user password to use for eztexting
		public $SMS_PROVIDER_EZTEXTING_PASSWORD;
		// The Base API URL for sending SMS messages using twilio.com
		public $SMS_PROVIDER_TWILIO_BASE_URL;
		// The API authentication token to use for twilio
		public $SMS_PROVIDER_TWILIO_AUTH_TOKEN;
		// The API FROM mobile phone # to use for twilio
		public $SMS_PROVIDER_TWILIO_FROM;
		
		public function __construct($sms_enabled, $gateway_type, $recipients,
				$recipients_are_group, $recipients_from_db,
				$sendhub_base_url, $textbelt_base_url, $eztexting_base_url,
				$eztexting_username, $eztexting_password, $twilio_base_url,
				$twilio_auth_token, $twilio_from) {
			
			$this->SMS_SIGNAL_ENABLED = $sms_enabled;
			$this->SMS_GATEWAY_TYPE = $gateway_type;
			$this->SMS_RECIPIENTS = $recipients;
			$this->SMS_RECIPIENTS_ARE_GROUP = $recipients_are_group;
			$this->SMS_RECIPIENTS_FROM_DB = $recipients_from_db;
			$this->SMS_PROVIDER_SENDHUB_BASE_URL = $sendhub_base_url;
			$this->SMS_PROVIDER_TEXTBELT_BASE_URL = $textbelt_base_url;
			$this->SMS_PROVIDER_EZTEXTING_BASE_URL = $eztexting_base_url;
			$this->SMS_PROVIDER_EZTEXTING_USERNAME = $eztexting_username;
			$this->SMS_PROVIDER_EZTEXTING_PASSWORD = $eztexting_password;
			$this->SMS_PROVIDER_TWILIO_BASE_URL = $twilio_base_url;
			$this->SMS_PROVIDER_TWILIO_AUTH_TOKEN = $twilio_auth_token;
			$this->SMS_PROVIDER_TWILIO_FROM = $twilio_from;
		}
	}
	
	// ----------------------------------------------------------------------
	class FireHallMobile
	{
		// Indicates whether we should allow use of the Native Mobile Android App
		public $MOBILE_SIGNAL_ENABLED;
		// Indicates whether we should allow use of mobile tracking
		public $MOBILE_TRACKING_ENABLED;
		// Indicates whether we should signal Native Mobile Android App responders during a callout
		public $GCM_SIGNAL_ENABLED;
		// The base URL to call the Google Cloud Messaging Service
		public $GCM_SEND_URL;
		// The API Key for the Google Cloud Messaging Service
		public $GCM_API_KEY;
		// The Project Id (aka sender id) for the Google Cloud Messaging Service
		public $GCM_PROJECTID;
	
		public function __construct($mobile_enabled, $mobile_tracking_enabled, $gcm_enabled, $gcm_send_url, $gcm_api_key, $gcm_projectid) {
			$this->MOBILE_SIGNAL_ENABLED = $mobile_enabled;
			$this->MOBILE_TRACKING_ENABLED = $mobile_tracking_enabled;
			$this->GCM_SIGNAL_ENABLED = $gcm_enabled;
			$this->GCM_SEND_URL = $gcm_send_url;
			$this->GCM_API_KEY = $gcm_api_key;
			$this->GCM_PROJECTID = $gcm_projectid;
		}
	}
	
	// ----------------------------------------------------------------------
	class FireHallWebsite
	{
		// The display name for the Firehall
		public $FIREHALL_NAME;
		// The address of the Firehall
		public $FIREHALL_HOME_ADDRESS;
		// The GEO coordinates of the Firehall
		public $FIREHALL_GEO_COORD_LATITUDE;
		public $FIREHALL_GEO_COORD_LONGITUDE;
		// The Base URL where you installed rip runner example: http://mywebsite.com/riprunner/
		public $WEBSITE_CALLOUT_DETAIL_URL;
		// The Google Map API Key
		public $WEBSITE_GOOGLE_MAP_API_KEY;
		// An array of source = destination city names of original_city_name = new_city_name city names to swap for google maps
		// example: "SALMON VALLEY," => "PRINCE GEORGE,",
		public $WEBSITE_CALLOUT_DETAIL_CITY_NAME_SUBSTITUTION;
			
		public function __construct($name,$home_address,$home_geo_coord_lat,$home_geo_coord_long,$callout_detail_url, $google_map_api_key, $city_name_substition) {
			$this->FIREHALL_NAME = $name;
			$this->FIREHALL_HOME_ADDRESS = $home_address;
			$this->FIREHALL_GEO_COORD_LATITUDE = $home_geo_coord_lat;
			$this->FIREHALL_GEO_COORD_LONGITUDE = $home_geo_coord_long;
			$this->WEBSITE_CALLOUT_DETAIL_URL = $callout_detail_url;
			$this->WEBSITE_GOOGLE_MAP_API_KEY = $google_map_api_key;
			$this->WEBSITE_CALLOUT_DETAIL_CITY_NAME_SUBSTITUTION = $city_name_substition;
		}
	}

	// ----------------------------------------------------------------------
	class FireHall_LDAP
	{
		// Indicates whether LDAP should be used for the firehall
		public $ENABLED;
		// The ldap connect url
		public $LDAP_SERVERNAME;
		// The ldap bind root dn (or null if anonymous binds allowed) 
		public $LDAP_BIND_RDN;
		// The ldap bind password (or null if anonymous binds allowed)
		public $LDAP_BIND_PASSWORD;
		// The ldap base bind dn
		public $LDAP_BASEDN;
		// The ldap bind user accounts dn
		public $LDAP_BASE_USERDN;
		// The ldap login filter expression
		public $LDAP_LOGIN_FILTER;
		// The ldap login filter expression
		public $LDAP_USER_DN_ATTR_NAME;
		// The ldap sortby filter expression
		public $LDAP_USER_SORT_ATTR_NAME;
		// The ldap administrator group filter expression
		public $LDAP_LOGIN_ADMIN_GROUP_FILTER;
		// The ldap sms group filter expression
		public $LDAP_LOGIN_SMS_GROUP_FILTER;
		// The ldap group memberof attribute name
		public $LDAP_GROUP_MEMBER_OF_ATTR_NAME;
		// The ldap sms mobile attribute name
		public $LDAP_USER_SMS_ATTR_NAME;
		// The ldap user id attribute name
		public $LDAP_USER_ID_ATTR_NAME;
		// The ldap user name attribute name
		public $LDAP_USER_NAME_ATTR_NAME;
				
		public function __construct($enabled,$name,$bind_rdn,$bind_password,$dn,
									$user_dn,$login_filter, 
									$user_dn_attr, 
				                    $user_sort_attr, $user_admin_group_filter_attr,
									$user_sms_group_filter_attr, $group_member_of_attr,
									$user_sms_attr, $user_id_attr, $user_name_attr) {
			$this->ENABLED = $enabled;
			$this->LDAP_SERVERNAME = $name;
			$this->LDAP_BIND_RDN = $bind_rdn;
			$this->LDAP_BIND_PASSWORD = $bind_password;
			$this->LDAP_BASEDN = $dn;
			$this->LDAP_BASE_USERDN = $user_dn;
			$this->LDAP_LOGIN_FILTER = $login_filter;
			$this->LDAP_USER_DN_ATTR_NAME = $user_dn_attr;
			$this->LDAP_USER_SORT_ATTR_NAME = $user_sort_attr;
			$this->LDAP_LOGIN_ADMIN_GROUP_FILTER = $user_admin_group_filter_attr;
			$this->LDAP_LOGIN_SMS_GROUP_FILTER = $user_sms_group_filter_attr;
			$this->LDAP_GROUP_MEMBER_OF_ATTR_NAME = $group_member_of_attr;
			$this->LDAP_USER_SMS_ATTR_NAME = $user_sms_attr;
			$this->LDAP_USER_ID_ATTR_NAME = $user_id_attr;
			$this->LDAP_USER_NAME_ATTR_NAME = $user_name_attr;
		}
	}
	
	// ----------------------------------------------------------------------
	class FireHallConfig
	{
		// Indicates whether the firehall is enabled or not
		public $ENABLED;
		// A unique ID to differentiate multipel firehalls
		public $FIREHALL_ID;
		// The Mysql configuration for the Firehall
		public $MYSQL;
		// The Email configuration for the Firehall
		public $EMAIL;
		// The SMS configuration for the Firehall
		public $SMS;
		// The Website configuration for the Firehall
		public $WEBSITE;
		// The Mobile configuration for the Firehall
		public $MOBILE;
		// The LDAP configuration for the firehall
		public $LDAP;
			
		public function __construct($enabled, $id,$mysql, $email, $sms, $website, $mobile, $ldapcfg) {
			$this->ENABLED = $enabled;
			$this->FIREHALL_ID = $id;
			$this->MYSQL = $mysql;
			$this->EMAIL = $email;
			$this->SMS = $sms;
			$this->WEBSITE = $website;
			$this->MOBILE = $mobile;
			$this->LDAP = $ldapcfg;
		}
	}
	
?>
