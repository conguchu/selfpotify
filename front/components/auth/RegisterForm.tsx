"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Switch } from "@/components/ui/Switch";
import { signup, signupAdmin } from "@/lib/api/auth";

export function RegisterForm({ onRegistered }: { onRegistered?: () => void }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [asAdmin, setAsAdmin] = useState(false);
  const [signupKey, setSignupKey] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      toast.error("Rellena usuario y contraseña");
      return;
    }
    if (asAdmin && !signupKey.trim()) {
      toast.error("Necesitas la clave de admin para registrarte como administrador");
      return;
    }
    setSubmitting(true);
    try {
      if (asAdmin) {
        await signupAdmin({
          username: username.trim(),
          password,
          signupKey: signupKey.trim(),
        });
      } else {
        await signup({ username: username.trim(), password });
      }
      toast.success("Cuenta creada. Ya puedes iniciar sesión.");
      setUsername("");
      setPassword("");
      setSignupKey("");
      onRegistered?.();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al registrar");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="reg-username">Usuario</Label>
        <Input
          id="reg-username"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="usuario"
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="reg-password">Contraseña</Label>
        <Input
          id="reg-password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="mínimo 6 caracteres"
        />
      </div>
      <div className="flex items-center justify-between rounded-md border border-border bg-bg-card px-3 py-2">
        <div>
          <p className="text-sm font-medium text-text">Soy administrador</p>
          <p className="text-xs text-text-muted">
            Necesita la clave especial del servidor.
          </p>
        </div>
        <Switch
          ariaLabel="Registrar como administrador"
          checked={asAdmin}
          onChange={setAsAdmin}
        />
      </div>
      {asAdmin ? (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="reg-signup-key">Clave de registro de admin</Label>
          <Input
            id="reg-signup-key"
            type="password"
            value={signupKey}
            onChange={(e) => setSignupKey(e.target.value)}
            placeholder="changeme-admin-key"
          />
          <p className="text-xs text-text-subtle">
            Definida en <code>app.admin.signup-key</code> del backend.
          </p>
        </div>
      ) : null}
      <Button type="submit" loading={submitting} size="lg" className="mt-2">
        Crear cuenta
      </Button>
    </form>
  );
}
