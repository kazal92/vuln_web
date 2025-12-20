package com.vuln.mall.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
public class ProfileController {

    // Using String for simpler path traversal demonstration in download
    private final String UPLOAD_DIR_STR = "uploads/";
    private final Path uploadLocation = Paths.get("uploads");

    public ProfileController() {
        try {
            Files.createDirectories(uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.vuln.mall.repository.UserRepository userRepository;

    @GetMapping("/profile")
    public String profile(jakarta.servlet.http.HttpSession session, Model model) {
        String username = (String) session.getAttribute("user");
        if (username != null) {
            com.vuln.mall.entity.User user = userRepository.findByUsername(username);
            if (user != null && user.getProfileImageUrl() != null) {
                model.addAttribute("fileUrl", user.getProfileImageUrl());
            }
        }
        return "profile";
    }

    @PostMapping("/profile/upload")
    @org.springframework.transaction.annotation.Transactional
    public String uploadFile(@RequestParam("file") MultipartFile file, jakarta.servlet.http.HttpSession session,
            Model model) {
        // 취약점: 제한 없는 파일 업로드 & 경로 탐색 업로드 (Unrestricted File Upload & Path Traversal)

        try {
            if (file.isEmpty()) {
                model.addAttribute("message", "파일을 선택해주세요.");
                return "profile";
            }

            String filename = file.getOriginalFilename();
            // 취약점: 경로 탐색 (Path Traversal)
            // 파일명에 "../"가 포함되어 있는지 확인하지 않습니다.
            // 확장자 검증도 없습니다.

            // WAR 배포 호환성 수정: 실제 웹 루트 경로 사용
            String webRootPath = session.getServletContext().getRealPath("/");
            File uploadDir = new File(webRootPath, "uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File destinationFile = new File(uploadDir, filename);
            file.transferTo(destinationFile);

            String fileUrl = "/uploads/" + filename;
            model.addAttribute("message", "파일 업로드 성공: " + filename + "!");
            model.addAttribute("fileUrl", fileUrl);

            // Persist to DB
            String username = (String) session.getAttribute("user");
            if (username != null) {
                com.vuln.mall.entity.User user = userRepository.findByUsername(username);
                if (user != null) {
                    user.setProfileImageUrl(fileUrl);
                    userRepository.save(user);
                    System.out.println("DEBUG: Profile image saved for user " + username + " -> " + fileUrl);
                } else {
                    System.out.println("DEBUG: User not found in DB: " + username);
                }
            } else {
                System.out.println("DEBUG: Session user is null");
            }

        } catch (IOException e) {
            model.addAttribute("message", "파일 업로드 실패: " + e.getMessage());
        }

        return "profile";
    }

    @GetMapping("/file/download")
    public void downloadFile(@RequestParam("name") String filename, HttpServletResponse response) {
        // 취약점: 임의 파일 다운로드 (Arbitrary File Download) - 경로 탐색
        // 'name' 파라미터에 대한 검증이 없습니다. "../"를 사용하여 상위 디렉토리로 이동할 수 있습니다.
        // 예: /file/download?name=../../../../Windows/win.ini

        File file = new File(UPLOAD_DIR_STR + filename);

        if (file.exists()) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

            try (FileInputStream fis = new FileInputStream(file);
                    OutputStream os = response.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
