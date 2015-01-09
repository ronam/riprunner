<?php
// ==============================================================
//	Copyright (C) 2014 Mark Vejvoda
//	Under GNU GPL v3.0
// ==============================================================
if ( !defined('INCLUSION_PERMITTED') ||
( defined('INCLUSION_PERMITTED') && INCLUSION_PERMITTED !== true ) ) {
	die( 'This file must not be invoked directly.' );
}

require_once( 'config.php' );
require_once( 'functions.php' );
require_once( 'plugins_loader.php' );
require_once( 'firehall_signal_gcm.php' );

function signalFireHallCallout($FIREHALL, $callDateTimeNative, $callCode, 
	                		$callAddress, $callGPSLat, $callGPSLong, 
	                		$callUnitsResponding, $callType) {
	
	// Connect to the database
	$db_connection = db_connect_firehall($FIREHALL);
	
	// update database info about this callout
	$callOutDateTimeString = '';
	if($callDateTimeNative != null && $callDateTimeNative != '') {
		$callOutDateTimeString = $callDateTimeNative->format('Y-m-d H:i:s');
	}
	
	// See if this is a duplicate callout?
	$sql = "SELECT id,call_key,status " .
			"FROM callouts WHERE calltime = '" . $db_connection->real_escape_string( $callOutDateTimeString ) . "'" .
			" AND calltype = '" . $db_connection->real_escape_string( $callCode ) . "'" .
			" AND (address = '" . $db_connection->real_escape_string( $callAddress ) . "'" .
			" OR (latitude = " . $db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLat)) ) . 
	        "     AND longitude = " . $db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLong)) ). "));";
	$sql_result = $db_connection->query( $sql );
	if($sql_result == false) {
		printf("Error: %s\n", mysqli_error($db_connection));
		throw new Exception(mysqli_error( $db_connection ) . "[ " . $sql . "]");
	}
	
	$callout_id = null;
	$callout_status = null;
	$callKey = null;
	
	if($row = $sql_result->fetch_object()) {
		$callout_id = $row->id;
		$callKey = $row->call_key;
		$callout_status = $row->status;
	}
	$sql_result->close();
	
	// Found duplicate callout so update some fields on original callout
	if(isset($callout_id)) {
		// Insert the new callout
		$sql = 'UPDATE callouts' .
				' SET address = \'' . $db_connection->real_escape_string( $callAddress )           . '\', ' .
				' latitude = ' . $db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLat)) )  . ', ' .
				' longitude = ' . $db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLong)) ) . ', ' .
				' units = \'' . $db_connection->real_escape_string( $callUnitsResponding ) . '\'' .
				' WHERE id = ' . $callout_id . 
				' AND (address <> \'' . $db_connection->real_escape_string( $callAddress )           . '\'' . 
				' OR latitude <> ' . $db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLat)) )  . 		
				' OR longitude <> ' . $db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLong)) ) .
				' OR units <> \'' . $db_connection->real_escape_string( $callUnitsResponding ) . '\');';
		
		$sql_result = $db_connection->query( $sql );
		
		if($sql_result == false) {
			printf("Error: %s\n", mysqli_error($db_connection));
			throw new Exception(mysqli_error( $db_connection ) . "[ " . $sql . "]");
		}
		
		$affected_rows = $db_connection->affected_rows;
		
		if($affected_rows > 0) {
			$update_callout_prefix_msg = "*UPDATED* ";
			
			signalCalloutToSMSPlugin($FIREHALL, $callDateTimeNative, $callCode,
				$callAddress, $callGPSLat, $callGPSLong,
				$callUnitsResponding, $callType, $callout_id, $callKey,
				$update_callout_prefix_msg);
			
			$gcmMsg = getSMSCalloutMessage($FIREHALL,$callDateTimeNative,
					$callCode, $callAddress, $callGPSLat, $callGPSLong,
					$callUnitsResponding, $callType, $callout_id, $callKey,0);
			
			signalCallOutRecipientsUsingGCM($FIREHALL,$callDateTimeNative,
				$callCode, $callAddress, $callGPSLat, $callGPSLong,
				$callUnitsResponding, $callType, $callout_id, $callKey,
				$callout_status,null,
				$update_callout_prefix_msg . $gcmMsg,$db_connection);
		}
	}
	else {
		// Insert the new callout
		$callKey = uniqid('', true);
		$sql = 'INSERT INTO callouts (calltime,calltype,address,latitude,longitude,units,call_key) ' .
				' values(' .
				'\'' . $db_connection->real_escape_string( $callOutDateTimeString ) . '\', ' .
				'\'' . $db_connection->real_escape_string( $callCode )              . '\', ' .
				'\'' . $db_connection->real_escape_string( $callAddress )           . '\', ' .
				$db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLat)) )  . ', ' .
				$db_connection->real_escape_string( floatval(preg_replace("/[^-0-9\.]/","",$callGPSLong)) ) . ', ' .
				'\'' . $db_connection->real_escape_string( $callUnitsResponding ) . '\', ' .   
				'\'' . $db_connection->real_escape_string($callKey) . '\');';
		
		$sql_result = $db_connection->query( $sql );
		
		if($sql_result == false) {
			printf("Error: %s\n", mysqli_error($db_connection));
			throw new Exception(mysqli_error( $db_connection ) . "[ " . $sql . "]");
		}
		
		$callout_id = $db_connection->insert_id;
		
		signalCalloutToSMSPlugin($FIREHALL, $callDateTimeNative, $callCode,
				$callAddress, $callGPSLat, $callGPSLong,
				$callUnitsResponding, $callType, $callout_id, $callKey,
				null);
	
		$gcmMsg = getSMSCalloutMessage($FIREHALL,$callDateTimeNative,
				$callCode, $callAddress, $callGPSLat, $callGPSLong,
				$callUnitsResponding, $callType, $callout_id, $callKey,0);
		
		signalCallOutRecipientsUsingGCM($FIREHALL,$callDateTimeNative,
			$callCode, $callAddress, $callGPSLat, $callGPSLong,
				$callUnitsResponding, $callType, $callout_id, $callKey,
				CalloutStatusType::Paged,null,$gcmMsg,$db_connection);

		// Only update status if not cancelled or completed already
		$sql_update = 'UPDATE callouts SET status=' . CalloutStatusType::Notified .' WHERE id = ' .
				$db_connection->real_escape_string( $callout_id ) . ' AND status NOT in(3,10);';
			
		$sql_result = $db_connection->query( $sql_update );
			
		if($sql_result == false) {
			printf("SQL #2 Error: %s\n", mysqli_error($db_connection));
			throw new Exception(mysqli_error( $db_connection ) . "[ " . $sql_update . "]");
		}
	}
	
	if($db_connection != null) {
		db_disconnect( $db_connection );
	}
}

