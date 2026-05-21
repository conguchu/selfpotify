import { apiFetch } from "./client";
import type { RescanResult } from "@/lib/types";

export function rescanLibrary() {
  return apiFetch<RescanResult>("/api/config/scan/rescan", {
    method: "POST",
  });
}
