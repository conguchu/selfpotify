import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { Toaster } from "sonner";
import { QueryProvider } from "@/lib/query/Provider";
import { SetupGate } from "@/components/layout/SetupGate";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Selfpotify",
  description: "Tu música, tu servidor.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="es"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full bg-bg text-text">
        <QueryProvider>
          <SetupGate>{children}</SetupGate>
          <Toaster
            theme="dark"
            position="bottom-right"
            toastOptions={{
              style: {
                background: "var(--color-bg-elevated)",
                color: "var(--color-text)",
                border: "1px solid var(--color-border)",
              },
            }}
          />
        </QueryProvider>
      </body>
    </html>
  );
}
