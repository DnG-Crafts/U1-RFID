# U1 Tag Protocol

This document contains the logic for reading and writing to RFID tags used by Snapmaker for the U1 printer.<br> 
The U1 utilizes the MIFARE Classic 1K architecture with a custom **HKDF (HMAC-based Key Derivation Function)** for security.

<br>

The following files are where this infomation was obtained.<br>

<a href=https://github.com/DnG-Crafts/U1-RFID/blob/main/docs/filament_parameters.py>filament_parameters.py</a><br>
<a href=https://github.com/DnG-Crafts/U1-RFID/blob/main/docs/filament_protocol.py>filament_protocol.py</a><br>
<a href=https://github.com/DnG-Crafts/U1-RFID/blob/main/docs/fm175xx_reader.py>fm175xx_reader.py</a><br>

These files are all located in the firmware at `\home\lava\klipper\klippy\extras\`

<br><br>

## 1. Security Overview

The tag security is built on three distinct layers:
1.  **Unique Key Derivation**: Every physical tag has 32 unique keys (16x Key A, 16x Key B) generated from its unique hardware ID (UID).
2.  **Access Control**: A specific access bit pattern (`0x87878769`) defines permissions for each sector.
3.  **RSA Signature**: A 256-byte digital signature ensures that the filament data (weight, color, type) has not been tampered with.

<br><br>

---

<br>

## 2. Key Derivation Function (KDF)

The printer does not use default keys (like `FFFFFFFFFFFF`). Instead, it derives keys using the **HMAC-SHA256** algorithm. <br>
This ensures that you cannot simply "copy-paste" data from one tag to another, as the destination tag will have a different UID and therefore require different keys.

### The Extraction Phase (Master Key)
First, a **Pseudo-Random Key (PRK)** is extracted by hashing the tag's UID with a manufacturer secret (Salt).

* **Salt A**: `Snapmaker_qwertyuiop[,.;]`
* **Salt B**: `Snapmaker_qwertyuiop[,.;]_1q2w3e`
* **Formula**:  
    `PRK = HMAC-SHA256(Salt, UID)`

### The Expansion Phase (Sector Keys)
The PRK is then used to generate 16 individual sector keys. For each sector ($i$ from 0 to 15), the following is performed:

1.  **Context String**: A string is built: `key_a_` + `sector` (e.g., `"key_a_0"`, `"key_a_1"`...).
2.  **Hashing**: The PRK, the Context String, and a constant byte `0x01` are hashed together.
3.  **Truncation**: The first 6 bytes of the resulting 32-byte hash become the actual MIFARE key.

**Logic Flow:** `Full_Hash = HMAC-SHA256(PRK, "key_a_" + sector + 0x01)`  
`Sector_Key = Full_Hash[0:6]`

<br><br>

---

<br>

## 3. Memory Map & Data Structure

The tag is organized into 16 sectors. Each sector contains 3 data blocks and 1 trailer block.

| Sector | Usage | Content |
| :--- | :--- | :--- |
| **Sector 0** | Manufacturer | UID, Vendor Name, Manufacturer Name |
| **Sector 1** | Basic Config | Version, Material Type (PLA/PETG), Color (RGB), SKU |
| **Sector 2** | Physical Specs | Diameter, Weight, Length, Temp Settings (Bed/Hotend) |
| **Sector 10-15** | Digital Sign | RSA-2048 Signature (spread across data blocks) |

### The Sector Trailer (Block 3)
Each sector's security is defined in the last block of that sector:
* **Bytes 0-5**: Derived Key A
* **Bytes 6-9**: Access Bits (`87 87 87 69`)
* **Bytes 10-15**: Derived Key B

<br><br>

---

<br>

## 4. The RSA signature Problem

When the printer reads a tag, it checks the RSA signature stored in Sectors 10-15 against the data in Sectors 0-9 this data also includes both derived keys(A/B) and access bits for each sector.

* Snapmaker uses a private key for this signature so we cannot sign the data in a way that the printer can verify the signature using the public keys.
* If you modify the **weight** or **color** without the private RSA key, the signature check fails.
* If you attempt to clone the data to another tag the uid and derived keys will be different and the signature check fails.

<br><br>

---

<br>

## 5. The bypass

To bypass the RSA signature requirement the `verify_signature_pkcs1` function needs to be patched in the firmware to always return *true*.

That function is found in `\home\lava\klipper\klippy\extras\filament_protocol.py`

This is the original function.
```
def verify_signature_pkcs1(public_key, data, signature):
    try:
        public_key = serialization.load_pem_public_key(public_key, backend=default_backend())
        public_key.verify(
            signature,
            data,
            padding.PKCS1v15(),
            hashes.SHA256()
        )
        return True
    except InvalidSignature:
        return False
