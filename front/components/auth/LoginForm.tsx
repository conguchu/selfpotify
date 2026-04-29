"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { login } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/auth/store";

export function LoginForm() {
  const router = useRouter();
  const loginWith = useAuthStore((s) => s.loginWith);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      toast.error("Rellena usuario y contraseña");
      return;
    }
    setSubmitting(true);
    try {
      const jwt = await login({ username: username.trim(), password });
      loginWith(jwt);
      toast.success(`Hola, ${jwt.username}`);
      const goAdmin = jwt.roles.includes("ROLE_ADMIN");
      router.replace(goAdmin ? "/admin" : "/home");
    } catch (err) {
      toast.error(
        err instanceof Error ? "Credenciales incorrectas" : "Error al iniciar sesión",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="login-username">Usuario</Label>
        <Input
          id="login-username"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="user"
          autoFocus
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="login-password">Contraseña</Label>
        <Input
          id="login-password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
        />
      </div>
      <Button type="submit" loading={submitting} size="lg" className="mt-2">
        Iniciar sesión
      </Button>
    </form>
  );
}
