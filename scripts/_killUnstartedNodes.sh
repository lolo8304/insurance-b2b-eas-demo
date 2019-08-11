#!/bin/bash
BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

stopped=
checkPorts(){
	port=$1
	name=$2
	nof=$(netstat -an | grep $port.*LISTEN | wc -l)
	if [ $nof -eq 0 ];  then
		echo "kill Java Nodes for $name"
		cd $BASEDIR
		echo "$(eval `./_killNode.sh $name`)"
		stopped="${stopped}${name}"
	fi
}

checkPorts 10103 Notary
checkPorts 10106 Swiss
checkPorts 10109 AXA
checkPorts 10112 FZL
checkPorts 10115 Swisscanto

retval=${stopped}

