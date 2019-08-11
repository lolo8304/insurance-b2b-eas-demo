#!/bin/bash
BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
. env.sh

get_nodes(){
	retval=$(netstat -an | grep 100 | grep LISTEN | wc -l)
}
get_webservers(){
        retval=$(netstat -an | grep 1080 | grep LISTEN | wc -l)
}

get_nodes
nof=$retval

get_webservers
nof2=$retval
echo "Currently $nof CORDA nodes running"
echo "Currently $nof2 Webservers  running"
