# Cloud Foundry Integration for Eclipse
      
  The Cloud Foundry Integration for Eclipse provides first-class support for the Cloud Foundry
  PaaS: http://www.cloudfoundry.org/. It allows you to directly deploy applications from your
  workspace to a running Cloud Foundry instance, view and manage deployed applications and services,
  start and stop applications and allows direct debugging when using a Micro Cloud Foundry.

  It also contains Spring UAA (User Agent Analysis), an optional component that help us to
  collect some usage data. This is completely anonymous and helps us to understand better how
  the tooling is used and how to improve it in the future.

## Installation (Release)

### when using Eclipse

  Go to the Eclipse Marketplace and search for "Cloud Foundry". You will find the release 1.0.0
  of the Cloud Foundry Integration for Eclipse and can install that into your existing Eclipse
  installation. An Eclipse JEE package is recommended.

### when using STS

  When using the SpringSource Tool Suite, please make sure to upgrade to the latest STS 2.9.1
  release and then go to the Dashboard -> Extensions and select the Cloud Foundry Integration
  for Eclipse from there.

### manually from the update site

  You can always install the latest release of the Cloud Foundry Integration for Eclipse from
  here:

  http://dist.springsource.com/release/TOOLS/cloudfoundry

  (put this URL into the "Install New Software" dialog of your Eclipse)

### Attention:

  If you have previous versions of the Cloud Foundry Integration for Eclipse already installed
  (M4 or M5, prior to this project being open-sourced), please uninstall those versions from
  your STS or Eclipse. There is no path to directly upgrade an existing installation from
  the closed-source milestone versions to this open-sourced releases.

  Once you have the Cloud Foundry Integration for Eclipse installed from the update sites
  mentioned here, you will get updates automatically.

## Installation (latest from the CI build)

  You can always install the latest bits and pieces of the project from the update site that is
  automatically produced by the continuous integration build. This reflects always the latest
  development, so you might observe some intereting behavior here and there.

  http://dist.springsource.com/snapshot/TOOLS/nightly/cloudfoundry

  (put this URL into the "Install New Software" dialog of your Eclipse)

## Getting started

  The basic steps for using the Cloud Foundry Integration for Eclipse are described here:

  http://start.cloudfoundry.com/tools/STS/configuring-STS.html

  Just notice that this description is targeted at users of the SpringSource Tool Suite, but
  once you have the Eclipse integration for Cloud Foundry installed, you can use it in the
  same way as described.

  There is also an introductory screencast featuring Micro Cloud Foundry and the Cloud Foundry
  Integration for Eclipse. (Just take care that this screencast also refers to the installation
  procedure for SpringSource Tool Suite users, not Eclipse users in general.)

  http://www.youtube.com/watch?v=cKkz_vRNG1Q

## Questions and bug reports:

  If you have a question that Google can't answer, the best way is to go to the Cloud Foundry
  community forum:

  http://support.cloudfoundry.com/home

  There you can also ask questions and search for other people with related or similar problems
  (and solutions). New versions of the Cloud Foundry Integration for Eclipse are announced
  there as well.

  With regards to bug reports and enhancement requests, please go to:

  https://issuetracker.springsource.com/browse/STS (please select the Cloud Foundry component)

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
  * Create [JIRA](https://issuetracker.springsource.com/browse/STS) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
  * Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
  * Watch for upcoming articles on Cloud Foundry by [subscribing](http://blog.cloudfoundry.com/) to the Cloud Foundry blog.

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup). Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. Active contributors might be asked to join the core team, and given the ability to merge pull requests.
