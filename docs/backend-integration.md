# 백엔드 연동 안내

이 프로젝트는 비용 0원의 **로컬 Firebase Emulator 과제 시연 환경**이다.
클라우드 프로젝트, 결제 연결, 실제 규칙 배포는 수행하지 않는다.
팀 계정은 향후 실제 Firebase 프로젝트가 필요할 때 사용한다.

## 서버 실행

프로젝트 루트 PowerShell에서:

```powershell
.\scripts\Start-Emulators.ps1
# 같은 네트워크의 휴대폰으로 시연할 때만:
.\scripts\Start-Emulators.ps1 -Lan
```

서버가 켜져 있어야 앱이 동작한다. Ctrl+C로 종료하면 .emulator-data에 데이터를 내보내고
다음 실행 때 가져온다. 테스트 명령은 이 시연 데이터를 가져오거나 저장하지 않는다.
Windows에서는 Ctrl+C 뒤 하위 프로세스 중단 코드가 표시될 수 있다. `Export complete`까지 기다린다.
시연 서버와 테스트 서버는 포트가 같으므로 동시에 실행하지 않는다.
LAN 모드는 신뢰할 수 있는 과제 시연 네트워크에서만 사용한다.
Emulator는 운영 서버의 인증·IAM을 재현하는 공개 인터넷 서버가 아니다.
방화벽 설정은 자동 변경하지 않는다.

처음 설치한 도구와 잠금 파일은 backend/package.json, backend/package-lock.json에 기록되어 있다.
다른 PC에서는 Node 24와 JDK를 준비하고 backend에서 npm ci 후 경로를 맞춘다.
현재 PC의 실행 스크립트는 .tools에 설치한 npm과 Android Studio JDK를 사용한다.

## 화면에서 사용할 인스턴스

Application 범위에서 AppContainer를 한 번 생성하고 각 화면에 Repository를 전달한다.

```kotlin
val container = AppContainer(applicationContext) // Android Emulator -> 10.0.2.2
// 실제 휴대폰: AppContainer(applicationContext, "개발 PC의 내부 IPv4 주소")
val auth = container.authRepository
val products = container.productRepository
val chats = container.chatRepository
```

