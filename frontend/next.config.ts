import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  images: {
    remotePatterns: [
      {
        protocol: "http",
        hostname: "localhost",
        port: "80",
        pathname: "/**",
      },
      {
        protocol: "https",
        hostname: "*.tumago.co.zw",
        pathname: "/**",
      },
    ],
  },
  async rewrites() {
    const backendUrl = process.env.BACKEND_URL || "http://localhost:80";
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
