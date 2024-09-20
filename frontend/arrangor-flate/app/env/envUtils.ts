export type Envs = "local" | "dev-gcp" | "prod-gcp";

export function getEnv(): Envs {
  const env = process.env.NAIS_CLUSTER_NAME as Envs;
  switch (env) {
    case "local":
      return "local";
    case "dev-gcp":
      return "dev-gcp";
    case "prod-gcp":
      return "prod-gcp";
  }
}
