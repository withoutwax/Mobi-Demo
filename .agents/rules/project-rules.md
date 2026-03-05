---
trigger: always_on
---

# 🤖 Antigravity AI Agent Rules for Modapl PoC Project

## 1. Core Directives (핵심 지침)

- **Spec-First (절대 규칙):** 코드를 작성하거나 시스템 구조를 제안하기 전에, 반드시 `docs/` 및 관련 디렉토리 하위에 있는 \*_모든 마크다운(_.md) 파일들(예: `SPEC.md`, `SKILL.md` 등)을 먼저 스캔하고 읽어야 한다.
- **Spec Update:** 사용자의 요청이 기존 명세에 없는 새로운 기능이거나 아키텍처 변경을 수반할 경우, 코드를 짜기 전에 먼저 `docs/SPEC.md`를 업데이트할 것을 제안하라.
- **README Sync (필수):** 모든 주요 기능 구현이나 아키텍처 변경(예: 새로운 기술 스택 도입, 데이터 흐름 변경, 파워트레인 다형성 구조 추가 등)이 완료되면, **반드시 `README.md`를 최신 상태로 업데이트**해야 한다.
- **Visual Architecture:** `README.md`에는 면접관이 한눈에 파악할 수 있도록 전체 시스템 아키텍처(Backend-Frontend-DB-SSE)를 텍스트 기반 다이어그램(Mermaid 등)이나 명확한 개요로 유지하라.
- **Spec vs README:** `docs/SPEC.md`는 개발을 위한 상세 명세서이고, `README.md`는 면접관을 위한 '하이레벨 아키텍처 및 프로젝트 가이드'임을 인지하고 각각의 톤앤매너에 맞게 관리하라.

## 2. SDD & TDD Workflow (개발 방법론)

- 모든 개발은 **TDD (Test Driven Development)** 사이클인 `Red -> Green -> Refactor` 순서를 따른다.
- 프로덕션 코드(구현체)를 제시하기 전에, 항상 **실패하는 테스트 코드(Test Code)를 먼저 작성**하여 사용자에게 제공하라.
- 사용자가 테스트 통과를 확인하면, 그제서야 최소한의 프로덕션 코드를 작성하고 이후 리팩토링을 진행한다.

## 3. Tech Stack & Conventions (기술 스택 및 코딩 컨벤션)

- **Backend (Kotlin & Spring Boot):** - Java 대신 **Kotlin**을 최우선으로 사용하여 구현한다.
  - 불변성(Immutability)을 지향하며, DTO나 엔티티 설계 시 `data class`, 상태나 타입 분기에는 `sealed interface` / `sealed class`를 적극 활용한다.
  - DB 통신은 Spring Data JPA와 MySQL을 사용한다.
- **Frontend (Next.js & TypeScript):**
  - 명확한 타입 타이핑(Type-safety)을 유지하며, `any` 타입 사용을 엄격히 금지한다.
  - 도메인별로 폴더와 컴포넌트를 깔끔하게 정리한다.
- **Communication (응답 스타일):**
  - 불필요한 서론을 생략하고 핵심 코드와 명확한 설명만 간결하게 제공한다.
  - 코드를 제시할 때는 해당 코드가 `docs/SPEC.md`의 어느 부분을 충족하는지 주석이나 짧은 설명으로 명시한다.
