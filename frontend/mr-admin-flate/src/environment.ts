export enum Environment {
  PROD = "PROD",
  DEV = "DEV",
  LOCAL = "LOCAL",
}

export function isProduction(): boolean {
  if (typeof window === "undefined") return false;
  const host = window.location.hostname;
  return !host.includes(".dev.");
}

export function isDevelopment(): boolean {
  return !isProduction();
}

export const environment: Environment = getEnvironment();

function getEnvironment() {
  if (isProduction()) {
    return Environment.PROD;
  }

  if (isDevelopment()) {
    return Environment.DEV;
  }

  return Environment.LOCAL;
}

export function isAnsattDomene(): boolean {
  if (typeof window === "undefined") return false;

  const host = window.location.hostname;
  return host.split(".")[1] === "ansatt";
}
