@echo off
:: Compile the Java file
javac zerocopy/Main.java

:: Run the compiled class, passing all batch script arguments (%*) to the Java program
java zerocopy/Main %*
