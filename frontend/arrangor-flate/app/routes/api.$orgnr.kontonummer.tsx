import { ArrangorflateService } from "api-client";
import { LoaderFunction } from "react-router";
import { apiHeaders } from "~/auth/auth.server";

export const loader: LoaderFunction = async ({ request, params }) => {
  const { orgnr } = params;

  if (!orgnr) throw Error("Orgnr ikke tilgjengelig");

  const { data, error } = await ArrangorflateService.getKontonummer({
    path: { orgnr },
    headers: await apiHeaders(request),
  });

  if (error) {
    throw error;
  }

  return data;
};
