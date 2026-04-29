"use client";

import { useState } from "react";
import { Trash2, Shield, User as UserIcon } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Spinner } from "@/components/ui/Spinner";
import { Switch } from "@/components/ui/Switch";
import { Tooltip } from "@/components/ui/Tooltip";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/Table";
import {
  useDeleteUser,
  useUpdateUserPassword,
  useUsers,
} from "@/lib/query/hooks";
import type { AdminUser } from "@/lib/types";
import { useAuthStore } from "@/lib/auth/store";

export function UserTable() {
  const usersQuery = useUsers();
  const currentUser = useAuthStore((s) => s.username);
  const deleteUser = useDeleteUser();
  const updatePassword = useUpdateUserPassword();
  const [confirmDelete, setConfirmDelete] = useState<AdminUser | null>(null);
  const [editPassword, setEditPassword] = useState<AdminUser | null>(null);
  const [newPassword, setNewPassword] = useState("");

  if (usersQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-10">
        <Spinner />
      </div>
    );
  }
  if (usersQuery.isError) {
    return (
      <p className="text-sm text-danger">
        Error al cargar usuarios:{" "}
        {usersQuery.error instanceof Error ? usersQuery.error.message : "?"}
      </p>
    );
  }
  const users = usersQuery.data ?? [];

  const onDelete = async () => {
    if (!confirmDelete) return;
    try {
      await deleteUser.mutateAsync(confirmDelete.id);
      toast.success("Usuario eliminado");
      setConfirmDelete(null);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al eliminar");
    }
  };

  const onChangePassword = async () => {
    if (!editPassword || !newPassword) {
      toast.error("Introduce una contraseña");
      return;
    }
    try {
      await updatePassword.mutateAsync({
        id: editPassword.id,
        password: newPassword,
      });
      toast.success("Contraseña actualizada");
      setEditPassword(null);
      setNewPassword("");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al actualizar");
    }
  };

  return (
    <>
      <Table>
        <THead>
          <TR>
            <TH>ID</TH>
            <TH>Usuario</TH>
            <TH>Rol</TH>
            <TH className="w-44 text-right">Acciones</TH>
          </TR>
        </THead>
        <TBody>
          {users.map((u) => {
            const isAdmin = u.type === "ADMIN";
            const isMe = u.username === currentUser;
            return (
              <TR key={u.id}>
                <TD className="text-text-muted tabular-nums">{u.id}</TD>
                <TD className="font-medium">
                  <span className="inline-flex items-center gap-2">
                    {isAdmin ? (
                      <Shield className="h-4 w-4 text-accent" />
                    ) : (
                      <UserIcon className="h-4 w-4 text-text-muted" />
                    )}
                    {u.username}
                    {isMe ? (
                      <Badge variant="accent" className="ml-1">
                        Tú
                      </Badge>
                    ) : null}
                  </span>
                </TD>
                <TD>
                  <div className="flex items-center gap-3">
                    <Tooltip
                      label="El cambio de rol requiere borrar y recrear (limitación del backend actual)"
                      side="right"
                    >
                      <Switch
                        ariaLabel={`Rol admin para ${u.username}`}
                        checked={isAdmin}
                        onChange={() => {
                          /* deshabilitado */
                        }}
                        disabled
                      />
                    </Tooltip>
                    <Badge variant={isAdmin ? "accent" : "neutral"}>
                      {isAdmin ? "Admin" : "Usuario"}
                    </Badge>
                  </div>
                </TD>
                <TD className="text-right">
                  <div className="inline-flex items-center gap-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        setEditPassword(u);
                        setNewPassword("");
                      }}
                    >
                      Cambiar pass
                    </Button>
                    <IconButton
                      label="Eliminar"
                      variant="ghost"
                      size="sm"
                      onClick={() => setConfirmDelete(u)}
                      disabled={isMe}
                    >
                      <Trash2 />
                    </IconButton>
                  </div>
                </TD>
              </TR>
            );
          })}
        </TBody>
      </Table>

      <Modal
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        title="Eliminar usuario"
        description={
          confirmDelete
            ? `Vas a borrar al usuario "${confirmDelete.username}". Esta acción no se puede deshacer.`
            : ""
        }
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmDelete(null)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={deleteUser.isPending}
              onClick={onDelete}
            >
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-text-muted">
          Las playlists creadas por este usuario podrían quedar huérfanas.
        </p>
      </Modal>

      <Modal
        open={!!editPassword}
        onClose={() => setEditPassword(null)}
        title="Cambiar contraseña"
        description={
          editPassword ? `Nueva contraseña para "${editPassword.username}"` : ""
        }
        footer={
          <>
            <Button variant="ghost" onClick={() => setEditPassword(null)}>
              Cancelar
            </Button>
            <Button
              loading={updatePassword.isPending}
              onClick={onChangePassword}
            >
              Guardar
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="edit-pwd">Contraseña</Label>
          <Input
            id="edit-pwd"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoFocus
          />
        </div>
      </Modal>
    </>
  );
}
