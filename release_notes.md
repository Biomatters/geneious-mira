# v1.0 (04 March 2014)
First release of Mira plugin for Geneious, from Biomatters.

# v1.0.1 (18 March 2017)
- GEN-28102 Fixed search failures by updating to be compatible with EuPathDB server changes
-  GEN-22191 Geneious hangs if an assembly report contains too much text (which happens on big MIRA assemblies)
-  GEN-22265 mira doesn't handle rna
-  GEN-22268 fix for space character in executable path produces error messages
-  GEN-22278 tcmalloc not enabled in Mac OS build when option specified to enable it
             static link stdc++ in Mac OS, removing the bundled shared libstdc++

# v1.1.1 (23rd February 2017)
-  GEN-28425 Release open source version of plugin
-  Nicer failure message when trying to run MIRA on sequences without quality (revision 63148)
-  Remove dependency on Geneious PrivateApi, required for compatibility with Geneious 10.1 and newer
