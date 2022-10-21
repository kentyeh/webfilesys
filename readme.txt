Building WebFileSys with maven.
The directory "maven-repository" contains some non-public libraries
that are required to build WebFileSys. All other libraries that
WebFileSys depends on are automatically downloaded by maven from
public repositories.

To install the non-public libraries into your local maven repository use the following commands:
mvn install:install-file -Dfile=[your-path-to-project-webfilesys]/maven-repository/mediachest/mediautil/1.0.0/mediautil-1.0.0.jar -DgroupId=mediachest -DartifactId=mediautil -Dversion=1.0.0 -Dpackaging=jar

mvn install:install-file -Dfile=[your-path-to-project-webfilesys]/maven-repository/com/googlecode/compress-j2me/0.3/compress-j2me-0.3.jar -DgroupId=com.googlecode -DartifactId=compress-j2me -Dversion=0.3 -Dpackaging=jar

mvn install:install-file -Dfile=[your-path-to-project-webfilesys]/maven-repository/com/ice/tar/javatar/2.3/javatar-2.3.jar -DgroupId=com.ice.tar -DartifactId=javatar -Dversion=2.3 -Dpackaging=jar

mvn install:install-file -Dfile=[your-path-to-project-webfilesys]/maven-repository/com/keypoint/png-gif/1.0/png-gif-1.0.jar -DgroupId=com.keypoint -DartifactId=png-gif -Dversion=1.0 -Dpackaging=jar


Use the command
  maven package
to build the webfilesys.war web application archive which can be deployed
in any servlet container.

If you wanna build annother war file other than webfilesys.war to deploy with different contextpath,
change pom.xml <artifactId> to another context.

Config log4j2.xml set path default to TOMCAT path,
IF you gonna deply to like JBoss，change fileName path to 
"${sys:jboss.server.home.dir}/log/${project.artifactId}.log"

You can import the project into the Eclipse IDE using the provided
project files (".project", ".classpath", ".setting/*").

If you have problems getting the project to run contact the author at
frank_hoehnel@hotmail.com !
