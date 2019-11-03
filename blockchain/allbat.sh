#!/bin/sh

# Loosely based on Matt's contribution from UnixScriptA.html which was provided for the assignment

osascript <<END

tell application "Terminal"

    do script ("cd \"/Users/robertdavidhernandez/Documents/grad school/CSC 435/distributed-systems/blockchain/\";\
      cp src/main/java/Blockchain.java .;\
      javac -cp .:lib/jaxb-ri/mod/jaxb-api.jar:lib/jaxb-ri/mod/jaxb-runtime.jar:lib/jaxb-ri/mod/javax.activation-api.jar:lib/jaxb-ri/mod/istack-commons-runtime.jar Blockchain.java;\
      java -cp .:lib/jaxb-ri/mod/jaxb-api.jar:lib/jaxb-ri/mod/jaxb-runtime.jar:lib/jaxb-ri/mod/javax.activation-api.jar:lib/jaxb-ri/mod/istack-commons-runtime.jar Blockchain 0\
      ")

end tell

tell application "Terminal"

    do script ("cd \"/Users/robertdavidhernandez/Documents/grad school/CSC 435/distributed-systems/blockchain/\";\
      cp src/main/java/Blockchain.java .;\
      javac -cp .:lib/jaxb-ri/mod/jaxb-api.jar:lib/jaxb-ri/mod/jaxb-runtime.jar:lib/jaxb-ri/mod/javax.activation-api.jar:lib/jaxb-ri/mod/istack-commons-runtime.jar Blockchain.java;\
      java -cp .:lib/jaxb-ri/mod/jaxb-api.jar:lib/jaxb-ri/mod/jaxb-runtime.jar:lib/jaxb-ri/mod/javax.activation-api.jar:lib/jaxb-ri/mod/istack-commons-runtime.jar Blockchain 1\
      ")

end tell

tell application "Terminal"

    do script ("cd \"/Users/robertdavidhernandez/Documents/grad school/CSC 435/distributed-systems/blockchain/\";\
      cp src/main/java/Blockchain.java .;\
      javac -cp .:lib/jaxb-ri/mod/jaxb-api.jar:lib/jaxb-ri/mod/jaxb-runtime.jar:lib/jaxb-ri/mod/javax.activation-api.jar:lib/jaxb-ri/mod/istack-commons-runtime.jar Blockchain.java;\
      java -cp .:lib/jaxb-ri/mod/jaxb-api.jar:lib/jaxb-ri/mod/jaxb-runtime.jar:lib/jaxb-ri/mod/javax.activation-api.jar:lib/jaxb-ri/mod/istack-commons-runtime.jar Blockchain 2\
      ")

end tell

