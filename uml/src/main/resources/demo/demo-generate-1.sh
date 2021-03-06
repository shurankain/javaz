#!/bin/sh

jar="javaz-util-full-0.95.jar"
current="1"
prev="1"
cmd="java -cp ${jar} org.javaz.uml.RenderFtl versions/out-current.json"

#variants to proceed templates
bean_whole="1"
bean_by_one="2"
bean_diff="4"
src=../../java/shop/orm

${cmd} create-postgresql     $bean_whole     -DtemplatePath=../ -DtablePrefix=myshop_ -DoutPath=sql-scripts -Dproject=shop-v1

${cmd} orm              $bean_by_one    -DtemplatePath=../ -DoutPath=${src} -Dpackage=shop -Dsubpkg=orm

${cmd} orm-builder      $bean_by_one    -DtemplatePath=../ -DoutPath=${src} -Dpackage=shop -Dsubpkg=orm

${cmd} postgresql-dao      $bean_by_one    -DtemplatePath=../ -DoutPath=${src} -Dpackage=shop -Dsubpkg=orm

#one more additional parameter - prev file.
#${cmd} update-postgresql     $bean_diff      versions/out-ver${prev}.json -DtemplatePath=../ -DtablePrefix=myshop_ -DoutPath=sql-scripts -Dv1=${current} -Dv2=${prev}

