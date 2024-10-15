import { LoaderFunction } from "@remix-run/node";
import { requirePersonIdent } from "../auth/auth.server";
import { RefusjonskravService } from "@mr/api-client";

export const loader: LoaderFunction = async ({ request, params }): Promise<Response> => {
  await requirePersonIdent(request);

  if (params.id === undefined) throw Error("Mangler id");

  const kvittering = await RefusjonskravService.getRefusjonkravKvittering({
    id: params.id,
  });

  return new Response(kvittering, {
    status: 200,
    headers: {
      contentType: "application/pdf",
    },
  });
};
