# Detailed info on how Paranoid Diary stores the data

## SQLite tables

Paranoid Diary uses SQLite database to store all sensitive data - diary records, tags, etc. This data is destroyed when the app is uninstalled.

### PwdCheck

This table has only one record and is used to check if the password entered by the user is correct, before attempting to decrypt the real data. It has zero records if the password was never set, or exactly one record otherwise.

The value is an encrypted random bytes array of a fixed length and MD5 hash of these random bytes.

To check if the password is correct, we decrypt the value using the password currently entered by the user, and extract the the random bytes and hash from it. If the password currently entered is correct, then the extracted hash should be the same as the one we calculate now.


CREATE TABLE PwdCheck (  
Key INTEGER NOT NULL, // Always 1  
Value BLOB NOT NULL,  
PRIMARY KEY (Key)  
)

### Records

CREATE TABLE Records (  
_ID INTEGER PRIMARY KEY,  
DiaryId INTEGER,  
TimeCreated INTEGER,  
TimeUpdated INTEGER,  
EncryptedText BLOB,  
Lat REAL,  
Lon REAL  
)

### RecordTags

CREATE TABLE RecordTags (  
RecordId INTEGER NOT NULL,  
TagId INTEGER NOT NULL,  
PRIMARY KEY (RecordId, TagId)  
)

### Tags

CREATE TABLE Tags (  
_ID INTEGER PRIMARY KEY,  
EncryptedName BLOB  
)

## Preferences

Paranoid Diary uses shared preferences to store non-sensitive data related to the 
visualization and app behaviour. This data is destroyed when the app is uninstalled.

* **pref\_theme** String e.g. MODE\_NIGHT\_YES
* **pref\_key\_geotagging\_enabled** boolean
* **pref\_key\_last\_backup\_time** long Timestamp when the last backup was shared. Note that we can't know if the action was completed.