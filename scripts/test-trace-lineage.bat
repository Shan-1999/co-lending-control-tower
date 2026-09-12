@echo off
set LOAN_REF=%1
if "%LOAN_REF%"=="" set LOAN_REF=LOAN-000001

echo === Trace Lineage for %LOAN_REF% ===
echo Calling GET http://localhost:8080/api/v1/audit/trace/%LOAN_REF%
echo.

curl.exe -s -X GET "http://localhost:8080/api/v1/audit/trace/%LOAN_REF%" -u operator:operator123
echo.
