package com.sidis.eas.client.webserver;

import com.sidis.eas.states.ServiceState;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ServiceStateScheduler extends VaultChangeScheduler<ServiceState> {
    public ServiceStateScheduler(NodeRPCConnection rpc) {
        super(rpc, ServiceState.class);
    }

    @PostConstruct
    public void installFeed() {
        this.installVaultFeedAndSubscribeToTopic("/topic/vaultChanged/sidis/eas");
    }
}
