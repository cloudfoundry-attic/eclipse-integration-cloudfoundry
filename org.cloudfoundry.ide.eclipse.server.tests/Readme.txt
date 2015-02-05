To run the Cloud Foundry unit tests, a local properties text file should be passed as a VM argument in the unit test
launch configuration. 

The plain text properties file should contain:

url: [target Cloud URL]
username: [your username]
password: [your password]
org: [Cloud organisation]
space: [Cloud space]

The full path location of the file should then be specified by the VM argument:

-Dtest.credentials=[full path file location]


Example:

/User/myuser/testing/CFcredentials.txt contains:

url: api.run.pivotal.io
username: myusername@pivotal.io
password: mypassword
org: PivotalOrg
space: TestSpace


The file location is then passed as a VM argument:

-Dtest.credentials=/User/myuser/testing/CFcredentials.txt

The URL in the properties file can also include "http://" or "https://", instead of just the host:

url: https://api.run.pivotal.io


A "CF Base Tests.launch" configuration is provided with the VM args needed to run the Junits:

-Xmx1024M -XX:PermSize=256M -XX:MaxPermSize=256M -Dtest.credentials=/User/myuser/testing/CFcredentials.txt

However, the test.credentials arg needs to be modified to point to your local credentials text file.