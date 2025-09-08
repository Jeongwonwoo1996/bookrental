@echo off
setlocal

cd /d C:\Users\wd\git\bookrental
chcp 65001 >nul

rem ----- 경로 설정 -----
set "JDBC_JAR=C:\Program Files\MySQL\mysql-connector-j-9.4.0\mysql-connector-j-9.4.0.jar"
set "LIB_DIR=lib"

rem ----- 폴더 준비 -----
if not exist bin mkdir bin
if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"

rem (선택) jbcrypt 존재 안내
if not exist "%LIB_DIR%\jbcrypt-0.4.jar" (
  echo [INFO] %LIB_DIR%\jbcrypt-0.4.jar 가 없습니다. 우선 다운로드해 넣어주세요.
)

rem ----- 소스 파일 목록 생성 -----
dir /s /b src\*.java > sources.txt

rem ----- 컴파일: lib의 모든 JAR + MySQL 커넥터 포함 -----
javac -cp "%LIB_DIR%\*;%JDBC_JAR%" -d bin -encoding UTF-8 @sources.txt
if errorlevel 1 (
  echo [ERROR] 컴파일 실패. 위 오류 메시지를 확인하세요.
  pause
  exit /b 1
)

rem ----- db.properties를 classpath(bin)로 복사 -----
copy /Y resources\db.properties bin\db.properties >nul

rem ----- 실행: 런타임 클래스패스에도 동일하게 포함 -----
java -cp "bin;resources;%LIB_DIR%\*;%JDBC_JAR%" io.github.bookrentalteam.bookrental.App

pause
endlocal
