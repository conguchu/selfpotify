"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Switch } from "@/components/ui/Switch";
import { useCreateUser } from "@/lib/query/hooks";

export function CreateUserForm({ onCreated }: { onCreated?: () => void }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isAdmin, setIsAdmin] = useState(false);
  const create = useCreateUser();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) {
      toast.error("Rellena usuario y contraseña");
      return;
    }
    try {
      await create.mutateAsync({
        username: username.trim(),
        password,
        isAdmin,
      });
      toast.success("Usuario creado");
      setUsername("");
      setPassword("");
      setIsAdmin(false);
      onCreated?.();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al crear");
    }
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cu-username">Usuario</Label>
        <Input
          id="cu-username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="nuevo_usuario"
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cu-password">Contraseña</Label>
        <Input
          id="cu-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Contraseña inicial"
        />
      </div>
      <div className="flex items-center justify-between rounded-md border border-border bg-bg-card px-3 py-2">
        <div>
          <p className="text-sm font-medium text-text">Administrador</p>
          <p className="text-xs text-text-muted">
            Tendrá acceso a este panel.
          </p>
        </div>
        <Switch
          ariaLabel="Crear como administrador"
          checked={isAdmin}
          onChange={setIsAdmin}
        />
      </div>
      <Button type="submit" loading={create.isPending} className="self-start">
        Crear usuario
      </Button>
    </form>
  );
}
