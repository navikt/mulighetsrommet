import { getWebInstrumentations, initializeFaro } from "@grafana/faro-react";

interface NaisConfig {
  telemetryCollectorURL: string;
  app: {
    name: string;
    version: string;
  };
}

declare global {
  interface Window {
    nais?: NaisConfig;
  }
}

export function initializeLogs() {
  const nais = window.nais;
  if (!nais) {
    console.warn("NAIS config not loaded");
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
  if (window.faro && err instanceof Error) {
    window.faro.api.pushError(err);
  }
}
