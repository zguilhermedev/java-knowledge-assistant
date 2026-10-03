package br.com.guilherme.knowledgeassistant.tool;

import java.time.Instant;
import java.util.Map;
import br.com.guilherme.knowledgeassistant.dto.ServiceStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("knowledge")
public class ServiceStatusGateway {
    private static final Map<String, String> SIMULATED = Map.of(
        "payment-service", "DEGRADED", "recharge-service", "UP", "auth-service", "UP");

    public ServiceStatus getServiceStatus(String serviceName) {
        if (serviceName == null || serviceName.length() > 80) {
            throw new IllegalArgumentException("Nome de serviço inválido.");
        }
        return new ServiceStatus(serviceName, SIMULATED.getOrDefault(serviceName, "UNKNOWN"), true, Instant.now());
    }
}
