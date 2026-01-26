import { getWebInstrumentations, initializeFaro } from "@grafana/faro-react";

interface NaisConfig {
  telemetryCollectorURL: string;
  app: {
    name: string;
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
  if (window.faro && err instanceof Error) {
    window.faro.api.pushError(err);
  }
}
