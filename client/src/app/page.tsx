"use client";

import { useVehicleStream } from "@/hooks/useVehicleStream";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Vehicle } from "@/types/vehicle";

export default function DashboardPage() {
  const { vehicles, isConnected, error } = useVehicleStream();

  return (
    <div className="container mx-auto p-4 max-w-5xl h-screen flex flex-col">
      <header className="mb-6 flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">
          실시간 차량 관제 대시보드
        </h1>
        <div className="flex items-center gap-2">
          {error ? (
            <Badge variant="destructive" className="text-sm py-1 px-3">
              🔴 {error}
            </Badge>
          ) : isConnected ? (
            <Badge
              variant="default"
              className="bg-green-600 hover:bg-green-700 text-sm py-1 px-3"
            >
              🟢 실시간 연결됨
            </Badge>
          ) : (
            <Badge variant="secondary" className="text-sm py-1 px-3">
              🟡 연결 중...
            </Badge>
          )}
        </div>
      </header>

      <ScrollArea className="flex-1 rounded-md border p-4 bg-slate-50/50">
        {vehicles.length === 0 ? (
          <div className="flex items-center justify-center h-40 text-slate-500">
            수신 대기 중이거나 활성화된 차량 데이터가 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {vehicles.map((vehicle) => (
              <VehicleCard key={vehicle.vehicleId} vehicle={vehicle} />
            ))}
          </div>
        )}
      </ScrollArea>
    </div>
  );
}

function VehicleCard({ vehicle }: { vehicle: Vehicle }) {
  // 차량의 타입 파워트레인을 기반으로 타입 가드를 적용하여(배터리 vs 연료) 배지 동적으로 렌더링
  return (
    <Card className="vehicle-item transition-all hover:shadow-md border-slate-200">
      <CardHeader className="pb-2 flex flex-row items-center justify-between">
        <CardTitle className="text-lg font-semibold">
          {vehicle.vehicleId}
        </CardTitle>
        <Badge variant="outline" className="text-xs uppercase bg-slate-100">
          {vehicle.vehicleType}
        </Badge>
      </CardHeader>
      <CardContent>
        <div className="flex flex-col gap-3 mt-2">
          <div className="flex items-center justify-between">
            <span className="text-sm text-slate-500 font-medium">
              현재 속도
            </span>
            <span className="text-2xl font-bold tabular-nums">
              {vehicle.speed.toFixed(1)}{" "}
              <span className="text-sm text-slate-500 font-normal">km/h</span>
            </span>
          </div>

          <div className="border-t pt-3 flex items-center justify-between">
            {vehicle.powertrain === "EV" ? (
              <>
                <span className="text-sm text-slate-500 font-medium tracking-tight">
                  배터리 잔량
                </span>
                <Badge
                  variant="secondary"
                  className="bg-blue-100 text-blue-800 hover:bg-blue-200"
                >
                  ⚡ {vehicle.batteryLevel.toFixed(1)} %
                </Badge>
              </>
            ) : (
              <>
                <span className="text-sm text-slate-500 font-medium tracking-tight">
                  연료 잔량
                </span>
                <Badge
                  variant="secondary"
                  className="bg-orange-100 text-orange-800 hover:bg-orange-200"
                >
                  ⛽ {vehicle.fuelLevel.toFixed(1)} %
                </Badge>
              </>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
