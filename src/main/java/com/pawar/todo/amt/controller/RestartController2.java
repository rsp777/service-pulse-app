//package com.pawar.todo.amt.controller;
//
//import java.net.InetAddress;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//
////import com.pawar.todo.amt.service.TaskService;
//
////import jakarta.persistence.OptimisticLockException;
//import jakarta.servlet.http.HttpServletRequest;
//import net.schmizz.sshj.SSHClient;
//import net.schmizz.sshj.common.IOUtils;
//import net.schmizz.sshj.connection.channel.direct.Session;
//import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
//
//@RestController
//@RequestMapping("/api")
//@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
//		RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH })
//public class RestartController {
//
//	private static final Logger logger = LoggerFactory.getLogger(RestartController.class);
//
//	@Value("${server.username}")
//	private String username;
//
//	@Value("${server.password}")
//	private String password;
//
//	@Value("${server.host}")
//	private String host;
//
////	@CrossOrigin(origins = "*", allowedHeaders = "*")
//
//	@PostMapping("/start/{appname}")
//	public String startApplication(@PathVariable("appname") String appName, HttpServletRequest request) {
//
//		try {
//			logger.debug("App Name : {}", appName);
//			logger.debug("host : {}", host);
//			logger.debug("username : {}", username);
//			logger.debug("password : {}", password);
////			String authorizationHeader = request.getHeader("Authorization");
//
////			logger.info("Authorization Header: " + authorizationHeader);
//
//			SSHClient ssh = new SSHClient();
//			ssh.addHostKeyVerifier(new PromiscuousVerifier());
//			ssh.connect(host);
//			ssh.authPassword(username, password);
//
//			Session session = ssh.startSession();
//
//			Session.Command cmd1 = session.exec("/home/sop/apps/sop/scripts/start.sh " + appName);
//
//			logger.info(IOUtils.readFully(cmd1.getInputStream()).toString());
//			session.close();
//			ssh.disconnect();
//			return "Application started successfully";
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			return "Error starting the application" + e.getMessage();
//		}
//	}
//
//	@PostMapping("/stop/{appname}")
//	public String stopApplication(@PathVariable("appname") String appName, HttpServletRequest request) {
//
//		try {
//			logger.debug("App Name : {}", appName);
//			logger.debug("host : {}", host);
//			logger.debug("username : {}", username);
//			logger.debug("password : {}", password);
////			String authorizationHeader = request.getHeader("Authorization");
//
////			logger.info("Authorization Header: " + authorizationHeader);
//
//			SSHClient ssh = new SSHClient();
//			ssh.addHostKeyVerifier(new PromiscuousVerifier());
//			ssh.connect(host);
//			ssh.authPassword(username, password);
//
//			Session session = ssh.startSession();
//
//			Session.Command cmd1 = session.exec("/home/sop/apps/sop/scripts/stop.sh " + appName);
//
//			logger.info(IOUtils.readFully(cmd1.getInputStream()).toString());
//			session.close();
//			ssh.disconnect();
//			return "Application started successfully";
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			return "Error starting the application" + e.getMessage();
//		}
//	}
//
//	@GetMapping("/ping")
//	public ResponseEntity<String> ping() {
//		try {
//            InetAddress address = InetAddress.getByName(host);
//            boolean reachable = address.isReachable(2000); // Timeout in milliseconds
//            if (reachable) {
//                return ResponseEntity.ok("Server is reachable");
//            } else {
//                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is not reachable");
//            }
//        } catch (Exception e) {
//            logger.error("Error pinging server: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error pinging server: " + e.getMessage());
//        }
//	}
//
////	@CrossOrigin(origins = "*", allowedHeaders = "*")
////	@PatchMapping("/tasks/update/{id}")
////	public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestParam(required = false) String taskName,
////			boolean isCompleted) {
////
////		try {
////			
////			logger.info("updating Task : {}", id);
////
////			Task updatedTask = taskService.updateTask(id, taskName, isCompleted);
////			logger.info("Task Update : {}", updatedTask.toString());
////			
////			return new ResponseEntity<>(updatedTask, HttpStatus.OK);
////
////		} catch (TaskNotFoundException tnfe) {
////			logger.error("Task not found : {}", tnfe.getMessage());
////
////			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found: " + tnfe.getMessage());
////		} catch (OptimisticLockException ole) {
////			logger.error("Optimistic locking error : {}", ole.getMessage());
////			return ResponseEntity.status(HttpStatus.CONFLICT).body("Task update conflict: " + ole.getMessage());
////		} catch (Exception e) {
////			logger.error("Task failed to update : {}", e.getMessage());
//////			e.printStackTrace();
////			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
////					.body("Task failed to update: " + e.getMessage());
////
////		}
////	}
////
//////	@CrossOrigin(origins = "*", allowedHeaders = "*")
////	@DeleteMapping("/tasks/delete")
////	public ResponseEntity<?> deleteAllTasks() {
////
////		try {
////			taskService.deleteAll();
////			logger.info("All Tasks deleted : {}");
////
////			return new ResponseEntity<>("Tasks deleted Successfully", HttpStatus.CREATED);
////
////		} catch (Exception e) {
////			e.printStackTrace();
////			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
////					.body("All Task failed to deleted : " + e.getMessage());
////		}
////	}
////
//////	@CrossOrigin(origins = "*", allowedHeaders = "*")
////	@DeleteMapping("/tasks/delete/{id}")
////	public ResponseEntity<?> deletebyTaskId(@PathVariable Long id) {
////
////		try {
////			logger.info("Task Id: {}", id);
////
////			taskService.deleteTask(id);
////			logger.info("Task deleted : {}");
////
////			return new ResponseEntity<>("Task deleted Successfully", HttpStatus.CREATED);
////
////		} catch (Exception e) {
////			e.printStackTrace();
////			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
////					.body("Task failed to deleted : " + e.getMessage());
////		}
////	}
//
//}
