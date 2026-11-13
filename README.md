# 함박산타 - 프로젝트 메리크리 | 클라이언트

**팀 함박산타** · 모바일 프로그래밍 3인 팀 프로젝트 · 개발 기간 2026-11-11 ~ 11-29

눈 내리는 크리스마스 마을에 최대 10명이 모여 보고, 듣고, 머무는 실시간 가상공간 안드로이드 앱. 줄여서 **메리크리**.

| 저장소 | 내용 |
| --- | --- |
| **merrycri-client** (여기) | 안드로이드 앱 (Kotlin + Jetpack Compose) |
| [merrycri-server](https://github.com/hambaksanta/merrycri-server) | Firebase 규칙, 데이터 구조, 서버 함수, 예시 데이터 |

- 데이터 구조 (세 명 합의 필수): [merrycri-server/docs/DATA_MODEL.md](https://github.com/hambaksanta/merrycri-server/blob/main/docs/DATA_MODEL.md)
- AI 공통 프롬프트: [docs/AI_PROMPT.md](docs/AI_PROMPT.md)
- 단계 0 할 일: [docs/TASKS_PHASE0.md](docs/TASKS_PHASE0.md)
- 기획서·구현서, 한눈에 보기 인포그래픽: (팀 공유 링크 붙이기)

## 1. 처음 받아서 실행하기

1. Android Studio → **File → New → Project from Version Control** → `https://github.com/hambaksanta/merrycri-client.git` → Clone
2. 오른쪽 위 코끼리 아이콘(**Sync Project with Gradle Files**)이 끝날 때까지 기다린다
3. Device Manager에서 AVD 만들기: 시스템 이미지 이름에 **Google Play**가 붙은 것 (예: Pixel 계열, API 34 이상)
4. ▶ Run

`app/google-services.json`이 없으면 앱은 **오프라인 모드**로 실행된다. 화면 이동과 혼자 걷기는 되고, 다른 사람은 안 보인다. 상단에 '오프라인'이 뜨면 이 상태다.

> Android Studio가 AGP·Gradle 업그레이드를 권하면 바로 누르지 말고 단톡에 먼저 말한다. 버전은 `gradle/libs.versions.toml` 한 곳에서만 바꾼다.

## 2. Firebase 연결

Firebase 프로젝트 만들기, 규칙 배포, 예시 데이터 넣기는 **[merrycri-server](https://github.com/hambaksanta/merrycri-server)의 README**를 따른다 (B 담당).
클라이언트 쪽에서 할 일은 하나뿐이다.

- Firebase 콘솔 → 프로젝트 설정 → **Android 앱 추가** → 패키지 이름 **`com.hambaksanta.merrycri`** → `google-services.json` 다운로드 → `app/` 폴더에 넣고 커밋
- DB를 만든 **다음에** 다운로드해야 파일 안에 DB 주소가 들어간다
- AVD 두 개를 동시에 띄우고 둘 다 광장에 들어가서 상단 인원이 2명이면 성공

`google-services.json`은 비밀 키가 아니고, 채점하는 사람도 필요하므로 저장소에 커밋한다. 보안은 서버 저장소의 DB 규칙이 맡는다.

## 3. 폴더와 담당

| 담당 | 역할 | 폴더 |
| --- | --- | --- |
| A | 월드·캐릭터 | `core/character`, `core/effects`, `feature/wardrobe`, `feature/plaza`, `feature/snowman`, `feature/sled`, `ui/theme` |
| B | 실시간·시스템 | `MainActivity`, `MerryCriApp`, `core/scene`, `core/audio`, `core/sensor`, `data/`, `notify/`, `feature/snowball`, 그리고 **merrycri-server 저장소 전체** |
| C | 공간 콘텐츠·기획 | `feature/tree`, `feature/post`, `feature/lounge`, `ui/components`, `docs/`, 음원·이미지 수집 |

**규칙: 내 폴더만 고친다.** 다른 사람 폴더를 고쳐야 하면 단톡에 먼저 말하거나 그 사람에게 부탁한다. `data/model/Models.kt`와 서버의 `docs/DATA_MODEL.md`는 세 명이 합의해야만 바꾸고, 바꿀 때는 두 저장소를 같이 고친다.

## 4. Git 규칙

- 기본 브랜치는 `main`. `main`에 직접 push 금지.
- 작업 브랜치: `feat/a-world`, `feat/b-system`, `feat/c-content`
- 하루 한 번 이상 `main`을 pull 받아서 내 브랜치에 합친다.
- 기능 하나가 AVD에서 돌아가면 바로 커밋한다.
- 커밋 메시지: `[A] 광장 전구 반짝임 추가`, `[B] presence 공간별 구독`, `[C] 벽난로 불 일렁임`
- 합칠 때는 Pull Request를 만들고, 다른 한 명이 AVD에서 실행해 보고 Merge한다.
- 이틀에 한 번(월·수·금) 30분 모여서 `main`을 AVD 두 대로 같이 돌려본다.

## 5. 음원·이미지 넣는 법

- BGM: `app/src/main/res/raw/bgm_01.ogg` ~ `bgm_08.ogg` (곡당 3MB 이하). 넣기만 하면 자동 셔플 재생.
- 효과음: `app/src/main/res/raw/` 에 `core/audio/AudioManager.kt`의 `Sfx` 목록 이름 그대로 (`sfx_bell.ogg` 등)
- 이미지: `app/src/main/res/drawable/` 에 `bg_plaza.png`, `outfit_hat_beanie.png` 처럼 소문자·숫자·밑줄만
- **라이선스를 확인한 파일만** 넣고, 아래 출처 표에 바로 적는다. 저작권 있는 노래(최신 캐럴 팝 등)는 넣지 않는다.

## 6. 테스트 팁

- 실시간 기능: Device Manager에서 AVD 2개를 동시에 실행
- 흔들기·기울이기: 에뮬레이터 ⋯ (Extended Controls) → Virtual sensors
- 선물 열기: 설정의 데모 모드를 켜면 12/25 전에도 열린다

## 7. 출처와 라이선스 (C가 채운다)

| 파일 | 출처 | 라이선스 |
| --- | --- | --- |
| | | |

## 8. AI 활용 범위

이 프로젝트는 AI(바이브코딩)로 코드 초안을 만들고 팀원이 실행·수정·검토했다. (수업 규정에 맞게 고쳐 쓴다)
