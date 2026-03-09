import { useState, useEffect } from "react";
import { Vehicle } from "../types/vehicle";

export function useVehicleStream(
  url: string = process.env.NEXT_PUBLIC_API_URL ||
    "http://localhost:8080/api/vehicles/stream",
) {
  // 차량 데이터 리스트 상태 관리
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let isMounted = true;
    let retryTimeoutId: number;

    const connect = () => {
      // EventSource를 사용하여 SSE 스트림 연결
      eventSource = new EventSource(url);

      // 연결 성공 시 상태 업데이트
      eventSource.onopen = () => {
        if (isMounted) {
          setIsConnected(true);
          setError(null);
        }
      };

      // INIT 이벤트 대응 (타임아웃 방지용 더미 이벤트 등)
      eventSource.addEventListener("INIT", () => {
        if (isMounted) {
          setIsConnected(true);
          setError(null);
        }
      });

      // 데이터가 수신될 때마다 JSON 파싱 후 상태 업데이트 (덮어쓰기)
      // *참고: 테스트 코드를 위해 기본 onmessage를 등록
      eventSource.onmessage = (event: MessageEvent) => {
        try {
          const parsedData = JSON.parse(event.data) as Vehicle[];
          if (isMounted) {
            // Deduplicate by vehicleId in case the buffer sends multiple frames at once
            const uniqueVehicles = Array.from(
              new Map(parsedData.map((v) => [v.vehicleId, v])).values(),
            );
            setVehicles(uniqueVehicles);
            setIsConnected(true);
            setError(null);
          }
        } catch (error) {
          console.error("Failed to parse vehicle stream data", error);
        }
      };

      // *참고: SseEmitterService에서 .name("vehicles") 로 보내는 실제 백엔드 이벤트 대응
      eventSource.addEventListener("vehicles", (event: MessageEvent) => {
        try {
          const parsedData = JSON.parse(event.data) as Vehicle[];
          if (isMounted) {
            // Deduplicate by vehicleId in case the buffer sends multiple frames at once
            const uniqueVehicles = Array.from(
              new Map(parsedData.map((v) => [v.vehicleId, v])).values(),
            );
            setVehicles(uniqueVehicles);
            setIsConnected(true);
            setError(null);
          }
        } catch (error) {
          console.error("Failed to parse vehicle stream data", error);
        }
      });

      // 에러 및 재연결 핸들링 로직
      eventSource.onerror = (err) => {
        console.error("EventSource connection error:", err);
        if (isMounted) {
          setIsConnected(false);
          setError("스트림 연결이 끊어졌습니다. 5초 후 재연결을 시도합니다.");
        }

        if (eventSource) {
          // 커넥션을 명시적으로 닫아 리소스 누수 및 불필요한 기본 재연결 폭주 방지
          eventSource.close();
        }

        // 5초 후 재연결 시도
        retryTimeoutId = window.setTimeout(() => {
          if (isMounted) {
            console.log("Attempting to reconnect EventSource...");
            connect();
          }
        }, 5000);
      };
    };

    connect();

    // Cleanup: 컴포넌트 언마운트 시 EventSource 명시적 종료
    return () => {
      isMounted = false;
      window.clearTimeout(retryTimeoutId);
      if (eventSource) {
        eventSource.close();
      }
    };
  }, [url]);

  return { vehicles, isConnected, error };
}
