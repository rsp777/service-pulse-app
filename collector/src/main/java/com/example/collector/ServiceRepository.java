package com.example.collector;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "SELECT id, name, url, type FROM services";
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                services.add(new ServiceEntity(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("url"),
                    rs.getString("type")
                ));
            }
        } catch (SQLException e) {
            LOG.error("Error fetching services from DB: {}", e.getMessage());
        }
        return services;
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

class ServiceEntity {
    private final int id;
    private final String name;
    private final String url;
    private final String type;

    public ServiceEntity(int id, String name, String url, String type) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.type = type;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getType() { return type; }
}
