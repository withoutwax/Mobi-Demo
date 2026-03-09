import { test, expect } from "@playwright/test";

test("대시보드 화면이 렌더링되고 모킹된 3대의 차량 데이터를 스트림으로 받아 표시한다", async ({
  page,
}) => {
  // 1. /api/vehicles/stream 엔드포인트 SSE 네트워크 모킹 (Network Intercept)
  await page.route("/api/vehicles/stream", async (route) => {
    // 가상의 차량 3대 (EV 2대, ICE 1대)
    const mockVehicles = [
      {
        vehicleId: "REG-EV-001",
        vehicleType: "REGULAR",
        powertrain: "EV",
        latitude: 37.5,
        longitude: 127.0,
        speed: 60,
        temperature: 22,
        batteryLevel: 80,
      },
      {
        vehicleId: "FRE-EV-002",
        vehicleType: "FREIGHT",
        powertrain: "EV",
        latitude: 37.6,
        longitude: 127.1,
        speed: 50,
        temperature: 21,
        batteryLevel: 45,
      },
      {
        vehicleId: "REG-ICE-003",
        vehicleType: "REGULAR",
        powertrain: "ICE",
        latitude: 37.4,
        longitude: 126.9,
        speed: 70,
        temperature: 23,
        fuelLevel: 60,
      },
    ];

    const streamData = `event: vehicles\ndata: ${JSON.stringify(mockVehicles)}\n\n`;

    // SSE 표준에 맞는 헤더를 실어서 Mock 응답 반환
    await route.fulfill({
      status: 200,
      contentType: "text/event-stream; charset=utf-8",
      headers: {
        "Cache-Control": "no-cache",
        Connection: "keep-alive",
      },
      body: streamData,
    });
  });

  // 2. 대시보드 메인 페이지 접속
  await page.goto("/");

  // 3. 화면 제목 렌더링 검증
  await expect(
    page.locator("h1", { hasText: "차량 관제 대시보드" }),
  ).toBeVisible();

  // 4. 차량 리스트(또는 마커/카드) 아이템이 정확히 3개 렌더링 되었는지 검증
  // 여기에서는 임시로 .vehicle-item 이라는 클래스를 가진 요소가 렌더링된다고 가정 (TDD 방식이므로 나중에 UI 구현과 일치시켜야 함)
  const vehicleItems = page.locator(".vehicle-item");
  await expect(vehicleItems).toHaveCount(3);

  // 5. 모킹된 특정 차량의 ID가 화면에 렌더링되었는지 추가 검증
  await expect(page.locator("text=REG-EV-001")).toBeVisible();
  await expect(page.locator("text=REG-ICE-003")).toBeVisible();
});
