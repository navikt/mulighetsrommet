type DEL_MED_BRUKER_EVENT = {
  name: "arbeidsmarkedstiltak.del-med-bruker";
  data: {
    action: string;
    tiltakstype: string;
  };
};

type KLIKK_PA_FANE_EVENT = {
  name: "arbeidsmarkedstiltak.fanevalg";
  data: {
    faneValgt: string;
    tiltakstype: string;
  };
};

type UNIKE_BRUKERE = {
  name: "arbeidsmarkedstiltak.unike-brukere";
};

type HISTORIKK_EVENT = {
  name: "arbeidsmarkedstiltak.historikk";
  data: {
    action: string;
  };
};

type VIS_ANTALL_TILTAK_EVENT = {
  name: "arbeidsmarkedstiltak.vis-antall-tiltak";
  data: {
    valgt_antall: number;
    antall_tiltak: number;
  };
};

type LANDINGSSIDE_FANE_VALGT_EVENT = {
  name: "arbeidsmarkedstiltak.landingsside.fane-valgt";
  data: {
    action: string;
  };
};

type VIS_ALL_HISTORIKK = {
  name: "arbeidsmarkedstiltak.historikk.vis-all-historikk";
};

export type Event =
  | DEL_MED_BRUKER_EVENT
  | KLIKK_PA_FANE_EVENT
  | UNIKE_BRUKERE
  | HISTORIKK_EVENT
  | VIS_ANTALL_TILTAK_EVENT
  | LANDINGSSIDE_FANE_VALGT_EVENT
  | VIS_ALL_HISTORIKK;
