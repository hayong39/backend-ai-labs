---
name: api-gen
description: api 생성하는 스킬
---

1. 명세서에서 입력/출력/동작/예외 존재 여부를 확인해. 

2. 구현해

3. gradle pmdMain 실행. 실행한 결과 에러가 존재하면 다시 수정하고 정적검사 실행 반복

4. @reviewer 에이전트로 코드리뷰해

5. 코드리뷰 결과 출력해