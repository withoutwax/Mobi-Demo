// client/src/components/dashboard/Vehicle3DModel.tsx
"use client";

import { useRef } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Environment, OrbitControls, ContactShadows } from "@react-three/drei";
import * as THREE from "three";
import { Vehicle } from "@/types/vehicle";

interface Vehicle3DModelProps {
  vehicle: Vehicle;
}

// 심플한 Low-poly 자동차 모델
function CarModel({ speed, isEv }: { speed: number; isEv: boolean }) {
  const groupRef = useRef<THREE.Group>(null);
  const wheelsRef = useRef<THREE.Group[]>([]);

  // 자동차 바디 색상 분기 (EV는 사이버펑크 틱한 파란색 계열, ICE는 스탠다드 회색 계열)
  const bodyColor = isEv ? "#1e3a8a" : "#475569";

  // 속도에 비례해서 바퀴 회전
  useFrame((state, delta) => {
    // speed (km/h) -> rad/sec 대략적 변환
    const rotationSpeed = (speed / 10) * delta;
    wheelsRef.current.forEach((wheel) => {
      if (wheel) wheel.rotation.x += rotationSpeed;
    });
  });

  return (
    <group ref={groupRef} position={[0, 0.5, 0]}>
      {/* 차체 하단 (Chassis) */}
      <mesh position={[0, 0.2, 0]} castShadow>
        <boxGeometry args={[1.8, 0.4, 4]} />
        <meshStandardMaterial
          color={bodyColor}
          roughness={0.4}
          metalness={0.6}
        />
      </mesh>

      {/* 차체 상단 (Cabin) */}
      <mesh position={[0, 0.7, -0.2]} castShadow>
        <boxGeometry args={[1.4, 0.6, 2]} />
        <meshStandardMaterial
          color="#0f172a"
          roughness={0.1}
          metalness={0.8}
          opacity={0.8}
          transparent
        />
      </mesh>

      {/* 바퀴 4개 생성 */}
      {[-0.9, 0.9].map((x, i) =>
        [-1.2, 1.2].map((z, j) => (
          <group
            key={`${i}-${j}`}
            position={[x, 0, z]}
            ref={(el) => {
              if (el) wheelsRef.current.push(el);
            }}
          >
            <mesh rotation={[0, 0, Math.PI / 2]} castShadow>
              <cylinderGeometry args={[0.35, 0.35, 0.2, 16]} />
              <meshStandardMaterial color="#171717" roughness={0.9} />
            </mesh>
            {/* 휠(Rim) */}
            <mesh
              rotation={[0, 0, Math.PI / 2]}
              position={[x > 0 ? 0.11 : -0.11, 0, 0]}
            >
              <cylinderGeometry args={[0.2, 0.2, 0.05, 8]} />
              <meshStandardMaterial
                color="#cbd5e1"
                metalness={0.9}
                roughness={0.2}
              />
            </mesh>
          </group>
        )),
      )}
    </group>
  );
}

export function Vehicle3DModel({ vehicle }: Vehicle3DModelProps) {
  const isEv = vehicle.powertrain === "EV";

  return (
    <div className="w-full h-full min-h-[300px] bg-slate-900/5 rounded-lg overflow-hidden relative">
      <div className="absolute top-4 left-4 z-10">
        <h3 className="text-xl font-bold tracking-tight text-slate-800">
          Live 3D View
        </h3>
        <p className="text-sm text-slate-500 capitalize">
          {vehicle.vehicleType} Vehicle
        </p>
      </div>

      <Canvas shadows camera={{ position: [-4, 3, 5], fov: 40 }}>
        {/* 주변 환경광 및 조명 세팅 */}
        <ambientLight intensity={0.5} />
        <spotLight
          position={[10, 15, 10]}
          angle={0.3}
          penumbra={1}
          intensity={1}
          castShadow
        />
        <Environment preset="city" />

        {/* 자동차 모델 */}
        <CarModel speed={vehicle.speed} isEv={isEv} />

        {/* 바닥 그림자 */}
        <ContactShadows
          position={[0, -0.01, 0]}
          opacity={0.4}
          scale={10}
          blur={2}
          far={4}
        />

        {/* 마우스 컨트롤 (회전, 줌) */}
        <OrbitControls
          enablePan={false}
          minPolarAngle={Math.PI / 4}
          maxPolarAngle={Math.PI / 2 - 0.1}
          minDistance={3}
          maxDistance={10}
        />
      </Canvas>
    </div>
  );
}
