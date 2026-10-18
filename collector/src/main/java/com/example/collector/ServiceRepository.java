package com.example.collector;

import com.example.collector.model.ServiceEntity;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class ServiceRepository {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRepository.class);

    @Value("${datasource.url}")
    private String dbUrl;
    @Value("${datasource.username}")
    private String dbUser;
    @Value("${datasource.password}")
    private String dbPass;

    public List<ServiceEntity> findAllServices() {
        List<ServiceEntity> services = new ArrayList<>();
        String sql = "SELECT s.id, s.name, s.url, s.type, sv.ip, sv.ssh_user, sv.ssh_key_path FROM services s JOIN servers sv ON s.server_id = sv.id";
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                services.add(new ServiceEntity(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("url"),
                    rs.getString("type"),
                    rs.getString("ip"),
                    rs.getString("ssh_user"),
                    rs.getString("ssh_key_path")
                ));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching services from DB: {}", e.getMessage());
        }
        return services;
    }

    public Optional<String> getConfig(String key, String defaultValue) {
        String sql = "SELECT config_value FROM system_config WHERE config_key = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("config_value"));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error fetching config {}: {}", key, e.getMessage());
        }
        return Optional.ofNullable(defaultValue);
    }

    public void saveMetric(int serviceId, double value, String type) {
        String sql = "INSERT INTO metrics (service_id, value, metric_type) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, serviceId);
            pstmt.setDouble(2, value);
            pstmt.setString(3, type);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error saving metric for service {}: {}", serviceId, e.getMessage());
        }
    }
}
