@echo on

cd github/endpoints-framework-maven-plugin

call mvnw.cmd clean install cobertura:cobertura -B -U

exit /b %ERRORLEVEL%
