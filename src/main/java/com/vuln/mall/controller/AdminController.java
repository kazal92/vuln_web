package com.vuln.mall.controller;

import com.vuln.mall.repository.OrderRepository;
import com.vuln.mall.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("orders", orderRepository.findAll());
        return "admin";
    }

    @GetMapping("/admin/health")
    public String healthCheckPage() {
        return "admin_health";
    }

    @PostMapping("/admin/health")
    public String healthCheck(@RequestParam String host, Model model) {
        // 취약점: 명령줄 인젝션 (Command Injection)
        // 'host' 입력을 쉘 커맨드에 직접 삽입합니다.
        // Windows: cmd.exe /c ping <host>
        // Linux: /bin/sh -c ping <host>

        StringBuilder output = new StringBuilder();
        String command = "";

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;

            if (os.contains("win")) {
                // Windows: cmd.exe /c 로 감싸서 & | 등의 메타문자 인젝션에 취약하게 만듦
                command = "ping -n 4 " + host;
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                // Linux/Unix
                command = "ping -c 4 " + host;
                processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Note: Decoding might depend on OS charset (MS949 for KR Windows)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "MS949"));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            output.append("\nExited with code: ").append(exitCode);

        } catch (Exception e) {
            output.append("명령어 실행 중 오류 발생: ").append(e.getMessage());
        }

        model.addAttribute("host", host);
        model.addAttribute("output", output.toString());
        // For security practice visibility, we removed the command output from UI in
        // polished version
        // model.addAttribute("executedCommand", command);
        return "admin_health";
    }

    @PostMapping("/admin/user/delete")
    public String deleteUser(@RequestParam Long id) {
        // 취약점: 부적절한 인가 (IDOR + Broken Access Control)
        // 1. 삭제할 'id'를 입력받습니다.
        // 2. 치명적: 현재 사용자가 ADMIN인지 확인하지 않습니다.
        // 일반 사용자(혹은 쿠키 검증이 없다면 비로그인 사용자)도 이 URL로 회원을 삭제할 수 있습니다.

        // 보호 조치: 메인 'admin' 계정은 삭제 방지
        userRepository.findById(id).ifPresent(user -> {
            if (!"admin".equals(user.getUsername())) {
                userRepository.delete(user);
            }
        });

        return "redirect:/admin";
    }
}