화면·화면 이동·ViewModel을 백엔드가 수정하거나 생성하지 않았다.
기존 Hello Android 화면은 팀 프론트엔드가 교체한다.
Firebase 초기화는 AppContainer를 생성할 때 실행하며 google-services.json이나 실제 키가 필요 없다.
서버 함수는 Android 기본 HTTP API로 Firebase callable 프로토콜을 호출한다.
Auth ID 토큰을 전달하며 Functions SDK·푸시 등록 모듈은 사용하지 않는다.
이 로컬 HTTP 구성은 클라우드 배포용 설정이 아니다.
호출 형식은 [Firebase callable 프로토콜](https://firebase.google.com/docs/functions/callable-reference)을 따른다.

Android 17(API 37)에서는 로컬 네트워크 권한을 허용한 뒤 Repository를 호출한다.
debug Manifest에 `android.permission.ACCESS_LOCAL_NETWORK`를 선언했다.
프론트엔드는 API 37 이상에서 런타임 권한 요청 및 거부 상태를 처리해야 한다.
현재 기본 화면으로 시연 환경을 준비할 때는 앱 설치 후 다음 명령으로 허용할 수 있다.

```powershell
C:\Android\sdk\platform-tools\adb.exe shell pm grant com.caumarket.app android.permission.ACCESS_LOCAL_NETWORK
```

기기 테스트는 이 권한을 자동 부여한다. API 36 이하에서는 이 권한이 필요 없다.
근거: [Android 로컬 네트워크 권한](https://developer.android.com/privacy-and-security/local-network-permission).
디버그 변형만 로컬 HTTP를 허용한다. 이 구성은 운영용 릴리스 배포 구성이 아니다.

## 인증

signUp(email, password, nickname)는 비밀번호 확인 인자를 받지 않는다.
Validation.validatePasswordConfirmation(password, confirmation)으로 화면 입력 일치를 확인한다.
가입과 프로필 저장이 성공하면 signUp이 로그아웃한 뒤 성공한다.
화면은 성공 시 로그인 화면으로 이동한다.

login은 User를 반환한다. getCurrentUser는 로그아웃 상태면 null을 반환한다.
프로필 저장·Auth 롤백이 모두 실패하면 INCOMPLETE_PROFILE을 반환한다.
이 경우 로그인된 계정의 completeSignUp(nickname)으로 프로필을 완성하고 다시 로그인한다.
이메일 소유나 실제 학생 신분을 인증했다고 표시하지 않는다.

## 물품

ProductInput은 제목·설명·가격·카테고리·물품 상태·장소·장소 상세·처분 기한을 받는다.
ProductUpdate에는 입력값과 상세 조회 시 받은 expectedUpdatedAt을 함께 전달한다.
수정에는 기존 사진을 ProductPhoto.Existing(url), 추가 사진을 ProductPhoto.New(uri)로 전달한다.
기존 사진의 순서를 유지하고 새 사진을 뒤에 추가한다. 첫 번째 배열 요소가 대표 사진이다.
기존 사진을 지워도 Storage 파일 번호를 다시 매기거나 재사용하지 않는다.
거래 상태는 changeProductStatus로 별도 변경하며 정보 저장과 개별 성공·실패 결과를 확인한다.

getProducts()는 SOLD를 제외한다. query가 실제 검색어이면 includeSold 기본값이 true다.
제목과 설명을 앱에서 대소문자 구분 없이 검색한다. 임의 개수 제한·페이지 경계는 없다.
처분 기한은 서울 기준 내일 이후 날짜의 00:00을 Timestamp로 전달한다.
가격은 0~10,000,000원 정수다. 선택 문자열은 null로 정규화한다.

물품 사진은 반환된 imageUrls를 화면 이미지 로더에 전달할 수 있다.
로컬 서버에 저장된 127.0.0.1 URL은 Repository가 현재 기기에서 접근할 서버 주소로 변환한다.

## 채팅과 읽음

observeChatRooms와 observeMessages는 Flow<Result<List<...>>>다.
서버에서 확인된 스냅샷만 성공 결과로 전달한다. 오류와 빈 목록은 구별한다.
치명적 리스너 오류 뒤에는 스트림을 다시 구독한다. 로그아웃/사용자 변경 시 기존 관찰을 닫는다.

화면이 열린 동안 정상 조회한 메시지의 마지막 messageId로 읽음 처리한다:

```kotlin
chats.observeMessages(chatId).collect { result ->
    result.onSuccess { messages ->
        // 화면 상태 갱신은 프론트엔드에서 수행
        messages.lastOrNull()?.let { chats.markAsRead(chatId, it.messageId) }
    }
}
```

화면이 닫히면 관찰과 읽음 호출을 중단한다. 채팅 목록에서는 markAsRead를 호출하지 않는다.
기존 markAsRead(chatId)는 해당 Repository의 마지막 서버 확인 스냅샷 경계를 사용한다.
버퍼링이나 화면별 갱신 지연을 고려하면 명시적 messageId 오버로드를 사용한다.
내 TEXT/IMAGE 메시지의 sequence가 상대 lastReadSequence보다 크면 '1' 표시 대상이다.
lastReadAt도 함께 제공하되 같은 Timestamp를 가진 메시지는 순번으로 구별한다.
unreadCount는 실제 숫자이며 99+ 변환은 화면에서 수행한다. SYSTEM은 미확인 개수에서 제외한다.

getChatRoomInfo는 상대 닉네임과 Product?를 반환한다.
product가 null이면 '삭제된 물품'을 표시한다. 권한 오류를 삭제 상태로 표시하지 않는다.
물품 삭제/SOLD 이후에도 양쪽이 남아 있는 기존 방의 전송은 가능하다.
나가면 이전 방에 재진입할 수 없고, 상대방도 그 방에서 추가 전송할 수 없다.

## 채팅 이미지

Message.imageUrls는 공유 가능한 토큰 URL이 아니라 gs:// 참조다.
일반 URL 이미지 로더로 직접 열지 않는다.

```kotlin
val bytes: Result<ByteArray> = chats.getChatImage(chatId, message.messageId, index)
```

서버가 매 요청마다 현재 참여 여부와 메시지의 실제 경로를 확인한다.
나간 사용자는 이후 다운로드할 수 없으며 남아 있는 상대는 기존 사진을 계속 조회할 수 있다.
이미 내려받은 사본은 회수할 수 없으므로 화면 캐시 수명은 프론트엔드에서 관리한다.
사진은 앱에서 방향 보정·최대 1600px·JPEG 품질 85로 변환하고 EXIF를 제거한다.
장당 최대 2 MiB를 초과하면 오류를 반환한다.

## 재시도와 저장 일관성

생성/수정/메시지 전송은 실패 시 MarketException.operationId를 확인한다.
같은 작업의 재시도는 원래 입력과 이 ID를 requestId로 전달한다.
새 메시지는 새 요청이므로 이전 ID를 재사용하지 않는다.
같은 ID에 다른 내용이나 다른 사용자로 접근하면 CONFLICT 또는 권한 오류다.

미완료 작업은 앱 내부의 백업 제외 디렉터리에 사진 바이트·입력값을 임시 저장한다.
완료·로그아웃 시 정리한다. 비밀번호는 저장하지 않는다.
앱 재시작 후 재시도하려면 프론트엔드도 실패 항목의 operationId를 유지해야 한다.
작업 취소 UI는 이번 범위에 구현하지 않았으며 취소 후 서버의 미완료 업로드는 24시간 만료 정리 대상이다.

서버는 업로드 예약 -> 파일별 전송 -> DB 트랜잭션 확정 순으로 처리한다.
DB 확정 전에 실패하면 기존 물품·메시지를 변경하지 않는다.
물품 삭제는 DB 문서 삭제를 먼저 확정하고 파일 정리 실패는 operations에 남겨 재처리한다.
서버 시작 시 정리를 실행한다. 실행 중 수동 정리:

```powershell
.\scripts\Cleanup-Uploads.ps1
```

## 검증 명령

```powershell
.\scripts\Test-Backend.ps1
.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest
# Android 가상 기기 또는 테스트 기기를 연결한 뒤:
.\scripts\Test-Android.ps1
```

규칙 테스트는 직접 SDK 접근을 시도하고, 서버 테스트는 실제 callable 요청을 사용한다.
Admin SDK는 Security Rules를 우회하므로 서버 소유권·필수값 검증도 별도로 수행한다.
실행한 테스트 결과와 아직 검증하지 못한 항목은 최종 작업 보고에서 구분한다.


## 처음 받는 팀원과 PowerShell 실행 정책

GitHub의 feat/backend 브랜치를 체크아웃하고 Android Studio에서 프로젝트 루트를 연다.
Node 24와 npm, Android SDK 37, Android Studio JDK 25를 준비한다.
로컬 도구(.tools), node_modules, SDK 경로(local.properties), Emulator 데이터는 Git에 포함하지 않는다.
Android Studio가 SDK 경로를 설정하도록 하고, 다른 설치 경로를 쓰면 scripts/Environment.ps1의 JDK 경로를 맞춘다.
프로젝트 루트에서 의존성을 설치한다. 기존 검증은 설치 스크립트 실행 없이 수행했다.

```powershell
npm.cmd --prefix backend ci --ignore-scripts
```

서명되지 않은 스크립트 실행 오류가 나면 시스템 전체 정책을 바꾸지 않고 해당 프로세스에서 실행한다.
아래 명령은 각각 따로 실행하며 시연 서버와 테스트 서버를 동시에 켜지 않는다.

```powershell
# 서버·규칙 테스트
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Test-Backend.ps1

# Device Manager에서 Android 가상 기기를 먼저 켠 후 기기 연동 테스트
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Test-Android.ps1

# 시연 서버
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Emulators.ps1
```

조직의 그룹 정책으로 계속 차단되면 Get-ExecutionPolicy -List 결과를 확인한다.
테스트 성공 기준은 서버 pass 7 / fail 0, Android 기기 2 tests / 0 failures와 BUILD SUCCESSFUL이다.
기본 앱 화면만으로 거래를 수동 테스트할 수 있는 상태는 아니며 프론트엔드 Repository 연결이 필요하다.
