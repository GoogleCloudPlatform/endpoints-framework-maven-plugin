@echo on

cd github/endpoints-framework-maven-plugin

call mvnw.cmd clean install cobertura:cobertura -B -U
REM curl -s https://codecov.io/bash | bash

exit /b %ERRORLEVEL%
