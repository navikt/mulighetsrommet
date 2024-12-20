import { LoaderFunction } from "react-router";
import { checkValidToken } from "../auth/auth.server";
import { ArrangorflateService } from "@mr/api-client";

export const loader: LoaderFunction = async ({ request, params }): Promise<Response> => {
  await checkValidToken(request);

  const { id } = params;
  if (!id) {
    throw Error("Mangler id");
  }

  const kvittering = await ArrangorflateService.getRefusjonkravKvittering({ id });

  return new Response(kvittering, {
    status: 200,
    headers: {
      contentType: "application/pdf",
    },
  });
};
