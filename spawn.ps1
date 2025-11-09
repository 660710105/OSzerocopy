# Compile the Java file

git clean -x -f

javac zerocopy/Main.java

# Run the compiled class, passing all script arguments () to the Java program
java zerocopy/Main $args
