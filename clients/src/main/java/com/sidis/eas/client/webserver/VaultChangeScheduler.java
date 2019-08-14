package com.sidis.eas.client.webserver;

import net.corda.core.contracts.LinearState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/* based on example
    https://www.callicoder.com/spring-boot-task-scheduling-with-scheduled-annotation/
 */

public abstract class VaultChangeScheduler<T extends LinearState> {
    private static final Logger logger = LoggerFactory.getLogger(VaultChangeScheduler.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;
    private final Class<T> typeOfT;

    private Long nof = 0L;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    protected VaultChangeScheduler(NodeRPCConnection rpc, Class<T> typeOfT) {
        this.typeOfT = typeOfT;
        if (Controller.DEBUG && rpc.proxy == null) {
            this.proxy = null;
            this.myLegalName = null;
            return;
        }
        this.proxy = rpc.proxy;
        this.myLegalName = rpc.proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    @Scheduled(fixedRate = 1000)
    public void scheduleTaskWithFixedRate() {
        if (proxy != null) {
            PageSpecification pageSpec = new PageSpecification(0, 5);
            Vault.Page<T> serviceStatePage = proxy.vaultQueryByWithPagingSpec(typeOfT, new QueryCriteria.VaultQueryCriteria(), pageSpec);
            Long newNof = serviceStatePage.getTotalStatesAvailable();
            logger.info("Fixed Rate Task :: Execution Time - {} - name={} - count={}", dateTimeFormatter.format(LocalDateTime.now()), this.typeOfT.getSimpleName(), newNof );
            if (newNof != this.nof) {
                this.nof = newNof;
                this.triggerChanged(typeOfT, newNof);
            }
        }
    }

    public abstract String getTopicName();


    protected void triggerChanged(Class<T> typeOfT, Long nof) {
        LinkedHashMap<String, Object> trigger = new LinkedHashMap<>();
        trigger.put("stateClass", typeOfT.getName());
        trigger.put("totalCount", nof);
        this.messagingTemplate.convertAndSend(this.getTopicName(), trigger);
    }

}
