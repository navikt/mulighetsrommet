import { ArrangorflateService } from "@mr/api-client-v2";

export async function hentArrangortilgangerForBruker() {
  return ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil();
}
