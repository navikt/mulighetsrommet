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
  console.error(refineError(err));
}

function refineError(err: unknown): Error {
  if (err instanceof Error) {
    return err;
  }
  if (typeof err === "string") {
    return new Error(err);
  }
  return new Error(JSON.stringify(err, null, 2));
}
