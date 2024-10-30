import { ArrangorflateService } from "@mr/api-client";

export async function hentArrangortilgangerForBruker() {
  return ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil();
}
