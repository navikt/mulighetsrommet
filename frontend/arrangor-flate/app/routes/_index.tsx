import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { redirect } from "react-router";
import { internalNavigation } from "../internal-navigation";
import { getCurrentTab } from "../utils/currentTab";
import { ArrangorflateService } from "@mr/api-client-v2";
import { apiHeaders } from "~/auth/auth.server";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const currentTab = getCurrentTab(request);
  const { data: arrangorer } = await ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil(
    {
      headers: await apiHeaders(request),
    },
  );

  if (arrangorer.length === 0) {
    return redirect("/ingen-tilgang");
  }

  return redirect(
    `${internalNavigation(arrangorer[0].organisasjonsnummer).refusjonskravliste}?forside-tab=${currentTab}`,
  );
}
