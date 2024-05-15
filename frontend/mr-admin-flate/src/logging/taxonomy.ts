type KLIKK_FORSIDE_KORT = {
  name: "tiltaksadministrasjon.klikk-forsidekort";
  data: {
    forsidekort: string;
  };
};

type SETT_TILGJENGELIG_FOR_REDAKTOR = {
  name: "tiltaksadministrasjon.sett-tilgjengelig-for-redaktor";
  data: {
    datoValgt: string;
  };
};

export type Event = KLIKK_FORSIDE_KORT | SETT_TILGJENGELIG_FOR_REDAKTOR;
