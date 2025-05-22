import { fetchDecoratorHtml } from "@navikt/nav-dekoratoren-moduler/ssr";
import { Environment, getEnvironment } from "~/services/environment";

export type DekoratorElements = {
  head: string;
  header: string;
  footer: string;
  scripts: string;
};

export const fetchSsrDekorator = async (): Promise<DekoratorElements | null> => {
  const env = getEnvironment();

  try {
    return await fetchDekorator(env);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
  } catch (e) {
    // eslint-disable-next-line no-console
    console.log("Klarte ikke å hente dekoratør, appen vil mangle header og footer");
    return null;
  }
};

export const fetchDekorator = async (env: Environment): Promise<DekoratorElements> => {
  const decorator = await fetchDecoratorHtml({
    env: env === Environment.ProdGcp ? "prod" : "dev",
    params: {
      simple: false,
      simpleFooter: true,
      chatbot: false,
      context: "samarbeidspartner",
      shareScreen: false,
      breadcrumbs: [],
    },
  });

  return {
    head: decorator.DECORATOR_HEAD_ASSETS,
    header: decorator.DECORATOR_HEADER,
    footer: decorator.DECORATOR_FOOTER,
    scripts: decorator.DECORATOR_SCRIPTS,
  };
};
