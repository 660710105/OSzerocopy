@echo off
SETLOCAL

REM 1. Find all .java files recursively under 'zerocopy'
REM    and write their full paths to a temporary file.
(for /R zerocopy %%F in (*.java) do echo "%%~fF") > __java_files.txt

REM 2. Compile all files listed in the temp file using javac's @argfile feature.
javac -d build @__java_files.txt

REM 3. Clean up the temporary file.
del __java_files.txt

REM 4. Run the Main class from the 'zerocopy' package.
REM    %* passes all arguments from the batch script to the java command.
java -cp build zerocopy.Main %*

ENDLOCAL
