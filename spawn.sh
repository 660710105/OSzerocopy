#!/bin/bash

git clean -x -f

javac zerocopy/Main.java
java zerocopy/Main $@
