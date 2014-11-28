# Cloud Foundry Integration for Eclipse
      
  The Cloud Foundry Integration for Eclipse provides first-class support for the Cloud Foundry
  PaaS: http://www.cloudfoundry.com/. It allows you to directly deploy applications from your
  workspace to a running Pivotal CF server instance, view and manage deployed applications and services,
  start and stop applications.

## Installation (Release)

  Java 7 is now a minimum execution environment requirement to install and run Cloud Foundry Integration for Eclipse.
  Please make sure your Eclipse or STS is using a Java 7 or higher JRE.

### when using Eclipse

  Go to the Eclipse Marketplace and search for "Cloud Foundry". You will find the release 
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

### Attention:
  
  Cloud Foundry Integration for Eclipse 1.5.0 and higher, as well as the nightly 
  update site and master development branch, now only support v2 Pivotal CF organizations and spaces. 
  
  V1 support for api.cloudfoundry.com as well as v1 micro and local clouds, is no longer available.

  The latest Cloud Foundry Integration for Eclipse can be update from within STS/Eclipse starting from 
  version 1.0.0. Updates from prior versions are not supported, and any version of Cloud Foundry Integration for
  Eclipse earlier than 1.0.0 (e.g. M4 and M5 closed-source versions) must be uninstalled first before newer 
  versions of the plug-in can be installed.

  Once you have the Cloud Foundry Integration for Eclipse installed from the update sites
  mentioned here, you will get updates automatically.


## Installation (latest from the CI build)

  You can always install the latest bits and pieces of the project from the update site that is
  automatically produced by the continuous integration build. This always reflects the latest
  development, so you might observe some interesting behavior here and there, and it is not guaranteed to be stable.

  http://dist.springsource.com/snapshot/TOOLS/cloudfoundry/nightly

  (put this URL into the "Install New Software" dialog of your Eclipse)
  
  
## Offline Installation

  Release versions of Cloud Foundry Integration for Eclipse can be installed offline using one of the release update 
  site zip files listed below. Once the zip file is available in an offline environment, Cloud Foundry Integration for
  Eclipse can be installed following these steps in Eclipse or STS:
  
  Help -> Install New Software -> Add -> Archive
  
  Browse to the location of the zip file, and installation should complete in offline mode.
  
  Zips for the update sites are:
  
  [Cloud Foundry Eclipse 1.7.4](http://dist.springsource.com/release/TOOLS/cloudfoundry/1.7.4/cloudfoundry-1.7.4.201411281310-RELEASE-updatesite.zip)
  
  [Cloud Foundry Eclipse 1.7.3](http://dist.springsource.com/release/TOOLS/cloudfoundry/1.7.3/cloudfoundry-1.7.3.201411202225-RELEASE-updatesite.zip)

  [Cloud Foundry Eclipse 1.7.2](http://dist.springsource.com/release/TOOLS/cloudfoundry/1.7.2/cloudfoundry-1.7.2.201410070515-RELEASE-updatesite.zip)
  
  [Cloud Foundry Eclipse 1.7.1](http://dist.springsource.com/release/TOOLS/cloudfoundry/1.7.1/cloudfoundry-1.7.1.201408270217-RELEASE-updatesite.zip)
  
  [Cloud Foundry Eclipse 1.7.0](http://dist.springsource.com/release/TOOLS/cloudfoundry/1.7.0/cloudfoundry-1.7.0.201406182004-RELEASE-updatesite.zip)
  

## Getting started

  The basic steps for using the Cloud Foundry Integration for Eclipse are described here:

  http://docs.cloudfoundry.com/docs/using/managing-apps/ide/sts.html

  Just notice that this description is targeted at users of the SpringSource Tool Suite, but
  once you have the Eclipse integration for Cloud Foundry installed, you can use it in the
  same way as described.
  
  Getting started with Cloud Foundry, including registering a new account, can be done through:
  
  http://docs.cloudfoundry.com/


## Questions and bug reports:

  If you have a question that Google can't answer, the best way is to go to the Cloud Foundry
  community forums:
  
   [General Cloud Foundry including Cloud Foundry Eclipse](https://groups.google.com/a/cloudfoundry.org/forum/#!forum/vcap-dev)

   or 

   [Cloud Foundry Eclipse only](https://groups.google.com/a/cloudfoundry.org/forum/#!forum/cf-eclipse)

  There you can also ask questions and search for other people with related or similar problems
  (and solutions). New versions of the Cloud Foundry Integration for Eclipse are announced
  there as well.
  
  Bugs and issues can be raised here in GitHub:
  
  https://github.com/SpringSource/eclipse-integration-cloudfoundry/issues 
  
  Watch for upcoming articles on Cloud Foundry by [subscribing](http://blog.cloudfoundry.com/) to the Cloud Foundry blog.
  
  Additional support can be found at the [Cloud Foundry support site](http://support.cloudfoundry.com/home).
  
## Working with the code

  If you want to work on the project itself, the best way is to install the Cloud Foundry integration
  for Eclipse into your Eclipse target platform and start from there, using the standard Eclipse way
  of plugin development using PDE.
  
  You can clone the Cloud Foundry Integration for Eclipse git repository and import the projects into
  your Eclipse workspace and start using them.

## Building the project
  
  The Cloud Foundry Integration for Eclipse uses Maven Tycho to do continuous integration builds and
  to produce p2 repos and update sites. To build the tooling yourself, you can execute:

  mvn -Pe36 package

## Contributing

  Before we accept any patches or pull requests we will need you to sign our CLA Agreement.

  1. Please select, sign and submit the appropriate CLA: [individuals](http://www.cloudfoundry.org/individualcontribution.pdf) or [corporations](http://www.cloudfoundry.org/corpcontribution.pdf). 

  2. After submitting the CLA, please [fork this repository](http://help.github.com/forking/).

  3. Set your name and email
  
	$ git config --global user.name "Firstname Lastname"
	
	$ git config --global user.email "your_email@youremail.com"

  4. Make your changes on a topic branch, commit, and push to github and open a pull request for review by the core team.

  5. Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does
mean that we can accept your contributions only after being reviewed by the core team, and you will get an author credit if we do. Active contributors might be asked to join the core team, and given the ability to merge pull requests.
