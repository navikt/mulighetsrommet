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

type VELG_AVTALE_FILTER = {
  name: "tiltaksadministrasjon.velg-avtale-filter";
  data: {
    filter: string;
    value: any;
  };
};

type VELG_TILTAKSGJENNOMFORING_FILTER = {
  name: "tiltaksadministrasjon.velg-tiltaksgjennomforing-filter";
  data: {
    filter: string;
    value: any;
  };
};

export type Event =
  | KLIKK_FORSIDE_KORT
  | SETT_TILGJENGELIG_FOR_REDAKTOR
  | VELG_AVTALE_FILTER
  | VELG_TILTAKSGJENNOMFORING_FILTER;
