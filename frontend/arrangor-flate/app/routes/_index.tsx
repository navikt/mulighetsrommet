import type { ActionFunctionArgs, LoaderFunctionArgs, MetaFunction } from "react-router";
import { redirect, redirectDocument } from "react-router";
import { hentArrangortilgangerForBruker } from "../auth/arrangortilgang.server";
import { internalNavigation } from "../internal-navigation";
import { getCurrentTab } from "../utils/currentTab";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export async function action({ request }: ActionFunctionArgs) {
  const formData = await request.formData();
  const orgnr = formData.get("orgnr");

  if (typeof orgnr === "string" && orgnr) {
    return redirect(`/${orgnr}`);
  }

  return null;
}

export async function loader({ request, params }: LoaderFunctionArgs) {
  const url = new URL(request.url);
  const currentTab = getCurrentTab(request);
  const { orgnr } = params;
  const { data: arrangorer } = await hentArrangortilgangerForBruker();

  if (!orgnr && arrangorer && arrangorer.length > 0 && url.pathname === "/") {
    return redirect(
      `${internalNavigation(arrangorer[0].organisasjonsnummer).refusjonskravliste}?forside-tab=${currentTab}`,
    );
  }

  throw redirectDocument("/oauth2/login");
}
