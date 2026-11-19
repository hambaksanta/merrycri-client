# 함박산타 - 프로젝트 메리크리 | 클라이언트

**팀 함박산타** · 모바일 프로그래밍 3인 팀 프로젝트 · 개발 기간 2026-11-11 ~ 11-29 (11/18 기획 전환: v2)

친구가 써준 편지로 내 크리스마스 트리가 자라고, 편지는 크리스마스에 열리며, 편지를 쓴 친구끼리 미니게임으로 점수를 겨루는 안드로이드 앱. 줄여서 **메리크리**.

| 저장소 | 내용 |
| --- | --- |
| **merrycri-client** (여기) | 안드로이드 앱 (Kotlin + Fragment + RecyclerView + Room) |
| [merrycri-server](https://github.com/hambaksanta/merrycri-server) | Firebase 보안 규칙, 데이터 구조, 예시 데이터 |

- 데이터 구조 (세 명 합의 필수): [merrycri-server/docs/DATA_MODEL.md](https://github.com/hambaksanta/merrycri-server/blob/main/docs/DATA_MODEL.md)
- **협업 규칙 (브랜치·커밋·PR): [CONTRIBUTING.md](CONTRIBUTING.md)**
- AI 공통 프롬프트 (AI에게 코드를 부탁할 때 맨 처음 붙여넣기): [docs/AI_PROMPT.md](docs/AI_PROMPT.md)
- 단계 0 할 일: [docs/TASKS_PHASE0.md](docs/TASKS_PHASE0.md)
- 기획서·구현서, 한눈에 보기 인포그래픽: ![img.png](img.png)

## 1. 처음 받아서 실행하기

1. Android Studio → **File → New → Project from Version Control** → `https://github.com/hambaksanta/merrycri-client.git` → Clone
2. 오른쪽 위 코끼리 아이콘(**Sync Project with Gradle Files**)이 끝날 때까지 기다린다
3. Device Manager에서 AVD 만들기: 시스템 이미지 이름에 **Google Play**가 붙은 것 (예: Pixel 계열, API 34 이상)
4. ▶ Run

`app/google-services.json`이 없으면 앱은 **오프라인 모드**로 실행된다. 로그인·닉네임 화면까지는 되고, 친구·편지 등 서버 기능은 안 된다. 상단에 '오프라인'이 뜨면 이 상태다.

> Android Studio가 AGP·Gradle 업그레이드를 권하면 바로 누르지 말고 단톡에 먼저 말한다. 버전은 `gradle/libs.versions.toml` 한 곳에서만 바꾼다.

## 2. Firebase 연결

Firebase 프로젝트 만들기, 규칙 배포, 예시 데이터 넣기는 **[merrycri-server](https://github.com/hambaksanta/merrycri-server)의 README**를 따른다 (B 담당).
클라이언트 쪽에서 할 일은 하나뿐이다.

- Firebase 콘솔 → 프로젝트 설정 → **Android 앱 추가** → 패키지 이름 **`com.hambaksanta.merrycri`** → `google-services.json` 다운로드 → `app/` 폴더에 넣고 커밋
- DB를 만든 **다음에** 다운로드해야 파일 안에 DB 주소가 들어간다
- AVD에서 익명 로그인 → 닉네임 설정까지 끝나고 친구 코드가 발급되면 성공

`google-services.json`은 비밀 키가 아니고, 채점하는 사람도 필요하므로 저장소에 커밋한다. 보안은 서버 저장소의 DB 규칙이 맡는다.

## 3. 폴더와 담당

| 담당 | 역할 | 폴더 |
| --- | --- | --- |
| A 박지우 | 트리·디자인 (보이는 것) | `ui/tree` (`TreeFragment`·`TreeView`·`WeatherView`), `res/values` 색·스타일, `res/drawable` 트리·장식·아이콘 |
| B 조온유 | 데이터·친구·시스템 (연결되는 것) | `MainActivity`, `data/` (Repository·Room), `notify/`, `ui/onboarding`, `ui/friends`, 카카오 공유, **merrycri-server 저장소 전체** |
| C 유수민 | 편지·게임·리더보드 (참여하는 것) | `ui/letter` (쓰기·친구 트리 방문·열람), `ui/game` (`GameActivity`·`BreakoutView`), `ui/leaderboard`, `core/audio` 사운드 연결, `docs/` |

**규칙: 내 폴더만 고친다.** 다른 사람 폴더를 고쳐야 하면 단톡에 먼저 말하거나 그 사람에게 부탁한다. `data/model/Models.kt`와 서버의 `docs/DATA_MODEL.md`는 세 명이 합의해야만 바꾸고, 바꿀 때는 두 저장소를 같이 고친다.

### 화면 구조 (v2)

v1의 3D 가상공간(Compose + SceneView, `SceneScaffold` 공통 처리)은 걷어내고, v2는 **AppCompat + Fragment + RecyclerView** 구조로 바뀐다.

- `MainActivity`가 하단 탭(홈/트리, 친구 등)과 Fragment 전환을 담당한다 (B, `chore/v2-skeleton` PR)
- 각자는 자기 담당 폴더의 Fragment/View만 채우면 되고, 공통 레이아웃(탭, 툴바)은 `MainActivity`가 제공한다
- 화면 전환 구조의 세부 사항은 `chore/v2-skeleton` PR이 머지되면 이 섹션에 추가 예정

## 4. Git 규칙

자세한 내용은 **[CONTRIBUTING.md](CONTRIBUTING.md)**. 요약:

- 브랜치: `dev`(기본, 매일 작업) / `main`(발표·제출용 최종본만). 둘 다 직접 push 금지 (GitHub 설정으로 막혀 있음). 모든 변경은 PR로만.
- 평소 PR은 `dev`로. `dev` → `main`은 세 명이 같이 확인한 뒤 **Create a merge commit**으로.
- 순서: 이슈 → 브랜치 `feat/32-letter-write` → 커밋 → PR → `dev`로 PR → 'Android 빌드' 통과 + 1명 승인 → Squash and merge
- 타입: `FEAT` `FIX` `DESIGN` `ASSET` `REFACTOR` `DOCS` `CHORE`
- 커밋·PR 제목: `FEAT: [C] 편지 쓰기 화면 레이아웃`
- 리뷰하는 사람은 AVD에서 직접 실행해 보고 승인한다.
- 이틀에 한 번(월·수·금) 30분 모여서 `dev`를 AVD 두 대로 같이 돌려본다.

## 5. 음원·이미지 넣는 법

- BGM: `app/src/main/res/raw/bgm_01.ogg` ~ `bgm_08.ogg` (곡당 3MB 이하). 넣기만 하면 자동 셔플 재생.
- 효과음: `app/src/main/res/raw/` 에 `core/audio/AudioManager.kt`의 `Sfx` 목록 이름 그대로 (`sfx_bell.ogg` 등)
- 이미지(트리·장식·아이콘 등): `app/src/main/res/drawable/` 에 소문자·숫자·밑줄만
- **라이선스를 확인한 파일만** 넣고, 아래 출처 표에 바로 적는다. 저작권 있는 노래(최신 캐럴 팝 등)는 넣지 않는다.

## 6. 테스트 팁

- 친구·실시간 기능: Device Manager에서 AVD 2개를 동시에 실행
- PR을 올리면 GitHub가 자동으로 빌드한다. 만들어진 APK는 PR의 Checks → Android 빌드 → Artifacts에서 받을 수 있다
- 편지 열람 테스트: 디버그 메뉴에서 `config/openAt`을 앞당기면 12/25 전에도 열람 화면을 테스트할 수 있다

## 7. 출처와 라이선스 (C가 채운다)

> 다운로드 과정에서 최종적으로 7개(bgm_01~07)로 확정함.

| 파일 | 출처 | 라이선스 |
| --- | --- | --- |
| bgm_01.mp3 | [Christmas Party Time — BFCMUSIC](https://pixabay.com/users/bfcmusic-16674938/) (Pixabay) | Pixabay Content License (출처 표기 불필요) |
| bgm_02.mp3 | [Festive Christmas Music — BFCMUSIC](https://pixabay.com/users/bfcmusic-16674938/) (Pixabay) | Pixabay Content License (출처 표기 불필요) |
| bgm_03.mp3 | [Christmas Spirit — SOULFULJAMTRACKS](https://pixabay.com/music/search/christmas/) (Pixabay) | Pixabay Content License (출처 표기 불필요) |
| bgm_04.mp3 | [Christmas Cheerful — SOULFULJAMTRACKS](https://pixabay.com/music/search/christmas/) (Pixabay) | Pixabay Content License (출처 표기 불필요) |
| bgm_05.mp3 | [Christmas Melody — SOULFULJAMTRACKS](https://pixabay.com/music/search/christmas/) (Pixabay) | Pixabay Content License (출처 표기 불필요) |
| bgm_06.mp3 | [Christmas — VIBEHORN](https://pixabay.com/music/search/christmas/) (Pixabay) | Pixabay Content License (출처 표기 불필요) |
| bgm_07.mp3 | [Magic Christmas Holiday — VIBEHORN](https://pixabay.com/music/search/christmas/) (Pixabay) | Pixabay Content License (출처 표기 불필요) |

### 효과음(SFX) 출처 — 17개, `res/raw/sfx_*.ogg`

Pixabay Sound Effects에서 전부 받았고, Pixabay Content License(상업적 이용 가능, 출처 표기 불필요)라 저작권 문제 없음. 0.2~1초로 잘라 모노 44.1kHz OGG(Vorbis)로 변환함. v1에서 만든 음원이지만 v2에서도 재사용 가능해 그대로 유지함 (예: 방울·종·종이·버튼 소리 등은 편지·트리·게임 효과음으로 재활용 예정).

| 파일 | 용도 | 원본 제목 (제작자) | 길이 | 라이선스 |
| --- | --- | --- | --- | --- |
| sfx_snow_step.ogg | 눈 밟기 | [Footsteps in the Snow](https://pixabay.com/sound-effects/nature-footsteps-in-the-snow-499652/) (DRAGON-STUDIO) | 0.6s | Pixabay Content License |
| sfx_bell.ogg | 방울 딸랑 | Bells Jingling (DRAGON-STUDIO) | 0.8s | Pixabay Content License |
| sfx_paper.ogg | 종이 바스락 | Paper Rustle (freesound_community) | 0.8s | Pixabay Content License |
| sfx_ribbon.ogg | 리본 묶기 | Moving fabric (freesound_community) | 0.7s | Pixabay Content License |
| sfx_bell_ring.ogg | 종소리 | Ding (DRAGON-STUDIO) | 0.72s | Pixabay Content License |
| sfx_sip.ogg | 홀짝 | Drink Sip and Swallow (freesound_community) | 0.8s | Pixabay Content License |
| sfx_crunch.ogg | 바삭 | Bite potato chips (freesound_community) | 0.8s | Pixabay Content License |
| sfx_snap.ogg | 똑(초콜릿) | Snap Sound (SoundReality) | 0.6s | Pixabay Content License |
| sfx_whoosh.ogg | 휙 | Simple Whoosh (DRAGON-STUDIO) | 0.57s | Pixabay Content License |
| sfx_thud.ogg | 퍽 | Thud Sound Effect (DRAGON-STUDIO) | 0.6s | Pixabay Content License |
| sfx_sled_slide.ogg | 썰매 미끄러짐 | Sledding on Snow (SSPsurvival) | 1.0s | Pixabay Content License |
| sfx_crackle.ogg | 장작 타닥 | Crackle Fireplace Campfire (CapaholiczSFX) | 1.0s | Pixabay Content License |
| sfx_wind.ogg | 바람 | Wind Gust (DRAGON-STUDIO) | 1.0s | Pixabay Content License |
| sfx_button.ogg | 버튼 | UI Button Click (Universfield) | 0.8s | Pixabay Content License |
| sfx_hanger.ogg | 옷걸이 딸깍 | Hangers Sliding On Metal (mrjutsumaster) | 0.8s | Pixabay Content License |
| sfx_chime.ogg | 종소리(차임) | Clear Bell Chime (Universfield) | 1.0s | Pixabay Content License |
| sfx_chew.ogg | 씹기 | Chewing a cracker (freesound_community) | 0.8s | Pixabay Content License |

(`똑(초콜릿)`은 초콜릿 쪼개는 소리를 Pixabay에서 찾지 못해 깔끔한 스냅/클릭음으로 대체함. 코드에서 쓸 이름은 `core/audio/AudioManager.kt`의 `Sfx` 목록과 맞춰서 B에게 확인받을 것.)

## 8. AI 활용 범위

이 프로젝트는 AI로 코드 초안을 만들고 팀원이 실행·수정·검토했다.
