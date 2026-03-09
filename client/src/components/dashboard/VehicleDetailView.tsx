import { Vehicle } from "@/types/vehicle";
import { Vehicle3DModel } from "./Vehicle3DModel";
import { VehicleRealtimeChart } from "./VehicleRealtimeChart";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";

interface VehicleDetailViewProps {
  vehicle: Vehicle | null;
}

export function VehicleDetailView({ vehicle }: VehicleDetailViewProps) {
  if (!vehicle) {
    return (
      <div className="w-full h-full flex flex-col items-center justify-center text-slate-400 bg-slate-50/50 rounded-xl border border-dashed border-slate-300">
        <svg
          xmlns="http://www.w3.org/.svg"
          className="h-12 w-12 mb-4 text-slate-300"
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
        <p className="font-medium text-lg text-slate-500">
          차량을 선택해주세요
        </p>
        <p className="text-sm mt-1">
          좌측 리스트에서 상세 확인을 원하는 차량을 클릭하세요.
        </p>
      </div>
    );
  }

  const isEv = vehicle.powertrain === "EV";

  return (
    <div className="w-full h-full flex flex-col gap-4 overflow-y-auto pr-2 pb-2">
      {/* 1. Header (ID & Badges) */}
      <div className="flex items-center justify-between pb-2">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-800 flex items-center gap-2">
            {vehicle.vehicleId}
            <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
          </h2>
          <p className="text-sm text-slate-500 mt-1">
            Live Telemetry Data Streaming
          </p>
        </div>
        <div className="flex gap-2">
          <Badge variant="outline" className="bg-white">
            {vehicle.vehicleType}
          </Badge>
          <Badge className={isEv ? "bg-blue-600" : "bg-orange-600"}>
            {vehicle.powertrain}
          </Badge>
        </div>
      </div>

      {/* 2. Top Section: 3D Visualization */}
      <div className="w-full h-[320px] rounded-xl overflow-hidden border bg-white shadow-sm relative">
        <Vehicle3DModel vehicle={vehicle} />
      </div>

      {/* 3. Middle Section: Key Metrics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <MetricCard title="Speed" value={`${vehicle.speed.toFixed(1)} km/h`} />

        {isEv ? (
          <MetricCard
            title="Battery Level"
            value={`${vehicle.batteryLevel.toFixed(1)} %`}
            valueColor="text-blue-600"
          />
        ) : (
          <MetricCard
            title="Fuel Level"
            value={`${vehicle.fuelLevel.toFixed(1)} %`}
            valueColor="text-orange-600"
          />
        )}

        <MetricCard
          title="Temperature"
          value={`${vehicle.temperature.toFixed(1)} °C`}
        />

        <MetricCard
          title="GPS Position"
          value={`${vehicle.latitude.toFixed(4)}, ${vehicle.longitude.toFixed(4)}`}
          small
        />
      </div>

      <Separator className="my-2" />

      {/* 4. Bottom Section: Real-time Chart */}
      <VehicleRealtimeChart vehicle={vehicle} />
    </div>
  );
}

function MetricCard({
  title,
  value,
  valueColor = "text-slate-900",
  small = false,
}: {
  title: string;
  value: string;
  valueColor?: string;
  small?: boolean;
}) {
  return (
    <Card className="shadow-sm border-slate-200 bg-white">
      <CardContent className="p-4 flex flex-col justify-center">
        <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">
          {title}
        </p>
        <p
          className={`font-bold mt-1 ${small ? "text-sm" : "text-xl"} ${valueColor}`}
        >
          {value}
        </p>
      </CardContent>
    </Card>
  );
}
