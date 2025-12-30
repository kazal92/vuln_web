package com.vuln.mall.controller;

import com.vuln.mall.entity.Product;
import com.vuln.mall.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ProductController {

    @Autowired
    private com.vuln.mall.repository.ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/product/search")
    public String searchResult(@RequestParam(name = "q", defaultValue = "") String query, Model model) {
        model.addAttribute("query", query);

        // Mock Data for Search (실습용)
        List<Product> searchResults = new ArrayList<>();
        if (!query.isEmpty()) {
            Product p1 = new Product();
            p1.setId(101L);
            p1.setName("Premium " + query);
            p1.setPrice(1200000);
            p1.setDescription("Best " + query + " in the market.");
            p1.setImageUrl("/images/product_1.jpg");
            searchResults.add(p1);
        }

        model.addAttribute("products", searchResults);
        return "search_result";
    }

    /**
     * [VULNERABILITY] Reflected File Download (RFD)
     * 검색 결과를 파일로 내보내는 기능.
     * 사용자가 입력한 filename을 검증 없이 헤더에 사용하고, query 내용을 파일 본문에 포함함.
     * 공격: filename=update.bat&q=calc||
     */
    @GetMapping("/api/export/products")
    @org.springframework.web.bind.annotation.ResponseBody
    public void exportProducts(@RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "filename", defaultValue = "products.csv") String filename,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        // 1. Content-Disposition 헤더 조작 (파일명 변조 가능)
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // 2. 파일 내용 생성 (사용자 입력 반영 -> 명령어 주입 가능)
        String content = "Product ID,Name,Price,Description\n";
        content += "101,Premium " + query + ",1200000,Best item\n";

        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write(content);
    }

    @GetMapping("/product/detail")
    public String productDetail(@RequestParam Long id, Model model) {
        Product product = productRepository.findById(id).orElse(null);
        model.addAttribute("product", product);

        if (product != null) {
            java.util.List<com.vuln.mall.entity.Review> reviews = reviewRepository
                    .findByProductIdOrderByCreatedAtDesc(id);
            model.addAttribute("reviews", reviews);
        }

        return "product_detail";
    }

    // SSRF - 이미지 미리보기
    @PostMapping("/admin/product/preview-image")
    public String previewImage(@RequestParam String imageUrl,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        // 취약점: SSRF (Server-Side Request Forgery)
        // 사용자가 제공한 URL을 검증 없이 서버가 요청합니다.
        // 내부 서비스(localhost)에 접근하거나 file:/// 프로토콜을 사용할 수 있습니다.

        try {
            URL url = new URL(imageUrl);
            // Fix: Use generic URLConnection to support both http:// and file://
            java.net.URLConnection conn = url.openConnection();

            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).setRequestMethod("GET");
            }

            conn.setConnectTimeout(3000); // 3 seconds timeout
            conn.setReadTimeout(5000);

            // 바이트 읽기
            java.io.InputStream is = conn.getInputStream();
            byte[] imageBytes = is.readAllBytes();
            is.close();

            // Base64 변환
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

            // 컨텐츠 타입 추측 또는 기본값 jpeg
            String contentType = conn.getContentType();
            if (contentType == null)
                contentType = "image/jpeg";

            redirectAttributes.addFlashAttribute("previewMessage",
                    "이미지를 성공적으로 불러왔습니다! (" + imageBytes.length + " bytes)");
            redirectAttributes.addFlashAttribute("previewImage", "data:" + contentType + ";base64," + base64Image);

            // Raw Content for SSRF text file viewing
            if (imageUrl.startsWith("file://") || (contentType != null && contentType.startsWith("text/"))) {
                redirectAttributes.addFlashAttribute("previewContent", new String(imageBytes));
            }

            redirectAttributes.addFlashAttribute("imageUrl", imageUrl);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("previewError", "이미지 불러오기 실패: " + e.getMessage());
            redirectAttributes.addFlashAttribute("imageUrl", imageUrl);
        }

        return "redirect:/admin";
    }

    // XXE - 대량 업로드
    @PostMapping("/admin/product/bulk-upload")
    @org.springframework.web.bind.annotation.ResponseBody
    public String bulkUpload(@RequestParam("file") MultipartFile file) {
        // 취약점: XXE (XML External Entity)
        // DTD나 외부 엔티티를 비활성화하지 않고 XML을 파싱합니다.

        StringBuilder result = new StringBuilder();
        try {
            String xmlContent = new String(file.getBytes());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // 기본 DocumentBuilderFactory는 XXE에 취약합니다.
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmlContent)));

            NodeList nodes = doc.getElementsByTagName("name");
            List<String> parsedNames = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                parsedNames.add(nodes.item(i).getTextContent());
            }

            // Extract all text content to show successful entity expansion
            String fullContent = doc.getDocumentElement().getTextContent();

            result.append("XXE 처리 성공!\n");
            result.append("파싱된 이름 목록: ").append(parsedNames.toString()).append("\n");
            result.append("전체 내용 (Entity Expansion 확인): ").append(fullContent);

            return result.toString();

        } catch (Exception e) {
            return "XML 파싱 오류: " + e.getMessage();
        }
    }
}
