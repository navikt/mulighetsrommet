export enum Miljø {
  DevGcp = "dev-gcp",
  ProdGcp = "prod-gcp",
  Lokalt = "lokalt",
}

const erHosKlient = () => typeof document !== "undefined";

export const hentMiljø = () => {
  return erHosKlient() ? hentMiljøFraKlient(window) : hentMiljøFraServer();
};

const hentMiljøFraServer = () => {
  const clusterName = process.env.NAIS_CLUSTER_NAME;

  if (clusterName === Miljø.DevGcp) {
    return Miljø.DevGcp;
  } else if (clusterName === Miljø.ProdGcp) {
    return Miljø.ProdGcp;
  } else {
    return Miljø.Lokalt;
  }
};

const hentMiljøFraKlient = (window: Window) => {
  const { href } = window.location;

  if (href.includes("dev.nav.no")) {
    return Miljø.DevGcp;
  } else if (href.includes("nav.no")) {
    return Miljø.ProdGcp;
  } else {
    return Miljø.Lokalt;
  }
};
