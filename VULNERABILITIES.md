# Securecorp몰 취약점 분석 보고서

이 문서는 Securecorp몰 애플리케이션에서 식별된 주요 보안 취약점 목록을 설명합니다. 학습 및 실습 목적으로 참고하시기 바랍니다.

## 1. SQL Injection (SQL 삽입)
- **위치:** `UserController.java` (로그인 기능), `HomeController.java` (검색 기능)
- **설명:**
  1. **로그인:** 사용자 입력(`username`, `password`)을 검증이나 이스케이프 처리 없이 SQL 쿼리 문자열에 직접 연결하여 실행합니다.
  2. **검색:** 검색어(`q`)를 `LIKE` 절에 직접 연결하므로 `UNION SELECT` 등의 공격이 가능합니다.
- **코드 예시 (로그인):**
  ```java
  String sql = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
  ```
- **코드 예시 (검색):**
  ```java
  String sql = "SELECT * FROM products WHERE name LIKE '%" + q + "%'";
  ```
- **위험성:** 공격자가 로그인 인증을 우회하거나, `UNION SELECT`를 통해 사용자 테이블(`users`)의 정보를 탈취할 수 있습니다.

## 2. Reflected XSS (반사형 크로스 사이트 스크립팅)
- **위치:** `HomeController.java` (검색), `search_result.html`
- **설명:** 사용자의 검색어(`q`)를 별도의 인코딩 없이 모델에 담아 뷰로 전달하며, Thymeleaf의 `th:utext`를 사용하여 HTML로 렌더링합니다.
- **코드 예시:**
  ```html
  <!-- search_result.html -->
  <h2>검색 결과: <span th:utext="${query}" class="text-danger"></span></h2>
  ```
- **위험성:** 악의적인 스크립트가 포함된 링크를 사용자가 클릭하게 유도하여 쿠키 탈취, 세션 하이재킹, 혹은 악성 동작을 수행하게 할 수 있습니다.

## 3. Broken Authentication & Insecure Cookies (인증 및 쿠키 보안 취약)
- **위치:** `UserController.java`, `OrderController.java`
- **설명:**
  1. 로그인 성공 시 `user_id`와 `role` 같은 중요 정보를 암호화하지 않은 평문 쿠키에 저장합니다.
  2. 요청 처리 시 서버 세션 검증 없이 클라이언트가 보낸 쿠키 값만 신뢰하여 사용자를 식별합니다.
- **위험성:** 공격자가 브라우저 개발자 도구 등을 이용해 쿠키의 `user_id`를 다른 사용자의 ID로 변경하면, 비밀번호 없이 해당 사용자의 계정으로 위장할 수 있습니다. 또한 `role`을 조작하여 관리자 권한을 획득할 수 있습니다.

## 4. IDOR (부적절한 인가 - Insecure Direct Object References)
- **위치:** `OrderController.java` (`orderDetail`, `cancelOrder`)
- **설명:** 주문 상세 조회나 취소 요청 시, 요청된 주문 ID가 현재 로그인한 사용자의 소유인지 확인하는 로직이 없습니다.
- **위험성:** URL 파라미터의 주문 ID 숫자만 변경하면 타인의 주문 내역을 열람하거나 무단으로 취소할 수 있습니다.

## 5. CSRF (크로스 사이트 요청 위조)
- **위치:** `OrderController.java` (`cancelOrder`) 및 전체 앱
- **설명:** Spring Security가 적용되지 않아 CSRF 토큰 보호가 없으며, 특히 상태를 변경하는 주문 취소 기능이 `GET` 요청으로 구현되어 있습니다.
- **위험성:** 사용자가 공격자가 심어둔 이미지 태그나 링크(`<img src="/order/cancel?id=1">`)를 우연히 로드하기만 해도, 사용자의 의도와 무관하게 주문 취소 요청이 실행될 수 있습니다.

## 6. Unrestricted File Upload & Path Traversal (파일 업로드 취약점)
- **위치:** `ProfileController.java`, `WebConfig.java`
- **설명:**
  1. 파일 업로드 시 확장자나 MIME 타입을 검증하지 않습니다.
  2. 업로드된 파일명(`filename`)을 그대로 사용하여 저장 경로를 생성하므로, `../` 문자를 이용한 경로 조작(Path Traversal)이 가능합니다.
- **위험성:** 공격자가 악성 스크립트(HTML 등)를 업로드하여 Stored XSS 공격을 수행하거나, 시스템의 중요 파일을 덮어쓰거나 임의 경로에 파일을 저장할 수 있습니다.

## 7. Mass Assignment (대량 할당)
- **위치:** `UserController.java` (회원가입)
- **설명:** 회원가입 폼의 입력을 `User` 엔티티 객체에 직접 매핑(`@ModelAttribute`)합니다. 입력 필드를 제한하지 않았습니다.
- **위험성:** 공격자가 회원가입 요청 시 폼에 없는 `role=ADMIN` 파라미터를 추가하여 전송하면, 이를 그대로 받아들여 일반 회원가입 경로를 통해 관리자 계정을 생성할 수 있습니다.

## 8. Information Leakage (정보 노출)
- **위치:** `UserController.java`
- **설명:** 데이터베이스 오류 발생 시 `e.getMessage()`를 통해 내부 예외 메시지를 그대로 사용자 화면에 출력합니다.
- **위험성:** 테이블 구조, 컬럼명, 쿼리 문법 등 데이터베이스 내부 정보가 노출되어 2차 공격(SQL Injection 등)을 더 쉽게 만듭니다.

## 9. Command Injection (명령어 삽입)
- **위치:** `AdminController.java` (`/admin/health`)
- **설명:** 시스템 헬스 체크 기능에서 사용자가 입력한 호스트 주소를 검증 없이 시스템 쉘 명령어(`ping`)에 전달하여 실행합니다.
- **코드 예시:**
  ```java
  // Windows 예시
  processBuilder = new ProcessBuilder("cmd.exe", "/c", "ping -n 4 " + host);
  ```
- **위험성:** 공격자가 `127.0.0.1 & whoami`와 같은 입력을 통해 웹 서버의 권한으로 서버 운영체제 상에서 임의의 명령어를 실행할 수 있습니다. (RCE: Remote Code Execution)

## 10. SSTI (Server-Side Template Injection)
- **위치:** `ToolController.java` (`/tools/preview`)
- **설명:** 템플릿 미리보기 기능에서 사용자 입력을 Spring Expression Language (SpEL) 파서로 직접 평가합니다.
- **코드 예시:**
  ```java
  Expression exp = parser.parseExpression(template);
  Object value = exp.getValue();
  ```
- **위험성:** 공격자가 `${T(java.lang.Runtime).getRuntime().exec('calc')}`와 같은 SpEL 표현식을 입력하여 서버 내부의 Java 객체에 접근하거나 임의의 코드를 실행할 수 있습니다.
