"use client";

import { useState } from "react";
import { useVehicleStream } from "@/hooks/useVehicleStream";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Vehicle } from "@/types/vehicle";
import { VehicleDetailView } from "@/components/dashboard/VehicleDetailView";

export default function DashboardPage() {
  const { vehicles, isConnected, error } = useVehicleStream();
  const [selectedVehicleId, setSelectedVehicleId] = useState<string | null>(
    null,
  );

  // 현재 선택된 차량 객체를 리스트에서 찾음 (실시간 업데이트 반영)
  const selectedVehicle =
    vehicles.find((v) => v.vehicleId === selectedVehicleId) || null;

  return (
    <div className="container mx-auto p-4 max-w-7xl h-screen flex flex-col">
      <header className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Mobi 실시간 관제 대시보드
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time Vehicle Tracking with 3D Telemetry
          </p>
        </div>
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

      <div className="flex-1 border rounded-xl overflow-hidden shadow-sm bg-white min-h-[500px] flex">
        {/* Left Panel: Vehicle List */}
        <div className="w-1/3 min-w-[300px] max-w-[400px] bg-slate-50/50 border-r flex flex-col">
          <div className="p-4 h-full flex flex-col">
            <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-3">
              Active Vehicles ({vehicles.length})
            </h2>
            <ScrollArea className="flex-1 pr-3">
              {vehicles.length === 0 ? (
                <div className="flex items-center justify-center h-40 text-sm text-slate-500 text-center">
                  수신 대기 중이거나 <br /> 활성화된 차량 데이터가 없습니다.
                </div>
              ) : (
                <div className="flex flex-col gap-3 pb-8">
                  {vehicles.map((vehicle) => (
                    <VehicleListCard
                      key={vehicle.vehicleId}
                      vehicle={vehicle}
                      isSelected={selectedVehicleId === vehicle.vehicleId}
                      onClick={() => setSelectedVehicleId(vehicle.vehicleId)}
                    />
                  ))}
                </div>
              )}
            </ScrollArea>
          </div>
        </div>

        {/* Right Panel: Selected Vehicle Details (3D & Charts) */}
        <div className="flex-1 relative bg-white h-full p-6">
          {!selectedVehicle && (
            <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-400">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-16 w-16 mb-4 text-slate-200"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1}
                  d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"
                />
              </svg>
              <p className="font-medium text-lg text-slate-600">
                차량을 선택해주세요
              </p>
              <p className="text-sm mt-1 text-slate-400">
                좌측 리스트에서 상세 확인을 원하는 차량을 클릭하세요.
              </p>
            </div>
          )}
          {selectedVehicle && <VehicleDetailView vehicle={selectedVehicle} />}
        </div>
      </div>
    </div>
  );
}

function VehicleListCard({
  vehicle,
  isSelected,
  onClick,
}: {
  vehicle: Vehicle;
  isSelected: boolean;
  onClick: () => void;
}) {
  const isEv = vehicle.powertrain === "EV";

  return (
    <Card
      className={`cursor-pointer transition-all border-l-4 ${
        isSelected
          ? "border-l-blue-600 shadow-md bg-white border-y-blue-200 border-r-blue-200"
          : "border-l-transparent hover:border-l-slate-300 hover:shadow-sm"
      }`}
      onClick={onClick}
    >
      <CardHeader className="p-3 pb-1 flex flex-row items-center justify-between">
        <CardTitle className="text-sm font-bold">{vehicle.vehicleId}</CardTitle>
        <Badge
          variant={isSelected ? "default" : "secondary"}
          className="text-[10px] uppercase h-5 px-1.5 flex items-center justify-center"
        >
          {vehicle.powertrain}
        </Badge>
      </CardHeader>
      <CardContent className="p-3 pt-2">
        <div className="flex items-center justify-between text-xs text-slate-600">
          <span className="font-medium">{vehicle.speed.toFixed(1)} km/h</span>

          {isEv ? (
            <span className="text-blue-600 font-medium">
              ⚡ {vehicle.batteryLevel.toFixed(1)}%
            </span>
          ) : (
            <span className="text-orange-600 font-medium">
              ⛽ {vehicle.fuelLevel.toFixed(1)}%
            </span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
