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
1. Import mira-biomatters.iml in to your project
1. Add the module to your external plugins runtime configuration
1. Run the retrieve-build-dependencies ant target to get necessary libs
1. Run with external plugins
