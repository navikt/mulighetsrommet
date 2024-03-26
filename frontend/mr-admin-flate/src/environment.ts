export enum Environment {
  PROD = "PROD",
  DEV = "DEV",
  DEMO = "DEMO",
  LOCAL = "LOCAL",
}

export const isProduction = window.location.origin.endsWith(".intern.nav.no");

export const isDevelopment = window.location.origin.endsWith(".intern.dev.nav.no");

export const isDemo = window.location.origin.endsWith(".ekstern.dev.nav.no");

export const environment: Environment = getEnvironment();

function getEnvironment() {
  if (isProduction) {
    return Environment.PROD;
  }

  if (isDevelopment) {
    return Environment.DEV;
  }

  if (isDemo) {
    return Environment.DEMO;
  }

  return Environment.LOCAL;
}
