"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppLogo } from "@/components/layout/AppLogo";
import { Card } from "@/components/ui/Card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { LoginForm } from "@/components/auth/LoginForm";
import { RegisterForm } from "@/components/auth/RegisterForm";
import { useAuthStore } from "@/lib/auth/store";
import { useAppName } from "@/lib/query/hooks";

export default function LoginPage() {
  const router = useRouter();
  const appName = useAppName();
  const hydrated = useAuthStore((s) => s.hydrated);
  const token = useAuthStore((s) => s.token);
  const isAdmin = useAuthStore((s) => s.roles.includes("ROLE_ADMIN"));
  const [tab, setTab] = useState<"login" | "register">("login");

  useEffect(() => {
    if (!hydrated || !token) return;
    router.replace(isAdmin ? "/admin" : "/home");
  }, [hydrated, token, isAdmin, router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-bg via-bg to-accent-soft/30 p-4">
      <div className="w-full max-w-md">
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <AppLogo className="h-16 w-16" />
          <h1 className="text-3xl font-bold tracking-tight">{appName}</h1>
          <p className="text-sm text-text-muted">
            Tu música, en tu servidor.
          </p>
        </div>
        <Card className="p-6">
          <Tabs value={tab} onValueChange={(v) => setTab(v as "login" | "register")}>
            <TabsList className="self-stretch">
              <TabsTrigger value="login" className="flex-1">
                Iniciar sesión
              </TabsTrigger>
              <TabsTrigger value="register" className="flex-1">
                Crear cuenta
              </TabsTrigger>
            </TabsList>
            <TabsContent value="login">
              <LoginForm />
            </TabsContent>
            <TabsContent value="register">
              <RegisterForm onRegistered={() => setTab("login")} />
            </TabsContent>
          </Tabs>
        </Card>
      </div>
    </div>
  );
}
