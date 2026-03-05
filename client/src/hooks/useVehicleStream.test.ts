import { renderHook, act } from "@testing-library/react";
import { useVehicleStream } from "./useVehicleStream";

describe("useVehicleStream Hook", () => {
  let mockEventSourceInfo: any;

  beforeEach(() => {
    // 이전 테스트의 잔여물 초기화
    (global as any).mockEventSourceInstance = null;
  });

  it("1. 초기 상태 검증: 훅 렌더링 시 vehicles 상태가 빈 배열([])이어야 한다", () => {
    const { result } = renderHook(() =>
      useVehicleStream("http://localhost:8080/api/vehicles/stream"),
    );

    expect(result.current.vehicles).toEqual([]);
  });

  it("2. 데이터 수신 검증: MessageEvent 발생 시 vehicles 상태가 데이터로 올바르게 덮어써져야 한다", () => {
    const { result } = renderHook(() =>
      useVehicleStream("http://localhost:8080/api/vehicles/stream"),
    );

    // 컴포넌트 마운트 시 생성된 EventSource 인스턴스 획득
    const mockInstance = (global as any).mockEventSourceInstance;
    expect(mockInstance).toBeDefined();
    expect(mockInstance.onmessage).toBeDefined();

    // 가상의 차량 데이터 리스트 JSON 문자열 생성
    const dummyData = [
      { vehicleId: "REG-EV-1", latitude: 37.5, longitude: 127.0, speed: 60.0 },
      { vehicleId: "FRE-ICE-2", latitude: 37.6, longitude: 127.1, speed: 45.0 },
    ];

    // 강제 MessageEvent 발생 (JSON.stringify 된 메시지)
    act(() => {
      mockInstance.onmessage({
        data: JSON.stringify(dummyData),
      } as MessageEvent);
    });

    // 상태 업데이트 확인
    expect(result.current.vehicles).toHaveLength(2);
    expect(result.current.vehicles[0].vehicleId).toBe("REG-EV-1");
  });

  it("3. Cleanup 검증: 훅 언마운트 시 EventSource.close()가 호출되어야 한다", () => {
    const { unmount } = renderHook(() =>
      useVehicleStream("http://localhost:8080/api/vehicles/stream"),
    );
    const mockInstance = (global as any).mockEventSourceInstance;

    expect(mockInstance.readyState).not.toBe(2); // Not closed initially

    unmount(); // 컴포넌트 해제

    expect(mockInstance.readyState).toBe(2); // 2 means CLOSED in EventSource spec
  });
});
