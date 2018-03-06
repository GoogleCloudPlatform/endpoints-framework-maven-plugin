@echo on

REM Java 9 does not work, force java 8
set JAVA_HOME=c:\program files\java\jdk1.8.0_152
set PATH=%JAVA_HOME%\bin;%PATH%

cd github/endpoints-framework-maven-plugin

call mvnw.cmd clean install cobertura:cobertura -B -U

exit /b %ERRORLEVEL%
