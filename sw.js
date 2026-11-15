// Guarda la app en el celular para que abra rápido y funcione sin internet.
const VERSION = "mis-finanzas-v5";
const ARCHIVOS = ["./", "./index.html", "./manifest.json", "./favicon.png", "./icono-192.png", "./icono-512.png", "./apple-touch-icon.png"];
// Las letras (fuentes) vienen de Google y casi nunca cambian.
const FUENTES = ["fonts.googleapis.com", "fonts.gstatic.com"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(VERSION).then((c) => c.addAll(ARCHIVOS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((ks) => Promise.all(ks.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  if (e.request.method !== "GET") return;
  const url = new URL(e.request.url);

  // VELOCIDAD: las letras primero se buscan en lo guardado (abre más rápido y se ven bien sin internet).
  if (FUENTES.includes(url.hostname)) {
    e.respondWith(
      caches.match(e.request).then((guardada) => guardada || fetch(e.request).then((r) => {
        if (r.ok || r.type === "opaque") {
          const copia = r.clone();
          caches.open(VERSION).then((c) => c.put(e.request, copia));
        }
        return r;
      }))
    );
    return;
  }

  // Lo demás: primero intenta internet (para tener lo más nuevo); si no hay, usa lo guardado.
  e.respondWith(
    fetch(e.request)
      .then((r) => {
        if (r.ok && url.origin === location.origin) {
          const copia = r.clone();
          caches.open(VERSION).then((c) => c.put(e.request, copia));
        }
        return r;
      })
      .catch(() => caches.match(e.request).then((r) => r || caches.match("./index.html")))
  );
});
