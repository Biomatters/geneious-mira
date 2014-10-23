## Authors
Biomatters

## Summary
A Geneious plugin for running the MIRA de novo assembler

## Installation
Download the gplugin file from http://geneious.com/plugins and drag it into Geneious.

For developers:

Run ant in the root directory to build a gplugin file from source (requires apache ant and ivy)

For Biomatters developers:

1. Clone this repository somewhere outside trunk 
2. Import mira-biomatters.iml in to your project
3. Add the module to your external plugins runtime configuration 
(File->Project Structure->Modules->Select externalPlugins under External Plugins->+ (add new)->Module Dependency-> Select mira-biomatters. Check "Export" and the hit OK)
4. Run the retrieve-build-dependencies ant target to get necessary libs
5. Run with external plugins

For additional details see: https://sites.google.com/a/biomatters.com/dev/documentation/install-external-open-source-plugins