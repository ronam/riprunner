<?php
// ==============================================================
//	Copyright (C) 2014 Mark Vejvoda
//	Under GNU GPL v3.0
// ==============================================================

ini_set('display_errors', 'On');
error_reporting(E_ALL);
/* This program reads emails from a POP3 mailbox and parses messages that
 * match the expected format. Each callout message is persisted to a database
 * table. 
 * */

define( 'INCLUSION_PERMITTED', true );
require_once( 'config.php' );
require_once( 'functions.php' );
require_once( 'firehall_parsing.php' );
require_once( 'firehall_signal_callout.php' );

// Disable caching to ensure LIVE results.
header( 'Cache-Control: no-store, no-cache, must-revalidate' );
header( 'Cache-Control: post-check=0, pre-check=0', false );
header( 'Pragma: no-cache' );

// Trigger the email polling check
$html = poll_email_callouts($FIREHALLS);

function validate_email_sender($FIREHALL, &$html, &$mail, $n) {
	$valid_email_trigger = true;
	
	if(isset($FIREHALL->EMAIL->EMAIL_FROM_TRIGGER) &&
			 $FIREHALL->EMAIL->EMAIL_FROM_TRIGGER != null &&
			 $FIREHALL->EMAIL->EMAIL_FROM_TRIGGER != '') {
		 
		$valid_email_trigger = false;
		 
		$html .=  "<h3>Looking for email from trigger [" .
				$FIREHALL->EMAIL->EMAIL_FROM_TRIGGER ."]</h3><br />" . PHP_EOL;
		 
		$header = imap_header($mail, $n);
		 
		if(isset($header) && $header != null) {
			if(isset($header->from) && $header->from != null) {
				 
				// Match on exact email address if @ in trigger text
				if(strpos($FIREHALL->EMAIL->EMAIL_FROM_TRIGGER, '@') !== FALSE) {
					$fromaddr = $header->from[0]->mailbox . "@" . $header->from[0]->host;
				}
				// Match on all email addresses from the same domain
				else {
					$fromaddr = $header->from[0]->host;
				}
				 
				if($fromaddr == $FIREHALL->EMAIL->EMAIL_FROM_TRIGGER) {
					$valid_email_trigger = true;
				}
				 
				$html .=  "<h3>Found email from [" .
						$header->from[0]->mailbox . "@" .
						$header->from[0]->host ."] result: " .
						(($valid_email_trigger) ? "true" : "false") . "</h3><br />" . PHP_EOL;
	
			}
			else {
				$html .=  "<h3>Error, Header->from is not set</h3><br />" . PHP_EOL;
			}
		}
		else {
			$html .=  "<h3>Error, Header is not set</h3><br />" . PHP_EOL;
		}
	}
	return $valid_email_trigger;	
}

