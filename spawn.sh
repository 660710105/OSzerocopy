#!/bin/bash

javac -d build zerocopy/**/*.java
javac -d build zerocopy/*.java
java -cp build zerocopy/Main $@
