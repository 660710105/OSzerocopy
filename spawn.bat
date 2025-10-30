@echo off
:: Compile the Java file
javac Main.java

:: Run the compiled class, passing all batch script arguments (%*) to the Java program
java Main %*
