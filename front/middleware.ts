import { NextRequest, NextResponse } from "next/server";

/**
 * Detecta dispositivos móviles a partir del User-Agent. Es una heurística
 * suficiente para decidir entre la vista de escritorio y la pantalla que
 * invita a descargar la app móvil.
 */
function isMobileUserAgent(userAgent: string): boolean {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(
    userAgent,
  );
}

/**
 * Como las páginas no son responsive, redirigimos a `/mobile` (una pantalla que
 * invita a descargar la app) cuando se accede desde un móvil a cualquier ruta,
 * salvo `/playlist/share/*`, que tiene su propio manejo para Android en otra
 * rama. Desde escritorio, `/mobile` redirige a `/home`.
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isMobile = isMobileUserAgent(request.headers.get("user-agent") ?? "");

  if (pathname.startsWith("/mobile")) {
    if (!isMobile) {
      return NextResponse.redirect(new URL("/home", request.url));
    }
    return NextResponse.next();
  }

  if (isMobile && !pathname.startsWith("/playlist/share")) {
    return NextResponse.redirect(new URL("/mobile", request.url));
  }

  return NextResponse.next();
}

export const config = {
  // Excluimos los assets estáticos y las rutas internas de Next.js.
  matcher: ["/((?!_next/|assets/|favicon.ico|.*\\.[^/]+$).*)"],
};
