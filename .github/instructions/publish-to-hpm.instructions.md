# **Publishing Instructions for Advanced Heliotrope**

This document describes the exact steps required to publish the **Advanced Heliotrope** package to the official **Hubitat Package Manager (HPM)** repository and how to prepare release versions.

This file is intended for automated agents and contributors.
Do **not** change the project structure when publishing.

---

# **1. Ensure the Package Is Release-Ready**

Before publishing:

* [ ] All items in `hpm-checklist.md` have passed
* [ ] The package installs correctly using manual URL import
* [ ] The package installs correctly using HPM “Install from URL”
* [ ] Version in `packageManifest.json` has been updated
* [ ] No compilation errors exist in any Groovy file
* [ ] All drivers and the app load and create child devices correctly
* [ ] Uninstall removes all components cleanly

---

# **2. Update Version and Tag the Release**

1. Update the `"version"` field in `packageManifest.json`
2. Commit the change
3. Create a version tag:

```
git tag v1.0.0
git push --tags
```

Use semantic versioning:

* `MAJOR.MINOR.PATCH`

---

# **3. Submit to the HPM Public Repository**

To make the package discoverable to all Hubitat users:

1. Go to the official HPM repository:
   [https://github.com/dcmeglio/hubitat-packagerepository](https://github.com/dcmeglio/hubitat-packagerepository)

2. Edit `repositories.json`

3. Add this entry:

```json
{
  "id": "advanced-heliotrope",
  "name": "Advanced Heliotrope",
  "location": "https://raw.githubusercontent.com/Electrified-Home/Hubitat-Advanced-Heliotrope/main/packageManifest.json"
}
```

4. Submit the pull request.

After approval, the package becomes publicly searchable in HPM.

---

# **4. Optional: Create a Release on GitHub**

1. Go to the Releases section
2. Create a new release using the same version tag
3. Add release notes
4. Link to documentation if desired

This is not required for HPM, but improves project presentation.

---

# **5. Publishing Workflow Summary**

Publishing a new version consists of:

1. Update manifest version
2. Commit changes
3. Tag release (`vX.Y.Z`)
4. Push tag
5. (Optional) Create GitHub release
6. (Optional) Update HPM repo via PR

Agents must follow this order.
