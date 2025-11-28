---
applyTo: '**'
---

# **HPM Publishing Checklist**

This checklist ensures the **Advanced Heliotrope** project is compatible with **Hubitat Package Manager (HPM)**.
The development agent must verify every item before preparing a release.

---

## **1. Manifest File**

Path: `packageManifest.json`

* [ ] `packageName` is present and matches project branding
* [ ] `version` is updated and semver-correct (e.g., `1.0.0`)
* [ ] `minimumHEVersion` is set
* [ ] `apps[]` and `drivers[]` arrays exist
* [ ] All entries include:

  * [ ] `id`
  * [ ] `name`
  * [ ] `namespace`
  * [ ] `location` (raw GitHub URL)
  * [ ] `required`
* [ ] No unused fields included
* [ ] JSON is valid and linted (no trailing commas, invalid quotes, etc.)

---

## **2. Raw GitHub URLs**

Every `location` and `importUrl` must be:

* [ ] Full-length (no `...` truncation)
* [ ] HTTPS
* [ ] `raw.githubusercontent.com`
* [ ] Referencing the `main` branch
* [ ] Pointing to actual `.groovy` files
* [ ] Case-sensitive paths correct
* [ ] No 404 when opened in a browser

---

## **3. Namespace Consistency**

* [ ] All drivers/apps use the **same namespace** as defined in the manifest
* [ ] Namespaces contain only valid characters (letters, numbers, hyphens)

---

## **4. App Entries**

* [ ] Parent app appears once under `"apps"`
* [ ] `location` points to `apps/SkyRegionsApp.groovy`
* [ ] The app file contains valid Hubitat `definition` metadata
* [ ] The app file compiles without error

---

## **5. Driver Entries**

* [ ] Sun position driver is included in `"drivers"`
* [ ] Region drivers (circular and rectangular) are included in `"drivers"`
* [ ] Each driver name in the manifest matches the **name field** inside its `metadata` block
* [ ] Each driver compiles in Hubitat
* [ ] No unused drivers referenced

---

## **6. Repository Structure**

* [ ] `apps/` contains only app files
* [ ] `drivers/` contains only driver files
* [ ] No stray or duplicate files
* [ ] No library files referenced by app/driver code
* [ ] No symbolic links or unusual file types

---

## **7. No Hubitat Library Dependencies**

Confirm that:

* [ ] No `library(...)` directives are used
* [ ] No `#include <library>` references exist
* [ ] All code is self-contained

Libraries break HPM installs.

---

## **8. Installation Test (Required)**

Simulate real-world install:

* [ ] Delete any existing installation of Advanced Heliotrope from the hub
* [ ] Install via HPM directly from GitHub
* [ ] Hubitat must:

  * [ ] Install the parent app
  * [ ] Install all drivers
  * [ ] Show no errors in logs
* [ ] Parent app successfully installs child devices
* [ ] Drivers load and run without exceptions

---

## **9. Update Test**

Using HPM:

* [ ] Bump version in manifest
* [ ] Run “Update” in HPM
* [ ] Confirm that:

  * [ ] App updates cleanly
  * [ ] All drivers update
  * [ ] There are no duplicate devices
  * [ ] No errors appear in the logs

---

## **10. Pre-Release Verification**

Before tagging a release:

* [ ] Manifest is correct
* [ ] All URLs tested
* [ ] All drivers/apps compile
* [ ] App creates required child devices
* [ ] Sun updates propagate to regions
* [ ] Region devices update state correctly
* [ ] No debug logs leaking sensitive data
* [ ] Version number incremented

---

# **Completion Requirement**

Only when **every item** in this checklist is confirmed should the agent proceed to:

* Commit manifest changes
* Prepare the release tag
* Notify that HPM is ready
