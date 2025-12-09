export enum Environment {
  PROD = "PROD",
  DEV = "DEV",
  LOCAL = "LOCAL",
}

export const isProduction = window.location.origin.endsWith(".intern.nav.no");

export const isDevelopment = window.location.origin.endsWith(".intern.dev.nav.no");

export const environment: Environment = getEnvironment();

function getEnvironment() {
  if (isProduction) {
    return Environment.PROD;
  }

  if (isDevelopment) {
    return Environment.DEV;
  }

  return Environment.LOCAL;
}

export function isAnsattDomene(): boolean {
  if (typeof window === "undefined") return false;

  const host = window.location.hostname;

  return host.endsWith(".ansatt.dev.nav.no");
}
