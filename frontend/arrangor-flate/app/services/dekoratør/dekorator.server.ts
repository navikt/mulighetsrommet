import { fetchDecoratorHtml } from "@navikt/nav-dekoratoren-moduler/ssr";
import { hentMiljø, Miljø } from "../miljø";

const visDekoratørUnderUtvikling = true;
const brukSsrDekoratørIMiljø = true;

export type Dekoratørfragmenter = {
  head: string;
  header: string;
  footer: string;
  scripts: string;
};

export const hentSsrDekoratør = async (): Promise<Dekoratørfragmenter | null> => {
  const miljø = hentMiljø();

  try {
    if (miljø === Miljø.Lokalt) {
      if (visDekoratørUnderUtvikling) {
        return await hentDekoratør(miljø);
      } else {
        return null;
      }
    } else {
      if (brukSsrDekoratørIMiljø) {
        return await hentDekoratør(miljø);
      } else {
        return null;
      }
    }
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
  } catch (e) {
    // eslint-disable-next-line no-console
    console.log("Klarte ikke å hente dekoratør, appen vil mangle header og footer");
    return null;
  }
};

export const hentDekoratør = async (miljø: Miljø): Promise<Dekoratørfragmenter> => {
  const decorator = await fetchDecoratorHtml({
    env: miljø === Miljø.ProdGcp ? "prod" : "dev",
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
