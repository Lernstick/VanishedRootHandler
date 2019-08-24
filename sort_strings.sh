#!/bin/sh
for i in src/main/resources/ch/lernstick/vanishedroothandler/Strings*
do
	sort $i>tmp
	mv tmp $i
done
