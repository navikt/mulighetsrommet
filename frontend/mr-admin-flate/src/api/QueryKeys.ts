import {
  GetArrangorerData,
  type GetAvtalerData,
  GetGjennomforingerData,
  LagretFilterType,
  NotificationStatus,
  Rolle,
} from "@mr/api-client-v2";

export const QueryKeys = {
  tiltakstype: (id?: string) => ["tiltakstype", id] as const,
  tiltakstyper: (filter?: object) => ["tiltakstyper", { ...filter }] as const,
  oppgaver: (filter?: object) => ["oppgaver", { ...filter }] as const,
  oppgavetyper: () => ["oppgaver", "oppgavetyper"] as const,
  tiltakstypeFaneinnhold: (id: string) => ["tiltakstype", id, "faneinnhold"] as const,
  gjennomforinger: (mine?: boolean, filter?: Pick<GetGjennomforingerData, "query">) =>
    ["gjennomforinger", mine, filter].filter((entry) => entry !== undefined),
  gjennomforing: (id?: string) => ["gjennomforing", id] as const,
  gjennomforingHistorikk: (id?: string) => ["gjennomforing", id, "historikk"] as const,
  gjennomforingDeltakerSummary(id: string) {
    return ["gjennomforing", id, "deltaker-summary"] as const;
  },
  ansatt: () => ["ansatt"] as const,
  avtaler: (mine?: boolean, avtaleFilter?: Pick<GetAvtalerData, "query">) =>
    ["avtaler", mine, avtaleFilter] as const,
  avtale: (id?: string) => ["avtale", id] as const,
  avtaleHistorikk: (id?: string) => ["avtale", id, "historikk"] as const,
  navEnheter: () => ["nav-enheter"],
  kostnadssted: (regioner?: string[]) => ["kostnadssted", regioner],
  arrangorer: (filter?: Pick<GetArrangorerData, "query">) => ["arrangorer", filter] as const,
  arrangorById: (id: string) => ["arrangor", id],
  arrangorHovedenhetById: (id: string) => ["arrangorHovedenhet", id],
  arrangorByOrgnr: (orgnr: string) => ["arrangor", { orgnr }],
  arrangorKontaktpersoner: (id: string) => ["arrangor", id, "kontaktpersoner"],
  arrangorKontaktpersonKoblinger: (id: string) => ["arrangorKoblinger", id],
  notifications: (status?: NotificationStatus) =>
    ["notifications", status].filter((part) => part !== undefined),
  notificationsSummary: () => ["notifications", "summary"],
  brregVirksomheter: (sokestreng: string) => ["virksomhet", "sok", sokestreng],
  sokSertifiseringer: (q: string) => ["sokSertifiseringer", "q", q],
  brregVirksomhetUnderenheter: (id: string) => ["virksomet", id, "underenheter"],
  navansatt: (rolle: Rolle) => ["nav-ansatte", rolle],
  sokNavansatt: (q: string, id: string) => ["sok-nav-ansatte", q, id],
  navRegioner: () => ["navRegioner"],
  personopplysninger: () => ["personopplysninger"],
  opprettTilsagn: () => ["opprett-tilsagn"],
  getTilsagnForGjennomforing: (gjennomforingId?: string) => ["tilsagn", gjennomforingId],
  getTilsagn: (id?: string) => ["tilsagn", id],
  besluttTilsagn: () => ["beslutt-tilsagn"],
  annullerTilsagn: () => ["annuller-tilsagn"],
  gjorOppTilsagn: () => ["gjor-opp-tilsagn"],
  slettTilsagn: () => ["slett-tilsagn"],
  avtalteSatser: (avtaleId: string) => ["satser", avtaleId],
  utdanninger: () => ["utdanninger"],
  lagredeFilter: (dokumenttype?: LagretFilterType) =>
    ["lagredeFilter", dokumenttype].filter((part) => part !== undefined),
  utbetalingerByGjennomforing: (gjennomforingId?: string) => [
    "utbetaling-for-gjennomforing",
    gjennomforingId,
  ],
  kontonummerArrangor: (orgnr: string) => ["kontonummer", orgnr],
};