```

The following change would bypass the RSA signature check allowing the data on the tag to be read without needing to signed.

```
def verify_signature_pkcs1(public_key, data, signature):
    return True
```


I have compiled a version of the extended firmware by paxx12 that has this bypass patch applied for testing which can be obtained <a href=https://github.com/DnG-Crafts/U1-RFID/releases/tag/Test>HERE</a>

<br><br>


---

<br><br>

## 6. Filament Data Structure

```

FILAMENT_INFO_STRUCT = {
    'VERSION': 0,
    'VENDOR': 'NONE', # Brand Owner
    'MANUFACTURER': 'NONE',
    'MAIN_TYPE': 'NONE',
    'SUB_TYPE': 'NONE',
    'TRAY': 0,
    'ALPHA': 0xFF,
    'COLOR_NUMS': 1,
    'ARGB_COLOR': 0xFFFFFFFF, # Old version
    'RGB_1': 0xFFFFFF,
    'RGB_2': 0,
    'RGB_3': 0,
    'RGB_4': 0,
    'RGB_5': 0,
    'DIAMETER': 0,
    'WEIGHT': 0,
    'LENGTH': 0,
    'DRYING_TEMP': 0,
    'DRYING_TIME': 0,
    'HOTEND_MAX_TEMP': 0,
    'HOTEND_MIN_TEMP': 0,
    'BED_TYPE': 0,
    'BED_TEMP': 0,
    'FIRST_LAYER_TEMP': 0,
    'OTHER_LAYER_TEMP': 0,
    'SKU': 0,
    'MF_DATE': '19700101',
    'RSA_KEY_VERSION': 0,
    'OFFICIAL': False,
    'CARD_UID': 0,
}

# Filament main type
FILAMENT_PROTO_MAIN_TYPE_RESERVED               = 0
FILAMENT_PROTO_MAIN_TYPE_PLA                    = 1
FILAMENT_PROTO_MAIN_TYPE_PETG                   = 2
FILAMENT_PROTO_MAIN_TYPE_ABS                    = 3
FILAMENT_PROTO_MAIN_TYPE_TPU                    = 4
FILAMENT_PROTO_MAIN_TYPE_PVA                    = 5

FILAMENT_PROTO_MAIN_TYPE_MAPPING = {
    "PLA":          FILAMENT_PROTO_MAIN_TYPE_PLA,
    "PETG":         FILAMENT_PROTO_MAIN_TYPE_PETG,
    "ABS":          FILAMENT_PROTO_MAIN_TYPE_ABS,
    "TPU":          FILAMENT_PROTO_MAIN_TYPE_TPU,
    "PVA":          FILAMENT_PROTO_MAIN_TYPE_PVA,
    "Reserved":     FILAMENT_PROTO_MAIN_TYPE_RESERVED
}

#Filament sub type
FILAMENT_PROTO_SUB_TYPE_RESERVED                = 0
FILAMENT_PROTO_SUB_TYPE_BASIC                   = 1
FILAMENT_PROTO_SUB_TYPE_MATTE                   = 2
FILAMENT_PROTO_SUB_TYPE_SNAPSPEED               = 3
FILAMENT_PROTO_SUB_TYPE_SILK                    = 4
FILAMENT_PROTO_SUB_TYPE_SUPPORT                 = 5
FILAMENT_PROTO_SUB_TYPE_HF                      = 6
FILAMENT_PROTO_SUB_TYPE_95A                     = 7
FILAMENT_PROTO_SUB_TYPE_95A_HF                  = 8

FILAMENT_PROTO_SUB_TYPE_MAPPING = {
    'Basic':        FILAMENT_PROTO_SUB_TYPE_BASIC,
    'Matte':        FILAMENT_PROTO_SUB_TYPE_MATTE,
    'SnapSpeed':    FILAMENT_PROTO_SUB_TYPE_SNAPSPEED,
    'Silk':         FILAMENT_PROTO_SUB_TYPE_SILK,
    'Support':      FILAMENT_PROTO_SUB_TYPE_SUPPORT,
    'HF':           FILAMENT_PROTO_SUB_TYPE_HF,
    '95A':          FILAMENT_PROTO_SUB_TYPE_95A,
    '95A HF':       FILAMENT_PROTO_SUB_TYPE_95A_HF,
    'Reserved':     FILAMENT_PROTO_SUB_TYPE_RESERVED
}

# Filament color nums
FILAMENT_PROTO_COLOR_NUMS_MAX                   = 5

# Filament Tag type
FILAMENT_PROTO_TAG_M1                           = 'M1_1K'

# M1 card protocol
M1_PROTO_TOTAL_SIZE                             = 1024

