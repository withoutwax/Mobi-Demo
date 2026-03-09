"use client";

import { useEffect, useState } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { Vehicle } from "@/types/vehicle";

interface VehicleRealtimeChartProps {
  vehicle: Vehicle;
}

// 차트 렌더링을 위해 가공된 히스토리 데이터 타입
interface ChartDataPoint {
  time: string;
  speed: number;
  energy: number; // EV면 batteryLevel, ICE면 fuelLevel
}

const MAX_DATA_POINTS = 20; // 20초(틱) 간의 데이터를 유지

export function VehicleRealtimeChart({ vehicle }: VehicleRealtimeChartProps) {
  const [data, setData] = useState<ChartDataPoint[]>([]);
  const isEv = vehicle.powertrain === "EV";
  const energyLabel = isEv ? "Battery (%)" : "Fuel (%)";
  const energyColor = isEv ? "#3b82f6" : "#f97316";

  // props로 넘어온 vehicle 데이터가 업데이트 될 때마다 배열에 누적
  useEffect(() => {
    const now = new Date();
    const timeString = `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}:${now.getSeconds().toString().padStart(2, "0")}`;

    // EV/ICE 타입 가드를 통해 에너지 레벨 추출
    const energyLevel =
      vehicle.powertrain === "EV" ? vehicle.batteryLevel : vehicle.fuelLevel;

    const newPoint: ChartDataPoint = {
      time: timeString,
      speed: Math.round(vehicle.speed),
      energy: Math.round(energyLevel),
    };

    const timer = setTimeout(() => {
      setData((prevData) => {
        const updatedData = [...prevData, newPoint];
        // 최대 데이터 포인트를 초과하면 큐(Queue)처럼 맨 앞 항목을 제거하여 Sliding Window 효과
        if (updatedData.length > MAX_DATA_POINTS) {
          return updatedData.slice(updatedData.length - MAX_DATA_POINTS);
        }
        return updatedData;
      });
    }, 0);

    return () => clearTimeout(timer);
  }, [vehicle]); // vehicle 객체 참조가 변경될 때마다(매 초마다 덮어씌워짐) 트리거

  return (
    <div className="w-full h-[250px] p-4 bg-white border rounded-lg shadow-sm">
      <h3 className="text-sm font-semibold mb-4 text-slate-700">
        Real-time Telemetry
      </h3>
      <div className="w-full h-[180px]">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={data}
            margin={{ top: 5, right: 10, left: -20, bottom: 0 }}
          >
            <CartesianGrid
              strokeDasharray="3 3"
              vertical={false}
              stroke="#e2e8f0"
            />
            <XAxis
              dataKey="time"
              tick={{ fontSize: 10, fill: "#64748b" }}
              tickMargin={10}
            />
            {/* 왼쪽 Y축: 속도 */}
            <YAxis
              yAxisId="left"
              domain={[0, 100]}
              tick={{ fontSize: 11, fill: "#64748b" }}
            />
            {/* 오른쪽 Y축: 에너지 */}
            <YAxis
              yAxisId="right"
              orientation="right"
              domain={[0, 100]}
              tick={{ fontSize: 11, fill: "#64748b" }}
            />
            <Tooltip
              contentStyle={{
                borderRadius: "8px",
                border: "none",
                boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
              }}
              labelStyle={{
                color: "#64748b",
                fontSize: "12px",
                marginBottom: "4px",
              }}
            />
            <Legend wrapperStyle={{ fontSize: "12px", paddingTop: "10px" }} />

            <Line
              yAxisId="left"
              type="monotone"
              name="Speed (km/h)"
              dataKey="speed"
              stroke="#0f172a"
              strokeWidth={2}
              dot={false}
              isAnimationActive={false} // 실시간 차트에서는 애니메이션을 끄는 것이 자연스러움
            />
            <Line
              yAxisId="right"
              type="monotone"
              name={energyLabel}
              dataKey="energy"
              stroke={energyColor}
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
