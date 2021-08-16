# Detailed info on how Paranoid Diary stores the data

## SQLite

Paranoid Diary uses SQLite database to store all sensitive data - diary records, tags, etc. This data is destroyed when the app is uninstalled.

## Preferences

Paranoid Diary uses shared preferences to store non-sensitive data related to the 
visualization and app behaviour. This data is destroyed when the app is uninstalled.

* **pref\_theme** String e.g. MODE\_NIGHT\_YES
* **pref\_key\_geotagging\_enabled** boolean
* **pref\_key\_last\_backup\_time** long Timestamp when the last backup was shared. Note that we can't know if the action was completed.