## position : section_num * 64 + block_nom * 16 + byte_num
# Section 0
M1_PROTO_UID_POS                                = (0 * 64 + 0 * 16 + 0)
M1_PROTO_UID_LEN                                = (4)
M1_PROTO_VENDOR_POS                             = (0 * 64 + 1 * 16 + 0)
M1_PROTO_VENDOR_LEN                             = (16)
M1_PROTO_MANUFACTURER_POS                       = (0 * 64 + 2 * 16 + 0)
M1_PROTO_MANUFACTURER_LEN                       = (16)
# Section 1
M1_PROTO_VERSION_POS                            = (1 * 64 + 0 * 16 + 0)
M1_PROTO_VERSION_LEN                            = (2)
M1_PROTO_MAIN_TYPE_POS                          = (1 * 64 + 0 * 16 + 2)
M1_PROTO_MAIN_TYPE_LEN                          = (2)
M1_PROTO_SUB_TYPE_POS                           = (1 * 64 + 0 * 16 + 4)
M1_PROTO_SUB_TYPE_LEN                           = (2)
M1_PROTO_TRAY_POS                               = (1 * 64 + 0 * 16 + 6)
M1_PROTO_TRAY_LEN                               = (2)
M1_PROTO_COLOR_NUMS_POS                         = (1 * 64 + 0 * 16 + 8)
M1_PROTO_COLOR_NUMS_LEN                         = (1)
M1_PROTO_ALPHA_POS                              = (1 * 64 + 0 * 16 + 9)
M1_PROTO_ALPHA_LEN                              = (1)
M1_PROTO_RGB_1_POS                              = (1 * 64 + 1 * 16 + 0)
M1_PROTO_RGB_1_LEN                              = (3)
M1_PROTO_RGB_2_POS                              = (1 * 64 + 1 * 16 + 3)
M1_PROTO_RGB_2_LEN                              = (3)
M1_PROTO_RGB_3_POS                              = (1 * 64 + 1 * 16 + 6)
M1_PROTO_RGB_3_LEN                              = (3)
M1_PROTO_RGB_4_POS                              = (1 * 64 + 1 * 16 + 9)
M1_PROTO_RGB_4_LEN                              = (3)
M1_PROTO_RGB_5_POS                              = (1 * 64 + 1 * 16 + 12)
M1_PROTO_RGB_5_LEN                              = (3)
M1_PROTO_SKU_POS                                = (1 * 64 + 2 * 16 + 0)
M1_PROTO_SKU_LEN                                = (4)
# Section 2
M1_PROTO_DIAMETER_POS                           =( 2 * 64 + 0 * 16 + 0)
M1_PROTO_DIAMETER_LEN                           = (2)
M1_PROTO_WEIGHT_POS                             = (2 * 64 + 0 * 16 + 2)
M1_PROTO_WEIGHT_LEN                             = (2)
M1_PROTO_LENGTH_POS                             = (2 * 64 + 0 * 16 + 4)
M1_PROTO_LENGTH_LEN                             = (2)
M1_PROTO_DRY_TEMP_POS                           = (2 * 64 + 1 * 16 + 0)
M1_PROTO_DRY_TEMP_LEN                           = (2)
M1_PROTO_DRY_TIME_POS                           = (2 * 64 + 1 * 16 + 2)
M1_PROTO_DRY_TIME_LEN                           = (2)
M1_PROTO_HOTEND_MAX_TEMP_POS                    = (2 * 64 + 1 * 16 + 4)
M1_PROTO_HOTEND_MAX_TEMP_LEN                    = (2)
M1_PROTO_HOTEND_MIN_TEMP_POS                    = (2 * 64 + 1 * 16 + 6)
M1_PROTO_HOTEND_MIN_TEMP_LEN                    = (2)
M1_PROTO_BED_TYPE_POS                           = (2 * 64 + 1 * 16 + 8)
M1_PROTO_BED_TYPE_LEN                           = (2)
M1_PROTO_BED_TEMP_POS                           = (2 * 64 + 1 * 16 + 10)
M1_PROTO_BED_TEMP_LEN                           = (2)
M1_PROTO_FIRST_LAYER_TEMP_POS                   = (2 * 64 + 1 * 16 + 12)
M1_PROTO_FIRST_LAYER_TEMP_LEN                   = (2)
M1_PROTO_OTHER_LAYER_TEMP_POS                   = (2 * 64 + 1 * 16 + 14)
M1_PROTO_OTHER_LAYER_TEMP_LEN                   = (2)
M1_PROTO_MF_DATE_POS                            = (2 * 64 + 2 * 16 + 0)
M1_PROTO_MF_DATE_LEN                            = (8)
M1_PROTO_RSA_KEY_VER_POS                        = (2 * 64 + 2 * 16 + 8)
M1_PROTO_RSA_KEY_VER_LEN                        = 2

```