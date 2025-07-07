export enum Environment {
  DevGcp = "dev-gcp",
  ProdGcp = "prod-gcp",
  Lokalt = "lokalt",
}

const erHosKlient = () => typeof document !== "undefined";

export const getEnvironment = () => {
  return erHosKlient() ? getClientEnvironment(window) : getServerEnvironment();
};

const getServerEnvironment = () => {
  const clusterName = process.env.NAIS_CLUSTER_NAME;

  if (clusterName === Environment.DevGcp) {
    return Environment.DevGcp;
  } else if (clusterName === Environment.ProdGcp) {
    return Environment.ProdGcp;
  } else {
    return Environment.Lokalt;
  }
};

/**
 * Differansiert fra Environment mtp hvilken modus appen kjører i
 * Demo modus: mocked ressurser
 */
export const isDemo = () =>
  erHosKlient()
    ? (window as any)["isDemo"] === true
    : process.env.VITE_MULIGHETSROMMET_API_MOCK === "true";

const getClientEnvironment = (window: Window) => {
  const { href } = window.location;

  if (href.includes("dev.nav.no")) {
    return Environment.DevGcp;
  } else if (href.includes("nav.no")) {
    return Environment.ProdGcp;
  } else {
    return Environment.Lokalt;
  }
};
