package com.sidis.eas.client.webserver;

import com.sidis.eas.states.ServiceState;
import org.springframework.stereotype.Component;

@Component
public class ServiceStateScheduler extends VaultChangeScheduler<ServiceState> {
    public ServiceStateScheduler(NodeRPCConnection rpc) {
        super(rpc);
    }

    @Override
    public String getTopicName() {
        return "/topic/sidis/eas/vaultChanged";
    }

}
