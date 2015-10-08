# IMPORTANT BREAKING CHANGES - October 2015
  
  Cloud Foundry Integration for Eclipse is now an Eclipse project under [Eclipse Cloud Development](https://projects.eclipse.org/projects/ecd), 
  and it has been renamed Eclipse Tools for Cloud Foundry (CFT):
  
  https://projects.eclipse.org/projects/ecd.cft
  
  This move will increase the exposure of the tools to a larger Eclipse user-base, and further enhance its features through additional 
  contributions and feedback from a wider community.
  
## New Repository
  
  As part of moving to Eclipse, the tools are now hosted in a new Eclipse GitHub repository:
  
  https://github.com/eclipse/cft
  
## Breaking Bundle and Extension Point Changes
  
  To comply with Eclipse project naming conventions, ALL bundle names and extension point IDs have been renamed in the new repository. 
  This means that new versions of the tools based off the new GitHub repository will NOT be backward compatible with any older versions 
  of the tools.
  
## New CLA
  
  CLA governing third-party contributions in the new repository have also changed, and the new tools will be 
  dual-licensed: Apache License 2.0 and EPL. All third-party contributors will be required to sign a new CLA before 
  Pull Requests are accepted and merged in the new repository. Please follow the links above for further information.
  
## Third-Party Pull Requests in New Repository
  
  Transition is still ongoing, and the new repository will continue to be changed frequently in the first three weeks 
  of October 2015. It is advisable NOT to submit new Pull Requests to the new repository until after October 20 2015 
  as it may require re-submission due to merge conflicts. Please check the new repository README for updates.
  
## Update Sites
  
  For the time being, the update site URLs remain unchanged:
  
  Current release:
  
  http://dist.springsource.com/release/TOOLS/cloudfoundry

  Nightly build:
  
  http://dist.springsource.com/snapshot/TOOLS/cloudfoundry/nightly
  
  However, once again, as we move forward with nightly builds based off the new repository and publish new releases, 
  they will NOT be backward compatible with the old tools. We plan on implementing detection mechanisms such that users 
  will be warned if the new versions of the tools, whether from the nightly or release sites, or the Eclipse marketplace, 
  are being installed in targets that already contain the old version. 
  
## Ongoing Transition

  We will no longer be accepting Pull Requests, or maintaining any of the branches in the old repository:
  
  https://github.com/cloudfoundry/eclipse-integration-cloudfoundry
  
  Any new enhancements and bug fixes will be pushed to the new repository.
  
  The transition is still ongoing so we will continue to update the new repository README as new changes occur.
  
## Raising Bugs and Feature Requests

  As CFT is now an Eclipse project, bugs and new feature requests should be raised via bugzilla:
  
  https://bugs.eclipse.org/bugs/
  
  We will migrate some of the existing bugs and feature requests to bugzilla as well.

## Next Release - November 2015

  The tentative release date for the first version of CFT based off the new names and IDs, and the new repository, 
  is the first week of November, 2015.
  
# [OLD README] Cloud Foundry Integration for Eclipse
      
  The Cloud Foundry Integration for Eclipse provides first-class support for the [Cloud Foundry
  PaaS] (http://www.cloudfoundry.com/). It allows you to directly deploy applications from your
  workspace to a running Pivotal CF server instance, view and manage deployed applications and services,
  start and stop applications.

## Installation (Release)

  Java 7 is now a minimum execution environment requirement to install and run Cloud Foundry Integration for Eclipse.
  Please make sure your Eclipse or STS is using a Java 7 or higher JRE.

### when using Eclipse

  Go to the [Eclipse Marketplace] (https://marketplace.eclipse.org/) and search for "Cloud Foundry". You will find the release 
  of the Cloud Foundry Integration for Eclipse and can install that into your existing Eclipse
  installation. An Eclipse JEE package is recommended.

### when using STS

  When using the Spring Tool Suite, please make sure to upgrade to the latest STS
  release and then go to the Dashboard -> Extensions and select the Cloud Foundry Integration
  for Eclipse from there.

### manually from the update site

  You can always install the latest release of the Cloud Foundry Integration for Eclipse from
  here:

  http://dist.springsource.com/release/TOOLS/cloudfoundry

  (put this URL into the "Install New Software" dialog of your Eclipse)
  
  
## Offline Installation

  Release versions of Cloud Foundry Integration for Eclipse can be installed offline using one of the release update 
  site zip files listed below. Once the zip file is available in an offline environment, Cloud Foundry Integration for
  Eclipse can be installed following these steps in Eclipse or STS:
  
  Help -> Install New Software -> Add -> Archive
  
  Browse to the location of the zip file, and installation should complete in offline mode.
  
  Zips for the update sites can be found here:
  
  [Update Sites Zips](updatesites.md)


## Getting started

  The basic steps for using the Cloud Foundry Integration for Eclipse are described here:

  http://docs.cloudfoundry.org/buildpacks/java/sts.html

  Just notice that this description is targeted at users of the SpringSource Tool Suite, but
  once you have the Eclipse integration for Cloud Foundry installed, you can use it in the
  same way as described.
  
  Getting started with Cloud Foundry, including registering a new account, can be done through:
  
  http://docs.cloudfoundry.com/


## Building the project
  
  The Cloud Foundry Integration for Eclipse uses Maven Tycho to do continuous integration builds and
  to produce p2 repos and update sites. To build the tooling yourself, you can execute:

  mvn -Pe36 package
