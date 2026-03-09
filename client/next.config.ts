import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Using direct CORS connection for SSE instead of Next.js rewrites to prevent connection dropping
  transpilePackages: [
    "three",
    "three-stdlib",
    "@react-three/fiber",
    "@react-three/drei",
  ],
};

export default nextConfig;
