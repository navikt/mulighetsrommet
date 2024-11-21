import {
  GetArrangorerData,
  type GetAvtalerData,
  GetEnheterData,
  GetTiltaksgjennomforingerData,
  NavAnsattRolle,
  NotificationStatus,
} from "@mr/api-client";

export const QueryKeys = {
  tiltakstype: (id?: string) => ["tiltakstype", id] as const,
  tiltakstyper: (filter?: object) => ["tiltakstyper", { ...filter }] as const,
  tiltakstypeFaneinnhold: (id: string) => ["tiltakstype", id, "faneinnhold"] as const,
  tiltaksgjennomforinger: (mine?: boolean, filter?: GetTiltaksgjennomforingerData) =>
    ["tiltaksgjennomforinger", mine, filter].filter((entry) => entry !== undefined),
  tiltaksgjennomforing: (id?: string) => ["tiltaksgjennomforing", id] as const,
  tiltaksgjennomforingHistorikk: (id?: string) =>
    ["tiltaksgjennomforing", id, "historikk"] as const,
  tiltaksgjennomforingDeltakerSummary(id: string) {
    return ["tiltaksgjennomforing", id, "deltaker-summary"] as const;
  },
  ansatt: () => ["ansatt"] as const,
  avtaler: (mine?: boolean, avtaleFilter?: GetAvtalerData) =>
    ["avtaler", mine, avtaleFilter] as const,
  avtale: (id: string) => ["avtale", id],
  avtaleHistorikk: (id?: string) => ["avtale", id, "historikk"] as const,
  enheter: (filter?: GetEnheterData) => ["enheter", filter],
  kostnadssted: (regioner?: string[]) => ["kostnadssted", regioner],
  arrangorer: (filter?: GetArrangorerData) => ["arrangorer", filter] as const,
  arrangorById: (id: string) => ["arrangor", id],
  arrangorHovedenhetById: (id: string) => ["arrangorHovedenhet", id],
  arrangorByOrgnr: (orgnr: string) => ["arrangor", { orgnr }],
  arrangorKontaktpersoner: (id: string) => ["arrangor", id, "kontaktpersoner"],
  arrangorKontaktpersonKoblinger: (id: string) => ["arrangorKoblinger", id],
  antallUlesteNotifikasjoner: () => ["antallUlesteNotifikasjoner"],
  notifikasjonerForAnsatt: (status: NotificationStatus) => ["notifikasjoner", status] as const,
  brregVirksomheter: (sokestreng: string) => ["virksomhet", "sok", sokestreng],
  sokSertifiseringer: (q: string) => ["sokSertifiseringer", "q", q],
  brregVirksomhetUnderenheter: (id: string) => ["virksomet", id, "underenheter"],
  navansatt: (rolle: NavAnsattRolle) => ["nav-ansatte", rolle],
  sokNavansatt: (q: string) => ["sok-nav-ansatte", q],
  navRegioner: () => ["navRegioner"],
  personopplysninger: () => ["personopplysninger"],
  opprettTilsagn: () => ["opprett-tilsagn"],
  getTilsagnForGjennomforing: (tiltaksgjennomforingId: string) => [
    "tilsagn",
    tiltaksgjennomforingId,
  ],
  getTilsagn: (id?: string) => ["tilsagn", id],
  besluttTilsagn: () => ["beslutt-tilsagn"],
  annullerTilsagn: () => ["annuller-tilsagn"],
  aftSatser: () => ["aftSatser"],
  utdanninger: () => ["utdanninger"],
};
