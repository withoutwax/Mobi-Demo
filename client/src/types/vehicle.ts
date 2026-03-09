export type VehicleType = "REGULAR" | "FREIGHT" | "MICRO";
export type Powertrain = "EV" | "ICE";

/**
 * 모든 차량 모델이 공유하는 공통(Base) 인터페이스
 */
export interface BaseVehicle {
  logId?: number; // DB Insert 시점에 할당되는 PK
  vehicleId: string; // 예: REG-EV-001
  vehicleType: VehicleType; // 차종 분류
  latitude: number; // 위도
  longitude: number; // 경도
  speed: number; // 속도 (km/h)
  temperature: number; // 외부/내부 온도
  createdAt?: string; // 생성 시각 (ISO DateTime)
}

/**
 * 전기차(EV) 모델: batteryLevel 보유
 */
export interface EvVehicle extends BaseVehicle {
  powertrain: "EV";
  batteryLevel: number;
  fuelLevel?: null;
}

/**
 * 내연기관(ICE) 모델: fuelLevel 보유
 */
export interface IceVehicle extends BaseVehicle {
  powertrain: "ICE";
  batteryLevel?: null;
  fuelLevel: number;
}

/**
 * Discriminated Union 을 활용한 통합 Vehicle 타입
 * powertrain 속성('EV' | 'ICE')으로 런타임에 안전하게 분기할 수 있음
 */
export type Vehicle = EvVehicle | IceVehicle;
