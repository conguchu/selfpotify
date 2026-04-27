"use client";

import { useEffect, useRef, useState } from "react";
import { Song, clearToken, getToken, listSongs, login, streamUrl } from "@/lib/api";

export default function Home() {
  const [authed, setAuthed] = useState(false);
  const [username, setUsername] = useState("user");
  const [password, setPassword] = useState("password");
  const [error, setError] = useState<string | null>(null);
  const [songs, setSongs] = useState<Song[]>([]);
  const [currentIdx, setCurrentIdx] = useState<number | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    if (getToken()) setAuthed(true);
  }, []);

  useEffect(() => {
    if (!authed) return;
    listSongs()
      .then(setSongs)
      .catch((e) => setError(String(e)));
  }, [authed]);

  async function onLogin(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await login(username, password);
      setAuthed(true);
    } catch (e) {
      setError(String(e));
    }
  }

  function onLogout() {
    clearToken();
    setAuthed(false);
    setSongs([]);
    setCurrentIdx(null);
  }

  function play(idx: number) {
    setCurrentIdx(idx);
    // wait for src update
    setTimeout(() => audioRef.current?.play().catch(() => {}), 0);
  }

  function next() {
    if (currentIdx === null) return;
    if (currentIdx < songs.length - 1) play(currentIdx + 1);
  }

  function prev() {
    if (currentIdx === null) return;
    if (currentIdx > 0) play(currentIdx - 1);
  }

  if (!authed) {
    return (
      <div className="container">
        <h1>Selfpotify Demo</h1>
        <form onSubmit={onLogin} style={{ display: "flex", flexDirection: "column", gap: 12, maxWidth: 320 }}>
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Usuario" />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Contraseña"
          />
          <button type="submit">Iniciar sesión</button>
          {error && <p style={{ color: "tomato" }}>{error}</p>}
        </form>
        <p style={{ color: "#888", fontSize: 12, marginTop: 16 }}>
          Por defecto: user / password
        </p>
      </div>
    );
  }

  const current = currentIdx !== null ? songs[currentIdx] : null;

  return (
    <div className="container">
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h1>Selfpotify Demo</h1>
        <button onClick={onLogout}>Salir</button>
      </header>

      {error && <p style={{ color: "tomato" }}>{error}</p>}
      {songs.length === 0 && <p>No hay canciones. Importa archivos en ~/Downloads y reinicia el servidor.</p>}

      <ul style={{ listStyle: "none", padding: 0 }}>
        {songs.map((s, i) => (
          <li
            key={s.id}
            className={`song-row ${i === currentIdx ? "active" : ""}`}
            onClick={() => play(i)}
          >
            <div>
              <div className="song-title">{s.title || `#${s.id}`}</div>
              <div className="song-meta">
                {s.genre || "—"} · {Math.round(s.duration_ms / 1000)}s
              </div>
            </div>
            <span style={{ color: "#1db954" }}>{i === currentIdx ? "▶" : ""}</span>
          </li>
        ))}
      </ul>

      {current && (
        <div className="player">
          <div style={{ marginBottom: 8 }}>
            <strong>{current.title || `#${current.id}`}</strong>
          </div>
          <audio
            ref={audioRef}
            src={streamUrl(current.id)}
            controls
            autoPlay
            onEnded={next}
          />
          <div className="controls">
            <button onClick={prev} disabled={currentIdx === 0}>⏮ Anterior</button>
            <button onClick={next} disabled={currentIdx === songs.length - 1}>Siguiente ⏭</button>
          </div>
        </div>
      )}
    </div>
  );
}
