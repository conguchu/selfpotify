"use client";

import { useState } from "react";
import { UserPlus } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Modal } from "@/components/ui/Modal";
import { CreateUserForm } from "@/components/admin/CreateUserForm";
import { UserTable } from "@/components/admin/UserTable";

export default function AdminUsersPage() {
  const [createOpen, setCreateOpen] = useState(false);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Usuarios</h1>
          <p className="text-sm text-text-muted">
            Alta, baja y cambio de credenciales.
          </p>
        </div>
        <Button leftIcon={<UserPlus className="h-4 w-4" />} onClick={() => setCreateOpen(true)}>
          Nuevo usuario
        </Button>
      </div>

      <Card>
        <CardTitle className="mb-1">Usuarios del sistema</CardTitle>
        <CardDescription className="mb-4">
          Da de alta y baja, cambia contraseñas y alterna el rol de administrador
          con el interruptor. No es posible degradar al último administrador.
        </CardDescription>
        <UserTable />
      </Card>

      <Modal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Nuevo usuario"
        description="Crea un usuario o un administrador."
      >
        <CreateUserForm onCreated={() => setCreateOpen(false)} />
      </Modal>
    </div>
  );
}
