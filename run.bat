@echo off
cd /d C:\Users\wd\git\bookrental

chcp 65001

:: bin 폴더 없으면 생성
if not exist bin mkdir bin

:: 소스 파일 목록 생성 (src 폴더 경로 명시)
dir /s /b src\*.java > sources.txt

:: 소스 컴파일
javac -d bin -encoding UTF-8 @sources.txt

:: 실행
java -cp bin io.github.bookrentalteam.bookrental.App

pause
