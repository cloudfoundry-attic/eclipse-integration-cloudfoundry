# Cloud Foundry Integration for Eclipse

  
## Trial Features

This section lists trial versions of new features available in released versions of Cloud Foundry Eclipse that
are still in development and not enabled by default in the tooling, but can be enabled through manual configuration.

These features may change or even be removed or replaced in future releases of Cloud Foundry Eclipse.

### Cloud Foundry Eclipse 1.8.0 Debug Support Using ngrok.com

Version 1.8.0 of Cloud Foundry Eclipse includes an experimental feature for debugging certain types of Spring Boot
and Java applications on Cloud Foundry. The debugging feature uses ngrok.com, and is experimental and disabled by default, as it is meant to be a trial integration to launch the Eclipse Java debugger through the Cloud Foundry Eclipse editor UI.

Steps below describe how to manually enable the debug feature.

NOTE: Debugging through ngrok.com may eventually be replaced with ssh integration in [Diego](https://github.com/cloudfoundry-incubator/diego-design-notes#diego-design-notes), once ssh support is available.

For the current implementation in 1.8.0, a prerequisite to debug a Java application on Cloud Foundry is to have an [ngrok.com](http://ngrok.com) account and authtoken provided by ngrok when you register. 

Ngrok is used to establish a tunnel to the application running on Cloud Foundry. In order to establish this tunnel, an ngrok executable needs to be included in the application as well as a shell script to
run ngrok and specify the ngrok authtoken. Both ngrok executable and shell script need to be included as part of the application resources when pushing it to Cloud Foundry.

WARNING: Debugging through ngrok.com is NOT secure. It's meant as an experimental debugging implementation. Use at your own risk.

### Enabling Debugging in Cloud Foundry Eclipse

As of version 1.8.0, debugging is only supported for Spring Boot and Java apps packaged as Jar.

1. In your Spring Boot or Java project in Eclipse, create a ".profile.d" folder in src/main/resources and make sure src/main/resources is in the project's .classpath, if it isn't already. The presence of the ".profile.d" folder will enable the debug UI in the Cloud Foundry Eclipse editor for that application.

2. In ".profile.d", add the Linux ngrok executable, which can be downloaded from here: [ngrok](https://ngrok.com/download)

3. Add a ngrok.sh script in ".profile.d" with the following content: [ngrok.sh](ngrok.sh)

NOTE: Cloud Foundry Eclipse expects the ngrok output file, ngrok.txt, which is generated automatically by the ngrok.sh script when the application is started, to be present at /app/.profile.d/

This output file will contain the port to connect the Eclipse debugger to ngrok.com, and is parsed automatically by Cloud Foundry Eclipse when it attempts to connect the Eclipse debugger to the running application in Cloud Foundry.

4. Push the application to your Cloud space using drag/drop or WTP Run on Server.

5. Once the application has been pushed, double-click on it in the Eclipse Servers view to open the Cloud Foundry editor, and the "Debug" button should be enabled. The application can now be connected to the debugger by clicking the "Debug" button. It can also be disconnected at any time without stopping the application.

6. Cloud Foundry Eclipse will automatically add JAVA_OPTS environment variable to the application to be debugged with the following options:

-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n

any time the "Debug" button is pressed, if it isn't already present.

By default, Cloud Foundry Eclipse tells ngrok to use 4000 to connect to the JVM running in Cloud Foundry.

### Future Enhancements

Upcoming version 1.8.1 will automate the addition of ".profile.d" folder and the ngrok files, thus avoiding the manual steps 1-3 listed above. Debugging through ngrok.com is only a temporary solution that may be replaced with ssh when it becomes available in Diego.