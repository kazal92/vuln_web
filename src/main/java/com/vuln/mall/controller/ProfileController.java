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
    public void downloadFile(@RequestParam("name") String filename, HttpServletRequest request,
            HttpServletResponse response) {
        // 취약점: 임의 파일 다운로드 (Arbitrary File Download) - 경로 탐색
        // 'name' 파라미터에 대한 검증이 없습니다. "../"를 사용하여 상위 디렉토리로 이동할 수 있습니다.
        // 예: /file/download?name=../../../../Windows/win.ini

        // WAR 배포 호환성: 실제 웹 루트 경로 사용
        String webRootPath = request.getSession().getServletContext().getRealPath("/");
        File uploadDir = new File(webRootPath, "uploads");

        // Ensure upload directory exists
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 1. 편의기능: 샘플 파일이 없으면 자동 생성 (사용자 요청 대응)
        if ("sample_product.xml".equals(filename)) {
            File sampleFile = new File(uploadDir, "sample_product.xml");
            if (!sampleFile.exists()) {
                try (java.io.FileWriter writer = new java.io.FileWriter(sampleFile)) {
                    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    writer.write("<products>\n");
                    writer.write("  <product>\n");
                    writer.write("    <name>Sample Product</name>\n");
                    writer.write("    <price>1000</price>\n");
                    writer.write("    <description>This is a sample product.</description>\n");
                    writer.write("  </product>\n");
                    writer.write("</products>");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        File file = new File(uploadDir, filename); // No validation on filename

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
        } else {
            // Debugging aid for Path Traversal
            // 공격자가 경로를 유추할 수 있도록 절대 경로를 에러 메시지에 포함 (취약점 실습용)
            String errorMsg = "File not found at: " + file.getAbsolutePath();
            System.out.println("DEBUG: " + errorMsg);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
