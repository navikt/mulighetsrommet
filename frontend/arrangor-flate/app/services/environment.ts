export enum Environment {
  DevGcp = "dev-gcp",
  ProdGcp = "prod-gcp",
  Lokalt = "lokalt",
  Demo = "demo",
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
  } else if (process.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
    return Environment.Demo;
  } else {
    return Environment.Lokalt;
  }
};

const getClientEnvironment = (window: Window) => {
  const { href } = window.location;

  if (href.includes("dev.nav.no")) {
    return Environment.DevGcp;
  } else if (href.includes("nav.no")) {
    return Environment.ProdGcp;
  } else if ((window as any)["isDemo"] === "true") {
    return Environment.Demo;
  } else {
    return Environment.Lokalt;
  }
};
