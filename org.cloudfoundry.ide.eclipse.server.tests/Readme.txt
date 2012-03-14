To run the Cloud Foundry unit tests, a local properties text file should be passed as a VM argument in the unit test launch configuration. 
The plain text properties file should contain the following two lines:

username : [Cloud Foundry server user email]
password : [Cloud Foundry server password]

The full path location of the file should then be specified by the VM argument:

-Dtest.credentials=[full path file location]

The username must be a valid user email registered to the http://api.cloudfounry.com server. 

Example:

/User/myuser/testing/CFcredentials.txt contains:

username : myusername@mydomain.com
password : mypassword


The file location is then passed as a VM argument:

-Dtest.credentials=/User/myuser/testing/CFcredentials.txt


See:
http://start.cloudfoundry.com/tools/STS/configuring-STS.html

on how to register a new user and password in api.cloudfoundry.com server.