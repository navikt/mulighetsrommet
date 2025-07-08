import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { redirect } from "react-router";
import { ArrangorflateService } from "api-client";
import { apiHeaders } from "~/auth/auth.server";
import { pathByOrgnr } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for utbetalinger" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const { data: arrangorer, error } =
    await ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil({
      headers: await apiHeaders(request),
    });

  if (error || !arrangorer) {
    throw problemDetailResponse(error);
  }
  if (arrangorer.length === 0) {
    return redirect("/ingen-tilgang");
  }

  return redirect(`${pathByOrgnr(arrangorer[0].organisasjonsnummer).utbetalinger}`);
}
