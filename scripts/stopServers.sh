#!/bin/bash
. env.sh
echo "servers killed: $(eval `./killServers.sh`)"
cd ~