function signalCalloutToSMSPlugin($FIREHALL, $callDateTimeNative, $callCode, 
	                		$callAddress, $callGPSLat, $callGPSLong, 
	                		$callUnitsResponding, $callType, $callout_id,
							$callKey, $msgPrefix) {
	
	if($FIREHALL->SMS->SMS_SIGNAL_ENABLED) {	
		$smsPlugin = findPlugin('ISMSPlugin', $FIREHALL->SMS->SMS_GATEWAY_TYPE);
		if($smsPlugin == null) {
			throw new Exception("Invalid SMS Plugin type: [" . $FIREHALL->SMS->SMS_GATEWAY_TYPE . "]");
		}
		
		if($FIREHALL->LDAP->ENABLED) {
			$recipients = get_sms_recipients_ldap($FIREHALL);
			$recipient_list = explode(';',$recipients);
			$recipient_list_array = $recipient_list;
		}
		else {
			$recipient_list_type = ($FIREHALL->SMS->SMS_RECIPIENTS_ARE_GROUP ?
					RecipientListType::GroupList : RecipientListType::MobileList);
			if($recipient_list_type == RecipientListType::GroupList) {
				$recipients_group = $FIREHALL->SMS->SMS_RECIPIENTS;
				$recipient_list_array = explode(';',$recipients_group);
			}
			else if($FIREHALL->SMS->SMS_RECIPIENTS_FROM_DB) {
				$recipient_list = getMobilePhoneListFromDB($FIREHALL,null);
				$recipient_list_array = $recipient_list;
			}
			else {
				$recipients = $FIREHALL->SMS->SMS_RECIPIENTS;
				$recipient_list = explode(';',$recipients);
				$recipient_list_array = $recipient_list;
			}
		}
		
		$smsText = getSMSCalloutMessage($FIREHALL,$callDateTimeNative,
				$callCode, $callAddress, $callGPSLat, $callGPSLong,
				$callUnitsResponding, $callType, $callout_id, $callKey,
				$smsPlugin->getMaxSMSTextLength());
		if(isset($msgPrefix)) {
			$smsText = $msgPrefix . $smsText;
		}
		$resultSMS = $smsPlugin->signalRecipients($FIREHALL->SMS, $recipient_list_array,
				$recipient_list_type, $smsText);
		if(isset($resultSMS)) {
			echo $resultSMS;
		}
	}
}

