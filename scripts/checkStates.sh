#!/bin/bash
i=10103
nodes=( $NodeNames )
for n in "${nodes[@]}"
do
  echo "check State $n"
  sshpass -p test ssh user1@localhost -p 10115 -oStrictHostKeyChecking=accept-new -oUserKnownHostsFile=/dev/null run vaultQuery contractStateType: com.sidis.eas.states.ServiceState
  (( i=i+3 ))
done
