#!/bin/sh

jar="../../uml/target/javaz-uml-1.4-SNAPSHOT-jar-with-dependencies.jar"
src="../src/main/java/"
bean_whole="1"
bean_by_one="2"
bean_difference="4"
params="-DtemplatePath=templates -Dpackage=org.javaz.copse -DtablePrefix=copse_"

java -cp ${jar} org.javaz.uml.RenderFtl versions/copse-exceptions-current.json create-exc ${bean_by_one} -DoutPath=${src}org/javaz/copse/exc ${params}




