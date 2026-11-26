# 백엔드 구현 및 검증 기록

기준일: 2026-11-26 (Asia/Seoul). 대상: `demo-cau-market` 로컬 Emulator.
클라우드 프로젝트·결제·유료 리소스·운영 규칙 배포는 수행하지 않았다.

## 구현 범위

- 인증: 학교 도메인 검사, 닉네임·비밀번호·확인 검증, 가입 후 로그아웃, 로그인, 본인 프로필, 가입 프로필 실패 복구.
- 물품: 최신순 목록, 제목·설명 검색, 카테고리와 거래 상태 필터, 내 판매 목록, 상세, 등록·수정·삭제·상태 복구.
- 사진: 순서 유지, 첫 사진 대표, JPEG 방향 보정·리사이즈·메타데이터 제거, 업로드 예약과 확정, 미완료 파일 만료 정리.
- 채팅: 동시 방 생성 중복 방지, 실시간 목록·메시지, 텍스트·사진, 사용자별 미확인 수, 읽음 순번, 영구 나가기와 새 방 생성.
- 권한: 본인 이메일만 제공, 서버 쓰기 전용, 참여자 조회, 나간 사용자의 메시지·사진 조회 거부, SYSTEM·카운터 위조 차단.
- 재시도: 승인된 requestId와 operations 기록으로 중복 저장 방지. 앱 내부 백업 제외 디렉터리에 실패 작업 보관.

## 주요 파일과 변경 이유

| 위치 | 역할 |
| --- | --- |
| `README.md`, `docs/backend-contract.txt` | 승인된 데이터·함수·정책을 먼저 기록 |
| `app/src/main/java/com/caumarket/app/core/` | 공통 모델, 입력 DTO, 오류와 순수 검증 |
| `app/src/main/java/com/caumarket/app/data/repository/` | 프론트엔드가 의존할 공개 인터페이스 |
| `app/src/main/java/com/caumarket/app/data/firebase/` | Auth/Firestore 관찰, 로컬 HTTP callable, 이미지 변환과 재시도 저장 |
| `app/src/main/java/com/caumarket/app/di/AppContainer.kt` | Application 범위 Repository 주입 |
| `backend/src/` | 서버 권한 검사, 트랜잭션, 이미지 확정·정리 |
| `firestore.rules`, `storage.rules`, `firestore.indexes.json` | 클라이언트 직접 접근 제한과 조회 인덱스 정의 |
| `firebase.json`, `.firebaserc`, `scripts/` | 무료 시연 서버와 검증 실행 |
| `backend/test/`, `app/src/test/`, `app/src/androidTest/` | 서버·규칙·Android 검증 |

상세 Kotlin 규격과 화면 연동 예시는 [연동 안내](backend-integration.md)를 따른다.
UI, 화면 이동, ViewModel, 권한 요청 화면은 구현하지 않았다.

## 실제 실행 결과

| 검증 | 명령 또는 실행 항목 | 결과 |
| --- | --- | --- |
| Android 빌드 | `./gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest` | 성공 |
| 최종 단위 테스트·Lint | `./gradlew.bat --no-daemon :app:testDebugUnitTest :app:lintDebug` | 단위 테스트 7개 통과, Lint 오류 0개 |
| 서버·보안 규칙 | Emulator 안에서 `node --test backend/test/*.test.cjs` | 7개 통과, 실패 0개 |
| 내보내기 | 위 서버 테스트 실행 시 `--export-on-exit .tools/test-export` | Auth·Firestore·Storage 내보내기 성공 |
| 복원 | `--import .tools/test-export`로 실행한 `node backend/scripts/verify-import.cjs` | 계정, 메시지, 비공개 이미지 바이트 및 토큰 미발급 상태 복원 확인 |
| 시연 실행 스크립트 | `./scripts/Start-Emulators.ps1` 실행 후 Ctrl+C | ready 확인, `.emulator-data` 내보내기 성공, Emulator 전체 종료 |
| Android 기기 연동 | `./scripts/Test-Android.ps1`, Pixel_4 API 37 | 2개 통과, 실패·오류 0개. 전체 Repository 시나리오와 앱 컨텍스트 검사 |
| 의존성 감사 | `npm audit --omit=dev` | 취약점 0개 |
| 개발 도구 포함 감사 | `npm audit` | moderate 13개, high/critical 0개 |

서버 테스트는 동시 방 생성 6회, 동일 요청 동시 전송 6회, 전송·읽음 경합, 재전송 내용 변조,
SOLD·복구·삭제·나가기, 숫자/사진 수 제한, 서울 날짜 경계, 수정 충돌,
부분 이미지 업로드 실패 후 재시도, 만료 및 늦게 도착한 파일 정리를 확인했다.
익명·외부 도메인·타인·나간 사용자 접근과 클라이언트의 카운터·SYSTEM 위조도 거부됐다.

기기 시나리오는 실제 Kotlin Repository로 가입·로그인, 물품 사진 등록·대표 사진 삭제,
SOLD 검색 포함/기본 목록 제외, RESERVED 복구, 텍스트·사진 전송, 실시간 수신,
두 가지 markAsRead 호출, 상대 프로필, 나간 사용자 사진 차단, 남은 사용자 사진 조회,
물품 삭제 후 null 요약과 로그아웃까지 통과했다.

Windows에서 시연 스크립트를 Ctrl+C로 끝내면 하위 Node 프로세스의 중단 코드 3221225786이 표시될 수 있다.
이번 실행은 그 뒤 `Export complete`와 전체 Emulator 종료를 확인했다. 중단 코드를 테스트 실패와 혼동하지 않는다.

## 검증 한계와 후속 작업

- 실제 휴대폰 LAN 연결, 학교 네트워크 방화벽, 모든 Android 버전, 화면 통합은 미검증이다.
- Auth 프로필 생성 실패와 Auth 삭제 실패가 동시에 발생하는 복구 분기는 구현했으나 장애 주입 검증은 하지 않았다.
- 저장소 삭제 자체가 실패하는 장애는 직접 주입하지 않았다. 정리 기록 유지·만료/늦은 업로드 재정리는 검증했다.
- EXIF 회전의 모든 방향·카메라 파일 형식과 대용량 장시간 부하는 전수 검증하지 않았다.
- Lint 오류는 0개이며 기존 리소스·최신 버전·플랫폼 ExifInterface 권고 등 경고 22개는 남아 있다. 승인 없이 라이브러리를 추가하거나 BoM을 올리지 않았다.
- 개발 도구 전이 의존성의 moderate 감사 항목 13개는 남아 있다. 강제 주요 버전 변경은 하지 않았다.
- Emulator의 통과 결과가 운영 IAM·Auth 정책·인덱스·규칙 배포 검증을 대신하지 않는다. 비밀번호 8자 제한은 앱 검증이며 Auth Emulator 자체의 기본 최소 길이는 6자다.
- 이 검증 기록 작성 당시 Git 커밋·푸시는 수행하지 않았다. 이후 사용자 요청으로 feat/backend 브랜치에 게시한다. 실제 Firebase 계정/프로젝트 생성과 별도 팀 메시지 발송은 하지 않았다.
- 2차 일정 협의, 이메일 소유 인증, 비밀번호 재설정, 푸시, 추천·관심 알림은 제외했다.

개발 환경의 로그·시연 데이터·도구 캐시는 Git에서 제외한다. 테스트 계정만 사용했으며 실제 비밀번호나 비밀키를 소스에 추가하지 않았다.
