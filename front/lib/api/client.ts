import { useAuthStore } from "@/lib/auth/store";

export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  body: unknown;
  constructor(status: number, body: unknown, message?: string) {
    super(message ?? `Request failed with status ${status}`);
    this.status = status;
    this.body = body;
  }
}

interface ApiFetchOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  extraHeaders?: Record<string, string>;
  parse?: "json" | "text" | "none";
}

export async function apiFetch<T>(
  path: string,
  opts: ApiFetchOptions = {},
): Promise<T> {
  const { body, extraHeaders, parse = "json", headers, ...rest } = opts;
  const token = useAuthStore.getState().token;

  const finalHeaders: Record<string, string> = {
    Accept: "application/json",
    ...(extraHeaders ?? {}),
    ...((headers as Record<string, string>) ?? {}),
  };
  if (token) {
    finalHeaders.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders["Content-Type"] = "application/json";
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: finalHeaders,
    body:
      body === undefined
        ? undefined
        : body instanceof FormData
          ? body
          : JSON.stringify(body),
  });

  if (res.status === 401 || res.status === 403) {
    if (typeof window !== "undefined" && useAuthStore.getState().token) {
      useAuthStore.getState().logout();
    }
  }

  if (!res.ok) {
    let errorBody: unknown = null;
    try {
      const text = await res.text();
      try {
        errorBody = text ? JSON.parse(text) : null;
      } catch {
        errorBody = text;
      }
    } catch {
      errorBody = null;
    }
    const msg =
      typeof errorBody === "string"
        ? errorBody
        : errorBody && typeof errorBody === "object" && "message" in errorBody
          ? String((errorBody as { message: unknown }).message)
          : `HTTP ${res.status}`;
    throw new ApiError(res.status, errorBody, msg);
  }

  if (parse === "none" || res.status === 204) {
    return undefined as T;
  }
  if (parse === "text") {
    return (await res.text()) as T;
  }
  const text = await res.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as T;
  }
}
