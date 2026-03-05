import "@testing-library/jest-dom";

// 모의 EventSource 클래스 구현
class MockEventSource {
  url: string;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  readyState: number = 0;

  constructor(url: string) {
    this.url = url;
    // MockEventSource 인스턴스를 전역 보관함에 주입하여 테스트 시 제어 가능하도록 함
    (global as any).mockEventSourceInstance = this;
  }

  close() {
    this.readyState = 2; // CLOSED
  }
}

// 브라우저 내장 EventSource를 강제로 Mocking
global.EventSource = MockEventSource as any;
