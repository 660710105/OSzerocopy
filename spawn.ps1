# 1. Find all .java files recursively starting from the 'zerocopy' directory
$javaFiles = Get-ChildItem -Path ./zerocopy -Filter *.java -Recurse

# 2. Compile all found Java files, outputting .class files to the 'build' directory
#    This combines the intent of the two 'javac' lines from the original script.
javac -d build $javaFiles.FullName

# 3. Run the Main class from the 'zerocopy' package
#    $args passes all arguments from the PowerShell script to the java command.
java -cp build zerocopy.Main $args