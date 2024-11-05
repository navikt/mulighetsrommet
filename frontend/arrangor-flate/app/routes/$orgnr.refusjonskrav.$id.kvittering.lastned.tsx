import { LoaderFunction } from "@remix-run/node";
import { checkValidToken } from "../auth/auth.server";
import { ArrangorflateService } from "@mr/api-client";

export const loader: LoaderFunction = async ({ request, params }): Promise<Response> => {
  await checkValidToken(request);

  const { id, orgnr } = params;
  if (!id || !orgnr) throw Error("Mangler id eller orgnr");

  const kvittering = await ArrangorflateService.getRefusjonkravKvittering({
    id,
    orgnr,
  });

  return new Response(kvittering, {
    status: 200,
    headers: {
      contentType: "application/pdf",
    },
  });
};
