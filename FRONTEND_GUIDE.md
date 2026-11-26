    # 중앙대 캠퍼스 마켓 (CauCampusMarket v2)

Figma 화면 구성안(`중고거래 앱 화면 구성안`)과 GitHub 저장소(`2026-CAU-Market`) README의 공통 데이터·함수 규격을 반영한 Android(Kotlin · Jetpack Compose) 프로젝트입니다.

## 1. GitHub README 공통 함수 규격 통일 내역

프론트엔드(ViewModel)가 호출하는 Repository 함수명을 `2026-CAU-Market/README.md` 7번 항목과 동일하게 통일했습니다.

- **AuthRepository**: `signUp`, `login`, `logout`
- **ProductRepository**: `getProducts(query, category, includeSold)`, `getMyProducts(status)`, `getProduct(productId)`, `createProduct(input, imageUris)`, `updateProduct(productId, input, imageUris)`, `deleteProduct(productId)`, `changeProductStatus(productId, status)`
- **ChatRepository**: `observeChatRooms()`, `getOrCreateChatRoom(productId)`, `observeMessages(chatId)`, `sendMessage(chatId, text)`, `sendImages(chatId, imageUris)`, `markAsRead(chatId)`, `leaveChatRoom(chatId)`

## 2. Figma UI 반영 내역

- 공통 색상(`#2563EB` Primary Blue, `#111827` 선택 칩, 상태 배지 `판매 중`/`예약 중`/`거래 완료`), 스켈레톤 로딩·빈 화면·오류 재시도 패턴 적용
- 로그인·회원가입 필드별 오류 문구 및 연분홍 인증 오류 배너
- 물품 목록 플랫 리스트, 검색 결과 건수·`거래 완료 제외` 체크박스, 우하단 파란 `+ 등록` 알약 버튼
- 물품 상세 `내 물품` 표시, 본인 물품 `수정`/`삭제` 버튼 및 삭제 확인 모달, 거래 완료 비활성화
- 물품 등록·수정 3단 거래 상태 세그먼트(`거래 완료` 변경 시 확인 모달), 나가기 확인 모달, `등록 중…`/`저장 중…` 중복 방지
- 채팅 목록 좌측 아바타·우측 `99+` 배지·물품 썸네일 및 나가기 모달, 채팅방 `⋯` 바텀시트·사진 최대 10장 전송(`sendImages`)·`읽음`/`1` 표시·상대방 퇴장 시 시스템 메시지 및 입력창 비활성화
- 마이페이지 상태별 탭 건수(`전체 N`, `판매 중 N`, `예약 중 N`, `거래 완료 N`), `+ 등록`/`수정` 버튼, 로그아웃 확인 모달
- 거래 일정 협의 화면(`교내 장소` 칩, 제안 수락/확정/취소, 채팅방 내 일정 카드 연동)

## 3. Android Studio 실행 방법

1. **OneDrive 밖의 폴더**(예: `C:\CauCampusMarket`)에 압축을 풉니다.
2. Android Studio에서 **`File → Open`**으로 안쪽 `CauCampusMarket` 폴더를 엽니다. (`gradle/wrapper/gradle-wrapper.properties`가 이미 포함되어 바로 Gradle Sync가 진행됩니다.)
3. Gradle JDK가 17 또는 21로 설정되어 있는지 확인 후 **▶(Run)**을 누릅니다.
4. 로그인 화면 하단의 **`데모 계정 자동 입력 (student@cau.ac.kr)`**을 눌러 바로 시연할 수 있습니다.
