#!/bin/sh
for i in src/ch/lernstick/vanishedroothandler/Strings*
do
	sort $i>tmp
	mv tmp $i
done
