import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#EFF8FE] px-4">
      <div className="text-center max-w-md">
        <h1 className="text-7xl font-bold text-[#0e74bc] mb-2">404</h1>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">
          Page Not Found
        </h2>
        <p className="text-gray-600 mb-6">
          The page you&apos;re looking for doesn&apos;t exist or has been moved.
        </p>
        <Link
          href="/"
          className="inline-block px-6 py-3 bg-[#0e74bc] text-white font-semibold rounded-lg hover:bg-[#0a5a94] transition-colors"
        >
          Back to Home
        </Link>
      </div>
    </div>
  );
}
