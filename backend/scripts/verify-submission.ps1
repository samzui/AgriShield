$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$java = (Get-ChildItem -Recurse src/main/java -Filter *.java).Count
$tests = (Get-ChildItem -Recurse src/test/java -Filter *.java).Count
Write-Host "Java source count: $java"
Write-Host "Test source count: $tests"
$pom = Get-Content pom.xml -Raw
foreach ($needle in @('spring-boot-starter-security','jjwt-api','spring-boot-starter-data-jpa','postgresql','mysql-connector-j')) {
  if ($pom -notmatch [regex]::Escape($needle)) { throw "Missing dependency: $needle" }
}
$security = Get-Content src/main/java/com/examly/springapp/config/SecurityConfig.java -Raw
if ($security -notmatch 'EnableMethodSecurity') { throw 'Method security missing' }
$prod = Get-Content src/main/resources/application-prod.yml -Raw
if ($prod -notmatch 'TLSv1.3') { throw 'TLS 1.3 configuration missing' }
Write-Host 'Static submission verification passed.'
Write-Host 'Run docker compose up --build for runtime verification.'
