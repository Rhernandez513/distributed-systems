#!/bin/sh

# Loosely based on Matt's contribution from UnixScriptA.html which was provided for the assignment

osascript <<END

tell application "Terminal"

    do script ("cd \"/Users/robertdavidhernandez/Documents/grad school/CSC 435/distributed-systems/blockchain/src/main/java\";javac bc.java; java bc 0")


end tell

tell application "Terminal"

    do script ("cd \"/Users/robertdavidhernandez/Documents/grad school/CSC 435/distributed-systems/blockchain/src/main/java\";javac bc.java; java bc 1")

end tell

tell application "Terminal"

    do script ("cd \"/Users/robertdavidhernandez/Documents/grad school/CSC 435/distributed-systems/blockchain/src/main/java\";javac bc.java; java bc 2")

end tell
