@echo off
chcp 65001
if not exist bin mkdir bin
dir /s /b bookrental\src\*.java > sources.txt
javac -d bin -encoding UTF-8 @sources.txt
java -cp bin io.github.bookrentalteam.bookrental.App
pause
