package com.vuln.mall.controller;

import com.vuln.mall.entity.Board;
import com.vuln.mall.entity.Comment;
import com.vuln.mall.repository.BoardRepository;
import com.vuln.mall.repository.CommentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private CommentRepository commentRepository;

    // Use absolute path or relative to project root
    private final String UPLOAD_DIR = "uploads/";

    // List
    @GetMapping
    public String list(Model model) {
        model.addAttribute("boards", boardRepository.findAllByOrderByCreatedDateDesc());
        return "board";
    }

    // Write Form
    @GetMapping("/write")
    public String writeForm(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "board_write";
    }

    // Write Action
    // Write Action
    @PostMapping("/write")
    public String write(@RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile file,
            HttpSession session) throws IOException {

        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Board board = new Board();
        board.setTitle(title);
        board.setContent(content);
        board.setAuthor((String) session.getAttribute("user"));

        // File Upload
        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;

            // WAR 호환 경로 설정
            String webRootPath = session.getServletContext().getRealPath("/");
            File uploadDir = new File(webRootPath, "uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File dest = new File(uploadDir, storedFilename);
            file.transferTo(dest);

            board.setOriginalFileName(originalFilename);
            board.setStoredFileName(storedFilename);
        }

        boardRepository.save(board);
        return "redirect:/board";
    }

    // View
    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

        board.setViewCount(board.getViewCount() + 1);
        boardRepository.save(board);

        model.addAttribute("board", board);
        return "board_view";
    }

    // Download
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request)
            throws MalformedURLException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

        if (board.getStoredFileName() == null) {
            return ResponseEntity.notFound().build();
        }

        String webRootPath = request.getSession().getServletContext().getRealPath("/");
        Path filePath = Paths.get(webRootPath + "uploads/" + board.getStoredFileName());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            String encodedFileName = URLEncoder.encode(board.getOriginalFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Edit Form
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

        model.addAttribute("board", board);
        return "board_edit";
    }

    // Edit Action
    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile file,
            HttpSession session) throws IOException {

        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

        board.setTitle(title);
        board.setContent(content);

        // File Update
        if (file != null && !file.isEmpty()) {
            String webRootPath = session.getServletContext().getRealPath("/");
            File uploadDir = new File(webRootPath, "uploads");
            if (!uploadDir.exists())
                uploadDir.mkdirs();

            // Delete old file if exists
            if (board.getStoredFileName() != null) {
                File oldFile = new File(uploadDir, board.getStoredFileName());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            String originalFilename = file.getOriginalFilename();
            String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;

            File dest = new File(uploadDir, storedFilename);
            file.transferTo(dest);

            board.setOriginalFileName(originalFilename);
            board.setStoredFileName(storedFilename);
        }

        boardRepository.save(board);
        return "redirect:/board/view/" + id;
    }

    // Delete
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        boardRepository.deleteById(id);
        return "redirect:/board";
    }

    // Add Comment
    @PostMapping("/comment/add")
    public String addComment(@RequestParam Long boardId, @RequestParam String content, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + boardId));

        Comment comment = new Comment();
        comment.setBoard(board);
        comment.setContent(content);
        comment.setAuthor((String) session.getAttribute("user"));

        commentRepository.save(comment);

        return "redirect:/board/view/" + boardId;
    }
}
