import { LoaderFunction } from "@remix-run/node";
import { checkValidToken } from "../auth/auth.server";
import { ArrangorflateService } from "@mr/api-client";

export const loader: LoaderFunction = async ({ request, params }): Promise<Response> => {
  await checkValidToken(request);

  if (params.id === undefined) throw Error("Mangler id");

  const kvittering = await ArrangorflateService.getRefusjonkravKvittering({
    id: params.id,
  });

  return new Response(kvittering, {
    status: 200,
    headers: {
      contentType: "application/pdf",
    },
  });
};