function sendSMSPlugin_Message($FIREHALL, $msg) {
	$resultSMS = "";
	
	if($FIREHALL->SMS->SMS_SIGNAL_ENABLED) {
		$smsPlugin = findPlugin('ISMSPlugin', $FIREHALL->SMS->SMS_GATEWAY_TYPE);
		if($smsPlugin == null) {
			throw new Exception("Invalid SMS Plugin type: [" . $FIREHALL->SMS->SMS_GATEWAY_TYPE . "]");
		}

		if($FIREHALL->LDAP->ENABLED) {
			$recipients = get_sms_recipients_ldap($FIREHALL);
			$recipient_list = explode(';',$recipients);
			$recipient_list_array = $recipient_list;
		}
		else {
			$recipient_list_type = ($FIREHALL->SMS->SMS_RECIPIENTS_ARE_GROUP ?
					RecipientListType::GroupList : RecipientListType::MobileList);
			if($recipient_list_type == RecipientListType::GroupList) {
				$recipients_group = $FIREHALL->SMS->SMS_RECIPIENTS;
				$recipient_list_array = explode(';',$recipients_group);
			}
			else if($FIREHALL->SMS->SMS_RECIPIENTS_FROM_DB) {
				$recipient_list = getMobilePhoneListFromDB($FIREHALL,null);
				$recipient_list_array = $recipient_list;
			}
			else {
				$recipients = $FIREHALL->SMS->SMS_RECIPIENTS;
				$recipient_list = explode(';',$recipients);
				$recipient_list_array = $recipient_list;
			}
		}
		
		$resultSMS = $smsPlugin->signalRecipients($FIREHALL->SMS, $recipient_list_array,
				$recipient_list_type, $msg);
	}
	
	return $resultSMS;
}

function getSMSCalloutMessage($FIREHALL,$callDateTimeNative,
			$callCode, $callAddress, $callGPSLat, $callGPSLong,
			$callUnitsResponding, $callType, $callout_id, $callKey,
			$maxLength) {
	
	$msgSummary = '911-Page: ' . $callCode . ', ' . $callType . ', ' . $callAddress;
	
 	$details_link = $FIREHALL->WEBSITE->WEBSITE_CALLOUT_DETAIL_URL 
 		. 'ci.php?cid=' . $callout_id
 		. '&fhid=' . $FIREHALL->FIREHALL_ID
 		. '&ckid=' . $callKey;
	
	$smsMsg = $msgSummary .', ' . $details_link;
	if(isset($maxLength) && $maxLength > 0) {
		$smsMsg = array($msgSummary, 
						$details_link);
	}
	return $smsMsg;
}

?>