# Cloud Foundry Integration for Eclipse
      
  The Cloud Foundry Integration for Eclipse provides first-class support for the Cloud Foundry
  PaaS: http://www.cloudfoundry.com/. It allows you to directly deploy applications from your
  workspace to a running Pivotal CF instance, view and manage deployed applications and services,
  start and stop applications and allows direct debugging when using a Micro Cloud Foundry.

  It also contains Spring UAA (User Agent Analysis), an optional component that help us to
  collect some usage data. This is completely anonymous and helps us to understand better how
  the tooling is used and how to improve it in the future.

## Installation (Release)

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

  To continue using an older v1 micro or local Cloud Foundry server, please install Cloud Foundry Integration 
  for Eclipse 1.4.0 using this update site from within STS/Eclipse's "Help -> Install New Software" menu:

  http://dist.springsource.com/release/TOOLS/cloudfoundry/1.4.0/

  The latest Cloud Foundry Integration for Eclipse can be update from within STS/Eclipse starting from 
  version 1.0.0. Updates from prior versions are not supported, and any version of Cloud Foundry Integration for
  Eclipse earlier than 1.0.0 (e.g. M4 and M5 closed-source versions) must be uninstalled first before newer 
  versions of the plug-in can be installed.

  Once you have the Cloud Foundry Integration for Eclipse installed from the update sites
  mentioned here, you will get updates automatically.


## Installation (latest from the CI build)

  You can always install the latest bits and pieces of the project from the update site that is
  automatically produced by the continuous integration build. This reflects always the latest
  development, so you might observe some interesting behavior here and there.

  http://dist.springsource.com/snapshot/TOOLS/cloudfoundry/nightly

  (put this URL into the "Install New Software" dialog of your Eclipse)

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
  community forum:

  http://support.cloudfoundry.com/home

  There you can also ask questions and search for other people with related or similar problems
  (and solutions). New versions of the Cloud Foundry Integration for Eclipse are announced
  there as well.
  
  Bugs and issues can be raised here in GitHub:
  
  https://github.com/SpringSource/eclipse-integration-cloudfoundry/issues


## Working with the code

  If you wanna work on the project itself, the best way is to install the Cloud Foundry integration
  for Eclipse into your Eclipse target platform and start from there, using the standard Eclipse way
  of plugin development using PDE.
  
  You can clone the Cloud Foundry Integration for Eclipse git repository and import the projects into
  your Eclipse workspace and start using them.

## Building the project
  
  The Cloud Foundry Integration for Eclipse uses Maven Tycho to do continuous integration builds and
  to produce p2 repos and update sites. To build the tooling yourself, you can execute:

  mvn -Pe36 package

## Contributing

  Here are some ways for you to get involved in the community:

  * Get involved with the community on the community forums.  Please help out on the [forum](http://support.cloudfoundry.com/home) by responding to questions and joining the debate.
  * Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). 
  * Watch for upcoming articles on Cloud Foundry by [subscribing](http://blog.cloudfoundry.com/) to the Cloud Foundry blog.

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_eclipsecla_committer_signup). Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. Active contributors might be asked to join the core team, and given the ability to merge pull requests.
