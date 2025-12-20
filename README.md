# 🛡️ 취약한 쇼핑몰 (Securecorp몰)

이 프로젝트는 Spring Boot로 구축된 **고의적으로 보안이 취약한** 쇼핑몰(Securecorp몰) 애플리케이션입니다.
SQL Injection, XSS, IDOR, SSRF, XXE, Insecure Deserialization 등 다양한 웹 애플리케이션 보안 취약점을 실습하고 테스트할 수 있도록 설계되었습니다.

## 🚀 실행 방법

1.  프로젝트 루트에서 터미널을 엽니다.
2.  `./gradlew bootRun` (Linux/Mac) 또는 `.\gradlew bootRun` (Windows) 명령어를 실행합니다.
3.  브라우저에서 `http://localhost:8080`으로 접속합니다.

## 💀 취약점 목록 (Vulnerability List)

이 애플리케이션에는 다음과 같은 보안 취약점이 구현되어 있습니다.

### 1. SQL Injection (SQL 삽입)
- **관련 파일**: `UserController.java`, `HomeController.java`
- **위치**: `/login` (로그인), `/search` (검색)
- **설명**: 사용자 입력값이 SQL 쿼리에 직접 결합되어 인증을 우회하거나 데이터베이스의 임의 데이터를 추출할 수 있습니다.
- **테스트**: 로그인 ID에 `' OR '1'='1` 입력 시 비밀번호 없이 로그인 성공.

### 2. Broken Authentication (깨진 인증)
- **관련 파일**: `UserController.java`
- **위치**: 로그인 후 쿠키
- **설명**: `user_id`, `role` 등 민감한 정보가 암호화되지 않은 쿠키에 평문으로 저장됩니다.
- **테스트**: 브라우저 개발자 도구에서 `role` 쿠키를 `ADMIN`으로 변경하면 관리자 권한 획득.

### 3. Mass Assignment (대량 할당 / 권한 상승)
- **관련 파일**: `UserController.java`
- **위치**: `/signup` (회원가입)
- **설명**: 회원가입 시 요청 파라미터로 `role` 값을 조작하여 관리자 권한을 가진 계정을 생성할 수 있습니다.
- **테스트**: 회원가입 요청 시 `role=ADMIN` 파라미터 추가.

### 4. XSS (교차 사이트 스크립팅)
- **관련 파일**: `ReviewController.java`, `HomeController.java`
- **Stored XSS**: `/product/detail` (리뷰 작성). 리뷰 내용에 스크립트가 포함되면 저장 후 열람 시 실행됩니다.
- **Reflected XSS**: `/search`. 검색어가 이스케이프 처리 없이 화면에 출력되어 스크립트가 실행됩니다.

### 5. IDOR (부적절한 객체 참조)
- **관련 파일**: `ReviewController.java`, `AdminController.java`, `OrderController.java`
- **위치**: `/review/delete`, `/review/update`, `/admin/user/delete`, `/order/detail?id=1`
- **설명**: 본인의 것이 아닌 데이터(리뷰, 주문, 사용자)를 ID 파라미터 조작만으로 삭제, 수정, 조회할 수 있습니다.

### 6. CSRF (크로스 사이트 요청 위조)
- **관련 파일**: `OrderController.java`
- **위치**: `/order/cancel?id=1`
- **설명**: GET 요청으로 상태를 변경(주문 취소)하며 CSRF 토큰 검증이 없습니다. 공격자가 조작된 링크를 관리자(또는 사용자)에게 클릭하게 하여 원치 않는 기능을 수행하게 할 수 있습니다.

### 7. Command Injection (명령줄 인젝션)
- **관련 파일**: `AdminController.java`
- **위치**: `/admin/health` (헬스 체크)
- **설명**: 입력한 호스트 주소가 시스템 쉘 명령어(`ping`)에 직접 전달됩니다. 메타 문자를 이용해 서버 내부 명령어를 실행할 수 있습니다.
- **테스트**: 호스트 입력에 `127.0.0.1 & whoami` 입력.

### 8. SSTI (서버 사이드 템플릿 인젝션)
- **관련 파일**: `ToolController.java`
- **위치**: `/tools/preview` (알림 템플릿 미리보기)
- **설명**: SpEL(Spring Expression Language)이 사용자 입력을 그대로 평가하여 서버 내부 코드를 실행할 수 있습니다.
- **테스트**: 템플릿에 `${T(java.lang.Runtime).getRuntime().exec('calc')}` 입력.

### 9. SSRF (서버 사이드 요청 위조)
- **관련 파일**: `ProductController.java`
- **위치**: `/admin/product/preview-image`
- **설명**: 서버가 사용자가 입력한 URL의 이미지를 대신 가져옵니다. 이를 통해 서버 내부 네트워크를 스캔하거나 로컬 파일에 접근할 수 있습니다.
- **테스트**: 이미지 URL에 `http://localhost:8080` 또는 `file:///c:/windows/win.ini` 등 입력.

### 10. XXE (XML 외부 엔티티)
- **관련 파일**: `ProductController.java`
- **위치**: `/admin/product/bulk-upload` (대량 등록)
- **설명**: XML 파일 업로드 시 외부 엔티티 참조를 허용하여 서버의 로컬 파일을 읽을 수 있습니다.

### 11. Insecure Deserialization (안전하지 않은 역직렬화)
- **관련 파일**: `CartController.java`
- **위치**: `/cart/restore` (장바구니 복원)
- **설명**: 쿠키(`cart_state`)에 저장된 자바 직렬화 객체를 검증 없이 역직렬화합니다. 가젯 체인(CommonsCollections)을 이용해 원격 코드 실행(RCE)이 가능합니다.

### 12. Path Traversal & File Upload
- **관련 파일**: `ProfileController.java`
- **위치**: `/profile` (프로필 업로드), `/file/download` (파일 다운로드)
- **설명**:
    - **파일 업로드**: 확장자 검증이 없어 `.exe`, `.jsp` 등 악성 파일을 업로드할 수 있으며, 파일명에 경로 탐색 문자(`../`)를 사용할 수 있습니다.
    - **파일 다운로드**: 파일명 파라미터 조작으로 서버의 임의 파일을 다운로드할 수 있습니다.

### 13. Broken Function Level Access Control (기능 수준 접근 제어 미흡)
- **관련 파일**: `ApiController.java`
- **위치**: `/api/v1/user/promote`
- **설명**: UI에는 노출되지 않았지만, 숨겨진 API 엔드포인트에 관리자 권한 체크 로직이 누락되어 있습니다.

### 14. JWT Weakness (취약한 JWT)
- **관련 파일**: `ApiController.java`
- **위치**: `/api/v1/auth/token`, `/api/v1/mobile/profile`
- **설명**: 매우 단순한 비밀 키(`secret123`)를 사용하거나 서명 검증이 미흡하여 토큰을 위조할 수 있습니다.

### 15. Security Misconfiguration (보안 설정 오류)
- **관련 파일**: `application.properties`
- **위치**: `/actuator`, `/h2-console`
- **설명**: Spring Actuator 엔드포인트와 H2 데이터베이스 콘솔이 인증 없이 외부에 노출되어 있습니다. 이를 통해 서버 내부 정보를 획득하거나 조작할 수 있습니다.

---

## ⚠️ 주의사항 (Disclaimer)
이 애플리케이션은 **보안 교육 및 연구 목적**으로 제작되었습니다.
**절대로 실제 운영 환경이나 공용 네트워크에 배포하지 마십시오.**
이 애플리케이션의 취약점을 악용하여 타인의 시스템을 공격하는 것은 불법입니다.
