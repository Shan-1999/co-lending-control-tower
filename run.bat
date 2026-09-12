@echo off
setlocal enabledelayedexpansion

echo ====================================================
echo   Co-Lending Control Tower - Initialization Script  
echo ====================================================

echo.
echo [1/8] Checking for Java 21...
java -version 2>&1 | findstr /I "version ""21" >nul
if errorlevel 1 (
    java -version 2>&1 | findstr /I "openjdk version ""21" >nul
    if errorlevel 1 (
        echo Error: Java 21 is required but not found.
        java -version
        exit /b 1
    )
)
echo Java 21 found.

echo.
echo [2/8] Checking PostgreSQL connectivity...
set PROFILE=
powershell -Command "if (Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet) { exit 0 } else { exit 1 }"
if %errorlevel% equ 0 (
    echo PostgreSQL is available on localhost:5432.
) else (
    echo PostgreSQL unavailable on localhost:5432. Falling back to H2 profile...
    set PROFILE=-Dspring.profiles.active=h2
)

echo.
echo [3/8] Running all unit and integration tests...
call mvnw.cmd clean test
if %errorlevel% neq 0 (
    echo Tests failed.
    exit /b 1
)
echo Tests passed successfully.

echo.
echo [4/8] Building the application...
call mvnw.cmd clean package -DskipTests

echo Starting the application in the background...
start "Control Tower App" cmd /c "java %PROFILE% -jar target\co-lending-control-tower-1.0.0.jar & pause"

echo Waiting for application to start up...
:wait_loop
timeout /t 2 /nobreak >nul
curl -s -f http://localhost:8080/api-docs >nul
if %errorlevel% neq 0 (
    echo .
    goto wait_loop
)
echo Application started successfully!

echo.
echo [5/8] Generating 2000 loans with seed=42...
curl -s -X POST "http://localhost:8080/api/v1/reset" -u operator:operator123 > nul
curl -s -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=2000&outputDir=./data/seed_a" -u operator:operator123

echo.
echo [6/8] Running ingestion and reconciliation...
echo Ingesting data...
curl -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/seed_a" -u operator:operator123

echo Running reconciliation...
curl -s -X POST "http://localhost:8080/api/v1/reconcile" -u operator:operator123

echo.
echo [7/8] Running evaluator against ground truth...
curl -s -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_a/groundtruth/ground_truth.json" -u operator:operator123

echo.
echo [8/8] Shutting down application...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do taskkill /F /PID %%a
echo Done!
