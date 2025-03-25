import { ArrangorflateService } from "api-client";
import { LoaderFunction } from "react-router";
import { apiHeaders } from "~/auth/auth.server";

export const loader: LoaderFunction = async ({ request, params }) => {
  const { id } = params;

  if (!id) throw Error("Id ikke tilgjengelig");

  const { data, error } = await ArrangorflateService.synkroniserKontonummerForUtbetaling({
    path: { id },
    headers: await apiHeaders(request),
  });

  if (error) {
    throw error;
  }

  return data;
};
