## Authors
Biomatters

## Summary
A Geneious plugin for running the MIRA de novo assembler

## Installation
From the root folder run the following command:

> gradlew createPlugin
This will create the plugin under build/distributions, drag it into Geneious.

## Development
The development of this project follows the [Gitflow branching strategy](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).  All development is done in the develop branch and merged to master when complete.  Thus master only contains released code.

The included build.gradle can be imported as a project in IntelliJ.

Here are some few useful Gradle tasks:

Run Geneious with your plugin
> ./gradlew runGeneious

Run all the tests
> ./gradlew test

Build the plugin for distribution
> ./gradlew createPlugin

## Releases
Latest official release can be found at http://www.geneious.com/plugins/mira-plugin

See also https://github.com/Biomatters/geneious-mira/releases

## Contributing
Report bugs to support@geneious.com or log it via [GitHub issues](https://github.com/Biomatters/geneious-mira/issues)
