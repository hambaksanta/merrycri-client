# 함박산타 - 프로젝트 메리크리 · 협업 규칙

클라이언트([merrycri-client](https://github.com/hambaksanta/merrycri-client))와 서버([merrycri-server](https://github.com/hambaksanta/merrycri-server)) 저장소에 똑같이 적용한다.

## 1. 한눈에 보기

```
이슈 만들기 → dev에서 브랜치 만들기 → 커밋 → dev로 PR → 빌드 통과 + 1명 승인 → Squash and merge → 브랜치 삭제
```

브랜치는 두 개를 고정으로 쓴다.

| 브랜치 | 용도 | 들어오는 길 |
| --- | --- | --- |
| `dev` (기본 브랜치) | 매일 작업이 모이는 곳 | 내 작업 브랜치 → `dev` PR |
| `main` | 발표·제출용 최종본만 | `dev` → `main` PR (세 명이 같이 확인했을 때만) |

- `dev`와 `main` 둘 다 **직접 push할 수 없다.** GitHub 설정으로 막혀 있다 (아래 5번).
- 평소 PR은 전부 `dev`로 보낸다. GitHub에서 PR을 열면 자동으로 `dev`가 선택된다.
- `main`으로 가는 PR은 마일스톤(중간 점검, 최종 제출) 때만 연다.

## 2. 타입

브랜치, 커밋, PR 제목, 이슈 제목에 같은 타입을 쓴다.

| 타입 | 언제 | 예 |
| --- | --- | --- |
| `FEAT` | 새 기능, 공간 오브젝트, 상호작용 | 라운지 장작 넣기 |
| `FIX` | 버그 수정 | 공간 이동 후 presence가 남는 문제 |
| `DESIGN` | 화면 배치, 색, 폰트, 애니메이션 느낌 조정 | 광장 전구 색 바꾸기 |
| `ASSET` | 이미지, 음원 추가·교체 | BGM 8곡 추가 |
| `REFACTOR` | 동작은 그대로, 코드만 정리 | SceneScaffold 레이어 함수 분리 |
| `DOCS` | README, 문서, 주석 | 출처 표 채우기 |
| `CHORE` | 빌드 설정, 의존성, 템플릿, 기타 | Compose BOM 올리기 |

## 3. 브랜치

`타입소문자/이슈번호-짧은설명` (영어 소문자와 하이픈)

```
feat/12-lounge-fire
fix/15-presence-leave
asset/9-bgm
```

- `dev`에서 새로 따서 만든다: Android Studio 오른쪽 아래 브랜치 이름 → `dev` 체크아웃 → Update Project → New Branch
- 하나의 브랜치 = 하나의 이슈. Merge되면 브랜치는 지운다.
- 오래 걸리는 작업이면 하루 한 번 `dev`를 내 브랜치에 합친다 (Git → Merge → `origin/dev`).

## 4. 커밋 메시지

`타입: [담당] 한 줄 설명`

```
FEAT: [C] 라운지 벽난로 불 일렁임
FIX: [B] 공간 이동 시 presence 삭제
ASSET: [C] BGM 8곡 추가
```

- 기능 하나가 AVD에서 돌아가면 바로 커밋한다. "작업 중", "ㅇㅇ" 같은 메시지는 쓰지 않는다.
- 커밋에 `google-services.json` 외의 비밀 값(키, 비밀번호)은 넣지 않는다.

## 5. Pull Request

- 제목: 커밋과 같은 형식 `FEAT: [C] 라운지 벽난로 불 일렁임`
- 본문: PR 템플릿이 자동으로 뜬다. `Closes #이슈번호`를 적으면 Merge될 때 이슈가 자동으로 닫힌다.
- 리뷰어: 나머지 두 명 중 한 명 이상 지정
- **Merge 조건 (GitHub이 강제)**
  - 승인 1명 이상
  - 클라이언트: 'Android 빌드' 체크 통과 (초록 체크). 빨간 X면 Merge 버튼이 막힌다.
  - 새 커밋을 올리면 이전 승인은 취소되고 다시 승인받는다.
  - 같은 브랜치의 최신 내용이 반영돼 있어야 한다. 뒤처졌으면 PR 화면의 **Update branch**를 누른다.
  - `dev`·`main`에 직접 push, 강제 push(force push), 브랜치 삭제 금지
- 리뷰하는 사람: PR 브랜치를 받아서 **AVD에서 직접 실행해 보고** 승인한다. 실시간 기능이면 AVD 두 대로.
- Merge 방법
  - 작업 브랜치 → `dev`: **Squash and merge** (커밋이 하나로 합쳐져 `dev` 기록이 깔끔해진다)
  - `dev` → `main`: **Create a merge commit** (여기서 squash 하면 다음번에 `dev`와 `main` 기록이 어긋나 충돌이 난다)

### `main`에 최종본 올리는 법

1. 세 명이 `dev`를 AVD 두 대로 같이 돌려 보고 문제가 없으면
2. PR 만들기: base `main` ← compare `dev`, 제목 `CHORE: [B] 중간 점검 버전 main 반영` 처럼
3. 빌드 통과 + 1명 승인 → **Create a merge commit**
4. `dev` 브랜치는 **지우지 않는다** (Merge 화면의 Delete branch 버튼 누르지 않기)

## 6. 이슈

- 이슈 템플릿 3가지: 기능(FEAT), 버그(FIX), 작업(DESIGN·ASSET·DOCS·CHORE·REFACTOR)
- 제목도 `타입: [담당] 설명` 형식
- 맡은 사람은 오른쪽 Assignees에 자기 이름을 넣는다.

## 7. 담당 폴더

내 폴더만 고친다. 다른 사람 폴더가 필요하면 고치지 말고 그 사람에게 이슈로 부탁한다.
폴더 목록은 [README의 '폴더와 담당'](README.md#3-폴더와-담당) 참고.
DB 경로·필드(`data/model/Models.kt`, 서버의 `docs/DATA_MODEL.md`)는 세 명 합의 후 두 저장소를 같이 고친다.

## 8. 충돌이 났을 때

1. 당황하지 말고 단톡에 "○○ 파일 충돌" 남기기
2. Android Studio: Git → Merge → `origin/dev` → 충돌 창에서 파일 더블클릭 → 왼쪽(내 것)·오른쪽(dev) 중 고르거나 둘 다 살리기
3. 남의 폴더 파일이 충돌하면 그 사람과 같이 푼다
