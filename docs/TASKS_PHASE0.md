# 함박산타 - 프로젝트 메리크리 · 단계 0 뼈대 (11/12 ~ 11/13)

완료 기준: **AVD에서 옷장 → 광장 → 나머지 공간 6개를 오갈 수 있고, AVD 두 대가 광장에서 서로 보인다.**
이 저장소에는 화면 이동(눈보라 전환), 3D 공통 틀(SceneScaffold + SceneView), 임시 3D 캐릭터·소품, 눈 효과, 접속자 동기화까지 이미 들어 있다.

## 모두
- [ ] 저장소 clone → Sync → AVD(Google Play 이미지)에서 실행 확인
- [ ] [CONTRIBUTING.md](../CONTRIBUTING.md) 읽기 → 첫 이슈 만들기 → 브랜치 따기 (예: `feat/3-lounge-fire`)
- [ ] [서버 저장소의 DATA_MODEL.md](https://github.com/hambaksanta/merrycri-server/blob/dev/docs/DATA_MODEL.md) 읽고 이의 없으면 단톡에 'OK'
- [ ] 수업 AI 활용 규정 확인

## A · 월드·캐릭터
- [ ] 레퍼런스 릴스 캡처로 화풍·시점 확정 (C와 같이)
- [ ] `CharacterView` 다듬기: 부위별 함수(drawHat 등)는 유지한 채 모양 개선
- [ ] 첫 AI 요청 예: "PlazaScene.kt 의 objects 에 Canvas로 눈 덮인 집 3채와 가운데 트리를 그려줘. 집 지붕 선을 따라 전구 40개를 놓고, 전구마다 깜빡임 주기 0.8~2초 랜덤으로."

## B · 실시간·시스템
- [ ] merrycri-server README 순서대로 Firebase 프로젝트 만들기(무료 Spark 요금제)·규칙 배포·예시 데이터 넣기
- [ ] 클라이언트 README '2. Firebase 연결' 대로 `google-services.json` 커밋
- [ ] AVD 두 대로 광장에서 서로 보이는지 확인 (상단 인원 수가 2명)
- [ ] 첫 AI 요청 예: "MerryNavHost.kt 의 화면 전환을 0.5초간 화면이 하얗게 됐다가 밝아지는 눈보라 전환으로 바꿔줘."

## C · 공간 콘텐츠·기획
- [ ] 무드보드: 릴스 캡처 + 참고 이미지 5장을 단톡/기획서에 공유
- [ ] BGM 후보 8곡 + 라이선스 링크 목록 (README 7번 표)
- [ ] 첫 AI 요청 예: "LoungeScene.kt 의 objects 에 벽난로를 만들어줘. radialGradient 3~4개가 0.1~0.3초마다 랜덤하게 크기·밝기가 바뀌고, 불티 파티클 20개가 위로 올라가고, 화면 전체에 주황 오버레이가 은은하게 일렁이게."

## 11/13(금) 밤 점검
- [ ] 각자 PR 올리기 → 'Android 빌드' 통과 + 1명 승인 → Squash and merge
- [ ] dev를 AVD 두 대로 실행: 옷장 → 광장에서 만남 → 공간 6개 이동
