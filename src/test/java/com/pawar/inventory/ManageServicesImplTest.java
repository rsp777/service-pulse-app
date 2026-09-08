package com.pawar.inventory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.sop.http.service.HttpService;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.constants.ScriptExtension;
import com.pawar.todo.amt.converter.ScriptExtensionConverter;
import com.pawar.todo.amt.mapper.ServiceHealthStatusMapper;
import com.pawar.todo.amt.mapper.ServiceMapper;
import com.pawar.todo.amt.model.ServiceHealthStatus;
import com.pawar.todo.amt.service.CommandService;
import com.pawar.todo.amt.service.AlertConfigurationService;
import com.pawar.todo.amt.service.ManageServicesImpl;
import com.pawar.todo.amt.service.PathService;
import com.pawar.todo.amt.service.ScriptService;
import com.pawar.todo.amt.service.ServerService;
import com.pawar.todo.amt.service.ServiceHealthStatusService;
import com.pawar.todo.amt.service.ServiceService;
import com.pawar.todo.amt.ssh.SshCommandService;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;

@ExtendWith(MockitoExtension.class)
class ManageServicesImplTest {

    @Mock private SshCommandService sshCommandService;
    @Mock private ScriptExtensionConverter scriptExtensionConverter;
    @Mock private HttpService httpService;
    @Mock private ScriptService scriptService;
    @Mock private ServiceService serviceService;
    @Mock private ServiceHealthStatusService serviceHealthStatusService;
    @Mock private ServerService serverService;
    @Mock private CommandService commandService;
    @Mock private ServiceHealthStatusMapper serviceHealthStatusMapper;
    @Mock private ServiceMapper serviceMapper;
    @Mock private PathService pathService;
    @Mock private AlertConfigurationService alertConfigurationService;
    @Mock private ServiceHealthStatus serviceHealthStatusEntity;

    private ManageServicesImpl manageServices;
    private ServerResponseDto server;
    private ServiceResponseDto service;

    @BeforeEach
    void setUp() {
        manageServices = new ManageServicesImpl(sshCommandService, scriptExtensionConverter, httpService,
            alertConfigurationService);
        manageServices.setScriptService(scriptService);
        manageServices.setServiceService(serviceService);
        manageServices.setServiceHealthStatusService(serviceHealthStatusService);
        manageServices.setServerService(serverService);
        manageServices.setCommandService(commandService);
        manageServices.setServiceHealthStatusMapper(serviceHealthStatusMapper);
        manageServices.setServiceMapper(serviceMapper);
        manageServices.setPathService(pathService);

        server = new ServerResponseDto(1, "web-01", "10.0.0.1", "Linux", ServerStatus.ONLINE.toString(),
                null, null, null, null, null, "test", "test");
        service = new ServiceResponseDto(7, null, "billing", null, null, "test", "test");
    }

    @Test
    void isServiceRunningRecognizesActiveCheckOutput() throws Exception {
        when(commandService.findCommand("CheckService")).thenReturn(Optional.of(command("systemctl", "is-active")));
        when(serverService.findServerById(1)).thenReturn(Optional.of(server));
        when(sshCommandService.execute(server, "systemctl is-active billing")).thenReturn("active\n");

        assertTrue(manageServices.isServiceRunning(1, "billing"));
    }

    @Test
    void isServiceRunningReturnsFalseForInactiveCheckOutput() throws Exception {
        when(commandService.findCommand("CheckService")).thenReturn(Optional.of(command("systemctl", "is-active")));
        when(serverService.findServerById(1)).thenReturn(Optional.of(server));
        when(sshCommandService.execute(server, "systemctl is-active billing")).thenReturn("inactive\n");

        assertFalse(manageServices.isServiceRunning(1, "billing"));
    }

    @Test
    void startServiceDoesNotRunStartScriptWhenAlreadyRunning() throws Exception {
        ServiceHealthStatusResponseDto health = new ServiceHealthStatusResponseDto(9, service, "DOWN",
                LocalDateTime.now(), null, null, null, null, "test", "test");
        when(serverService.findServerById(1)).thenReturn(Optional.of(server));
        when(serviceService.findServiceById(7)).thenReturn(Optional.of(service));
        when(serviceHealthStatusService.findServiceHealthStatusByServiceId(7)).thenReturn(Optional.of(health));
        when(commandService.findCommand("CheckService")).thenReturn(Optional.of(command("systemctl", "is-active")));
        when(sshCommandService.execute(server, "systemctl is-active billing")).thenReturn("active");
        when(serviceHealthStatusMapper.toEntity(health)).thenReturn(new ServiceHealthStatus());

        assertTrue(manageServices.startService(1, 7).contains("already running"));
        verify(sshCommandService, never()).execute(eq(server), eq("$SCRIPTS_HOME/start.sh billing"));
        verify(serviceHealthStatusService).updateServiceHealthStatus(eq(9), any(ServiceHealthStatusRequestDto.class));
    }

    @Test
    void stopServicePersistsDownAfterRemoteCheckConfirmsStopped() throws Exception {
        ServiceHealthStatusResponseDto health = new ServiceHealthStatusResponseDto(9, service, "UP",
                LocalDateTime.now(), null, null, null, null, "test", "test");
        when(serverService.findServerById(1)).thenReturn(Optional.of(server));
        when(serviceService.findServiceById(7)).thenReturn(Optional.of(service));
        when(serviceHealthStatusService.findServiceHealthStatusByServiceId(7)).thenReturn(Optional.of(health));
        when(pathService.findByPathName("SCRIPTS_HOME")).thenReturn(Optional.of(
            new PathResponseDto(2, "SCRIPTS_HOME", "/opt/scripts", null, null, null, null, "test", "test")));
        when(scriptService.findScriptByScriptName("stop")).thenReturn(Optional.of(
            new ScriptResponseDto(4, "stop", "SH", null, null, null, "test", "test")));
        when(scriptExtensionConverter.toEnum("SH")).thenReturn(ScriptExtension.SH);
        when(commandService.findCommand("CheckService")).thenReturn(Optional.of(command("systemctl", "is-active")));
        when(serverService.findServerById(1)).thenReturn(Optional.of(server));
        when(sshCommandService.execute(server, "$SCRIPTS_HOME/stop.sh billing")).thenReturn("stopped");
        when(sshCommandService.execute(server, "systemctl is-active billing")).thenReturn("inactive");
        when(serviceHealthStatusMapper.toEntity(health)).thenReturn(serviceHealthStatusEntity);
        when(serviceMapper.reqToDto(any())).thenReturn(null);

        manageServices.stopService(1, 7);

        verify(serviceHealthStatusService).updateServiceHealthStatus(eq(9), any(ServiceHealthStatusRequestDto.class));
    }

    private CommandResponseDto command(String name, String parameters) {
        return new CommandResponseDto(3, name, "CheckService", parameters, CommandStatus.CREATED.toString(),
                null, null, null, "test", "test");
    }
}
