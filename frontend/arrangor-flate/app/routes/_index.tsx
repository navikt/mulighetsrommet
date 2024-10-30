import type { ActionFunctionArgs, LoaderFunctionArgs, MetaFunction } from "@remix-run/node";
import { redirect } from "@remix-run/node";
import { hentArrangortilgangerForBruker } from "../auth/arrangortilgang.server";
import { checkValidToken, setupOpenApi } from "../auth/auth.server";

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
  await checkValidToken(request);
  await setupOpenApi(request);
  const url = new URL(request.url);
  const { orgnr } = params;
  const arrangorer = await hentArrangortilgangerForBruker();

  if (!orgnr && arrangorer.length > 0 && url.pathname === "/") {
    return redirect(`/refusjonskrav/${arrangorer[0].organisasjonsnummer}`);
  }

  return null;
}
