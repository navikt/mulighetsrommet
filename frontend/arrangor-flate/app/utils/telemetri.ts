import {
  type Faro,
  getWebInstrumentations,
  initializeFaro,
  ReactIntegration,
} from "@grafana/faro-react";
import { TracingInstrumentation } from "@grafana/faro-web-tracing";
import { Environment, getEnvironment } from "~/services/environment";

let faro: Faro | undefined;

/**
 * Singleton for Faro-instans.
 *
 * Faro brukes til instrumentering og telemetri og den typen ting i appen.
 */
export function getFaro(telemetriUrl: string): Faro | null {
  if (faro) {
    return faro;
  }
  if (!telemetriUrl) {
    throw new Error("Telemetri URL er ikke satt. Sett TELEMETRY_URL i .env");
  }
  const env = getEnvironment();

  if (env === Environment.Lokalt) {
    // eslint-disable-next-line no-console
    console.info("Faro er slått av for lokal utvikling");
    return null;
  }

  faro = initializeFaro({
    url: telemetriUrl,
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