function process_email_trigger($FIREHALL, &$html, &$mail, $n) {
	# Following are number to names mappings
	$codes = array("7bit","8bit","binary","base64","quoted-printable","other");
	$stt = array("Text","Multipart","Message","Application","Audio","Image","Video","Other");
	
	# Read the email structure and decide if it's multipart or not
	$st = imap_fetchstructure($mail, $n);
	 
	$multi = null;
	if(array_key_exists('parts',$st)) {
		$multi = $st->parts;
	}
	$nparts = count($multi);
	if ($nparts == 0) {
		$html .=  "* SINGLE part email<br>";
	} 
	else {
		$html .=  "* MULTI part email<br>";
	}
		
	# look at the main part of the email, and subparts if they're present
	for ($p = 0; $p <= $nparts; $p++) {
		if($st->type == 1) {
			$text = imap_fetchbody($mail,$n,$p);
		}
		else {
			$text = imap_body($mail,$n);
		}
		 
		if ($p ==  0) {
			$it = $stt[$st->type];
			$is = ucfirst(strtolower($st->subtype));
			$ie = $codes[$st->encoding];
		}
		else {
			$it = $stt[$multi[$p-1]->type];
			$is = ucfirst(strtolower($multi[$p-1]->subtype));
			$ie = $codes[$multi[$p-1]->encoding];
		}
		
		# Report on the mimetype
		$mimetype = "$it/$is";
		$html .=  "<br /><b>Part $p ... ";
		$html .=  "Encoding: $ie for $mimetype</b><br />";
			
		# decode content if it's encoded (more types to add later!)
		if ($ie == "7bit") {
			$realdata = $text;
		}
		elseif ($ie == "8bit") {
			$realdata = imap_8bit($text);
		}
		elseif ($ie == "base64") {
			$realdata = imap_base64($text);
		}
		elseif ($ie == "quoted-printable") {
			$realdata = imap_qprint($text);
			//$realdata = quoted_printable_decode($text);
		}
		 
		list($isCallOutEmail, 
			 $callDateTimeNative, 
			 $callCode,
			 $callAddress, 
			 $callGPSLat, 
			 $callGPSLong,
			 $callUnitsResponding, 
			 $callType) = processFireHallText($realdata);
	
		if($isCallOutEmail == true) {
	
			signalFireHallCallout($FIREHALL, $callDateTimeNative,
				$callCode, $callAddress, $callGPSLat,
				$callGPSLong, $callUnitsResponding, $callType);
	
			# Delete processed email message
			if($FIREHALL->EMAIL->EMAIL_DELETE_PROCESSED) {
				echo 'Delete email message#: ' . $n . PHP_EOL;
				imap_delete($mail, $n);
			}
		}
	
		# If it's a .jpg image, save it (more types to add later)
		// 			                if ($mimetype == "Image/Jpeg") {
 		// 			                        $picture++;
		// 			                        $fho = fopen("imx/mp$picture.jpg","w");
		// 			                        fputs($fho,$realdata);
		// 			                        fclose($fho);
		// 			                        # And put the image in the report, limited in size
		// 			                        $html .= "<img src=/demo/imx/mp$picture.jpg width=150><br />";
		// 			                }
		
		# Add the start of the text to the message
		$shorttext = substr($text,0,800);
		if (strlen($text) > 800) { 
			$shorttext .= " ...\n";
		}
		$html .=  nl2br(htmlspecialchars($shorttext))."<br>";
	}
}

function poll_email_callouts($FIREHALLS_LIST) {

	$html = "";
	
	echo 'Loop count: ' . sizeof($FIREHALLS_LIST) .PHP_EOL;
	
	# Loop through all Firehall email triggers
	foreach ($FIREHALLS_LIST as &$FIREHALL) {
		if($FIREHALL->ENABLED == false || 
			$FIREHALL->EMAIL->EMAIL_HOST_ENABLED == false) {
			continue;
		}
		$pictures = 0;
				
		$html .= '<h2>Checking for: ' . $FIREHALL->WEBSITE->FIREHALL_NAME . '</h2><br />';
		# Connect to the mail server and grab headers from the mailbox
		$mail = imap_open($FIREHALL->EMAIL->EMAIL_HOST_CONNECTION_STRING, 
						  $FIREHALL->EMAIL->EMAIL_HOST_USERNAME, 
						  $FIREHALL->EMAIL->EMAIL_HOST_PASSWORD);
		
		//if (imap_num_msg($mail) == 0)
		//	$errors = imap_errors();
				
		$headers = imap_headers($mail);
		
		# loop through each email header
		for ($n=1; $n<=count($headers); $n++) {
			$html .=  "<h3>".$headers[$n-1]."</h3><br />" . PHP_EOL;
		
		    $valid_email_trigger = validate_email_sender($FIREHALL, $html, $mail, $n);
		    if($valid_email_trigger == true) {
		    	process_email_trigger($FIREHALL, $html, $mail, $n);
		    }
		}
		imap_expunge($mail);
		imap_close($mail);
	}
	
	return $html;
}

# report results ...
?>

<html>
<head>
<title>Reading Mailboxes in search for callout triggers</title>
</head>
<body>
<h1>Mailbox Summary ...</h1>
<?= $html ?>
</body>
</html>