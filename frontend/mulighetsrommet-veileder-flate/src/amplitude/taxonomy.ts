type DEL_MED_BRUKER_EVENT = {
  name: "mulighetsrommet.del-med-bruker";
  data: {
    action: string;
    fylkeOgLokalkontor: {
      fylke: string;
      lokalkontor: string;
    };
  };
};

type FILTRERING_EVENT = {
  name: "mulighetsrommet.filtrering";
  data: Record<string, string>;
};

type SE_HISTORIKK_EVENT = {
  name: "mulighetsrommet.historikk";
};

type KOPIKNAPP_TILTAKSNUMMER = {
  name: "mulighetsrommet.kopiknapp";
};

type SORTERING_TILTAKSOVERSIKT = {
  name: "mulighetsrommet.sortering";
  data: Record<string, string>;
};

type KLIKK_PAA_REGELVERKLENKE = {
  name: "mulighetsrommet.regelverk";
};

type FANEVALG_DETALJVISNING = {
  name: "mulighetsrommet.faner";
  data: Record<string, string>;
};

type KLIKK_EPOSTLENKE_ARRANGOR = {
  name: "mulighetsrommet.arrangor.kontaktperson.epost";
};

type KLIKK_EPOST_TILTAKSANSVARLIG = {
  name: "mulighetsrommet.tiltaksansvarlig.epost";
};

type KLIKK_TEAMSLENKE_TILTAKSANSVARLIG = {
  name: "mulighetsrommet.tiltaksansvarlig.teamslenke";
};

type KLIKK_OPPRETT_AVTALE = {
  name: "mulighetsrommet.opprett-avtale";
  data: Record<string, string>;
};

export type AmplitudeEvent =
  | DEL_MED_BRUKER_EVENT
  | FILTRERING_EVENT
  | SE_HISTORIKK_EVENT
  | KOPIKNAPP_TILTAKSNUMMER
  | SORTERING_TILTAKSOVERSIKT
  | KLIKK_PAA_REGELVERKLENKE
  | FANEVALG_DETALJVISNING
  | KLIKK_EPOSTLENKE_ARRANGOR
  | KLIKK_EPOST_TILTAKSANSVARLIG
  | KLIKK_TEAMSLENKE_TILTAKSANSVARLIG
  | KLIKK_OPPRETT_AVTALE;
