# U1 RFID
Snapmaker U1 RFID Programming


The tags required are <a href=https://www.nxp.com/products/NTAG213_215_216>NTAG215</a> and <a href=https://www.nxp.com/products/NTAG213_215_216>NTAG216</a><br><br>
NTAG213 will not work due to the storage size limitation.<br>


This app uses the <a href=https://github.com/spuder/OpenSpool>OpenSpool</a> format which was added in the <a href=https://github.com/paxx12/SnapmakerU1-Extended-Firmware>SnapmakerU1-Extended-Firmware</a> you must install this firmware to write your own RFID tags.<br>
The extended firmware adds many features and fixes which you can read about <a href=https://snapmakeru1-extended-firmware.pages.dev/>HERE</a>


<br><br><br>
The android app is available on google play<br>
<a href="https://play.google.com/store/apps/details?id=dngsoftware.u1fid&hl=en"><img src=https://github.com/DnG-Crafts/U1-RFID/blob/main/docs/gp.webp width="30%" height="30%"></a>
<br>

<br>

## Tag Format

The tag format is a simple NDEF `application/json` record which is in the <a href=https://github.com/spuder/OpenSpool>OpenSpool</a> format.

### Example JSON Payload

```
{
"id":"0",
"protocol":"openspool",
"version":"1.0",
"brand":"Snapmaker",
"type":"PLA",
"subtype":"Matte",
"alpha":"FF",
"color_hex":"0000FF",
"min_temp":190,
"max_temp":220,
"bed_min_temp":50,
"bed_max_temp":60,
"diameter":175,
"weight":1000
}

```

## Official Snapmaker Tags

The following document explains the format of the official snapmaker tags.<br>
<a href=https://github.com/DnG-Crafts/U1-RFID/blob/main/docs/U1%20Tag%20Protocol.md>U1 Tag Protocol</a><br>

Currently we can read and write to the MIFARE Classic 1K tags but we cant validate the data with a RSA signature due to the private key snapmaker uses.<br>
We can bypass this signature check which requires firmware modification.