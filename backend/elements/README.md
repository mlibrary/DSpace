# Symplectic Elements XML Crosswalks
This directory contains the XML crosswalks used to convert between Symplectic Elements XML and DSpace XML. 

The **staging-deposit-xwalkout.xml and staging-harvest-xwalkin.xml** files are sample XML documents that can be used to test the crosswalks. 

The **staging-validate.sh** script can be used to validate the sample XML documents against the crosswalks using the **xmllint** command-line tool. 

The crosswalks are defined in the **XwalkOut.xsd and XwalkIn.xsd** files, which specify the structure and data types of the XML documents. 

Prerequisites for using **staging-validate.sh** 
* Include having **xmllint** installed and available in your system's PATH.
* The script assumes that the sample XML documents and crosswalks are located in the same directory as the script. Download the sample crosswalks from the following Google Drive Folder: [Symplectic Elements Michigan Experts](https://drive.google.com/drive/folders/155Ji42mKJab__ds4JaW5UlT6kIO0Sv58?usp=share_link)
