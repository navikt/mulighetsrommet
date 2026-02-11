import { getWebInstrumentations, initializeFaro, Faro } from "@grafana/faro-react";

interface NaisConfig {
  telemetryCollectorURL: string;
  app: {
    name: string;
  };
}

declare global {
  interface Window {
    nais?: NaisConfig;
    faro?: Faro;
  }
}

export function initializeLogs() {
  if (typeof window === "undefined") {
    return;
  }
  const nais = window.nais;
  if (!nais) {
    return;
  }
  if (window.faro) {
    return;
  }
  const hostname = window.location.hostname;
  initializeFaro({
    paused: hostname.includes("localhost") || hostname.includes("demo"),
    url: nais.telemetryCollectorURL,
    app: nais.app,
    instrumentations: [...getWebInstrumentations()],
  });
}

export function pushError(err: unknown) {
  // eslint-disable-next-line no-console
  console.error(err);
  if (typeof window === "undefined") {
    return;
  }

  if (window.faro) {
    if (err instanceof Error) {
      window.faro.api.pushError(err);
    } else {
      const message = typeof err === "string" ? err : JSON.stringify(err, null, 2);
      window.faro.api.pushError(new Error(message));
    }
  }
}
