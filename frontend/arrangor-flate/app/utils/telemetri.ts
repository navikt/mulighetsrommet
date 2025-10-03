import {
  type Faro,
  getWebInstrumentations,
  initializeFaro,
  ReactIntegration,
} from "@grafana/faro-react";
import { TracingInstrumentation } from "@grafana/faro-web-tracing";
import { Environment, getEnvironment, isDemo } from "~/services/environment";

let faro: Faro | undefined;

/**
 * Singleton for Faro-instans.
 *
 * Faro brukes til instrumentering og telemetri og den typen ting i appen.
 */
export function getFaro(telemetryUrl: string): Faro | null {
  if (faro) {
    return faro;
  }
  const env = getEnvironment();
  if (env === Environment.Lokalt || isDemo()) {
    return null;
  }

  if (!telemetryUrl) {
    throw new Error("TELEMETRY_URL er ikke satt");
  }

  faro = initializeFaro({
    url: telemetryUrl,
    app: {
      name: "arrangor-flate",
      namespace: "team-mulighetsrommet",
      environment: env === Environment.ProdGcp ? "prod-gcp-loki" : "dev-gcp-loki",
    },
    instrumentations: [
      ...getWebInstrumentations({
        captureConsole: true,
      }),
      new TracingInstrumentation(),
      new ReactIntegration(),
    ],
  });
  return faro;
}
