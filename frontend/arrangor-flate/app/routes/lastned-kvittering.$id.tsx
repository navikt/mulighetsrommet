import { LoaderFunction } from "@remix-run/node";
import { RefusjonskravService } from "@mr/api-client";
import { checkValidToken } from "../auth/auth.server";

export const loader: LoaderFunction = async ({ request, params }): Promise<Response> => {
  await checkValidToken(request);

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
