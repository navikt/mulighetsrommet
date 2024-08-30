import { fetchDecoratorHtml } from "@navikt/nav-dekoratoren-moduler/ssr";
import { hentMiljø, Miljø } from "../miljø";

const visDekoratørUnderUtvikling = true;
const brukSsrDekoratørIMiljø = true;

export type Dekoratørfragmenter = {
  styles: string;
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
  } catch (e) {
    console.log("Klarte ikke å hente dekoratør, appen vil mangle header og footer");
    return null;
  }
};

// TODO Vurdere å bytte ut denne med https://github.com/navikt/nav-dekoratoren-moduler?tab=readme-ov-file#injectdecoratorserverside--injectdecoratorserversidedom
export const hentDekoratør = async (miljø: Miljø): Promise<Dekoratørfragmenter> => {
  const decorator = await fetchDecoratorHtml({
    env: miljø === Miljø.ProdGcp ? "prod" : "dev",
    simple: false,
    chatbot: false,
    context: "samarbeidspartner",
    breadcrumbs: byggBrødsmulesti(miljø),
  });

  return {
    styles: decorator.DECORATOR_STYLES,
    header: decorator.DECORATOR_HEADER,
    footer: decorator.DECORATOR_FOOTER,
    scripts: decorator.DECORATOR_SCRIPTS,
  };
};

// TODO Bygg korrekt brødsmulesti
export const byggBrødsmulesti = (miljø: Miljø) => {
  if (miljø === Miljø.ProdGcp) {
    return [
      // {
      //   title: "Min side – arbeidsgiver",
      //   url: "https://arbeidsgiver.nav.no/min-side-arbeidsgiver/",
      // },
      // {
      //   title: "Kandidater til dine stillinger",
      //   url: "https://arbeidsgiver.nav.no/kandidatliste/",
      // },
    ];
  } else {
    return [
      // {
      //   title: "Min side – arbeidsgiver",
      //   url: "https://arbeidsgiver.intern.dev.nav.no/min-side-arbeidsgiver/",
      // },
      // {
      //   title: "Kandidater til dine stillinger",
      //   url: "https://presenterte-kandidater.intern.dev.nav.no/kandidatliste/",
      // },
    ];
  }
};
