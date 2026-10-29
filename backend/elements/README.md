# Symplectic Elements XML Crosswalks
This directory contains the XML crosswalks used to convert between Symplectic Elements XML and DSpace XML. 

The **staging-deposit-xwalkout.xml and staging-harvest-xwalkin.xml** files are sample XML documents that can be used to test the crosswalks. 

The **staging-validate.sh** script can be used to validate the sample XML documents against the crosswalks using the **xmllint** command-line tool. 

The crosswalks are defined in the **XwalkOut.xsd and XwalkIn.xsd** files, which specify the structure and data types of the XML documents. 

Prerequisites for using **staging-validate.sh** 
* Include having **xmllint** installed and available in your system's PATH.
* The script assumes that the sample XML documents and crosswalks are located in the same directory as the script. Download the sample crosswalks from the following Google Drive Folder: [Symplectic Elements Michigan Experts](https://drive.google.com/drive/folders/155Ji42mKJab__ds4JaW5UlT6kIO0Sv58?usp=share_link)

Run schema validation from the repository root:

```bash
(cd backend/elements && ./staging-validate.sh)
```

The script stops with a nonzero exit status if either XML file fails validation.
XSD validation checks structure and schema constraints, not the live Elements data
model or the behavior of the RT2 crosswalk engine.

## Mapping behavior and deployment checks

* Harvest uses the automatic creation and public-availability date mappings:
  `dc.date.accessioned` and `dc.date.available`, respectively.
* Deposit accepts both spaced (`CC BY`) and hyphenated (`CC-BY`) Creative Commons
  identifiers. Harvest continues to emit hyphenated identifiers. Confirm these
  identifiers against the live Elements licence choices before deployment.
* Harvest preserves all publication types emitted by the deposit crosswalk,
  including `preprint`. Existing subtype-specific selection rules retain
  precedence when an item has multiple types.
* Sponsorship entries formatted as `Grant ID: Funder` or `Grant ID` are harvested
  as grants. Other entries are preserved as acknowledgement text, with multiple
  text entries joined by newlines. Since `dc.description.sponsorship` also holds
  commissioning-body data, this convention cannot distinguish all original
  Elements funding and commissioning fields.
* `oj` has one harvest mapping, to the existing local label `Anishinaabe`; deposit
  accepts that label as well as `Ojibwe` and `Ojibwa`. `yi` maps to `Yiddish`.
  The incorrect country-name mappings for `GE` and `PN` have been removed;
  unmatched values still pass through for the existing Elements validation.
* Author ORCID reconstruction is deferred pending a supported RT2 mapping
  example. Authors still come from `dc.contributor.author`, and ORCIDs from
  `dc.identifier.orcid` are harvested as record-level associated identifiers.
  `dc.identifier.name-orcid` does not currently restore author/ORCID associations.

Before deploying, exercise representative deposits and harvests in Elements,
including embargoed items, licence choices, each publication type, and funding
text mixed with grants. The collection selector in the deposit XML remains
staging-specific and must not be used unchanged in production.
