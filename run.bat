@echo off
cd /d C:\Users\wd\git\bookrental

chcp 65001

:: bin 폴더 없으면 생성
if not exist bin mkdir bin

:: 소스 파일 목록 생성 (src 폴더 경로 명시)
dir /s /b src\*.java > sources.txt

:: 소스 컴파일
javac -d bin -encoding UTF-8 @sources.txt

:: db.properties를 bin으로 복사 (classpath에서 찾기 위함)
copy /Y resources\db.properties bin\db.properties >nul

:: 실행 (MySQL 드라이버 JAR 포함)
java -cp "bin;resources;C:\Program Files\MySQL\mysql-connector-j-9.4.0\mysql-connector-j-9.4.0.jar" io.github.bookrentalteam.bookrental.App

pause
