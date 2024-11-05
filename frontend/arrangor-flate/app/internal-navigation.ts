export const internalNavigation = (orgnr: string) => {
  return {
    root: "/",
    refusjonskravliste: `/${orgnr}/refusjonskrav`,
    beregning: (id: string) => `/${orgnr}/refusjonskrav/${id}/beregning`,
    bekreft: (id: string) => `/${orgnr}/refusjonskrav/${id}/bekreft`,
    kvittering: (id: string) => `/${orgnr}/refusjonskrav/${id}/kvittering`,
    tilsagn: (id: string) => `/${orgnr}/tilsagn/${id}`,
  };
};
