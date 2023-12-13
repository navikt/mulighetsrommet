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

export type Event = DEL_MED_BRUKER_EVENT | KLIKK_PA_FANE_EVENT | UNIKE_BRUKERE;
