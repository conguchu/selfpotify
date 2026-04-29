import { apiFetch } from "./client";
import type {
  AdminSignupPayload,
  JwtResponse,
  LoginPayload,
  SignupPayload,
} from "@/lib/types";

export function login(payload: LoginPayload) {
  return apiFetch<JwtResponse>("/api/auth/login", {
    method: "POST",
    body: payload,
  });
}

export function signup(payload: SignupPayload) {
  return apiFetch<string>("/api/auth/signup", {
    method: "POST",
    body: payload,
    parse: "text",
  });
}

export function signupAdmin(payload: AdminSignupPayload) {
  const { signupKey, ...body } = payload;
  return apiFetch<string>("/api/auth/signup-admin", {
    method: "POST",
    body,
    extraHeaders: { "X-Admin-Signup-Key": signupKey },
    parse: "text",
  });
}
