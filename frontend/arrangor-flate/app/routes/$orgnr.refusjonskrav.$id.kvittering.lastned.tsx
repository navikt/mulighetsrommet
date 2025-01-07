import { LoaderFunction } from "react-router";
import { ArrangorflateService } from "@mr/api-client-v2";

export const loader: LoaderFunction = async ({ params }): Promise<Response> => {
  const { id } = params;
  if (!id) {
    throw Error("Mangler id");
  }

  const { data: kvittering } = await ArrangorflateService.getRefusjonkravKvittering({
    path: { id },
  });
  if (!kvittering) {
    return new Response("Generering av pdf feilet", {
      status: 500,
    });
  }

  return new Response(kvittering, {
    status: 200,
    headers: {
      contentType: "application/pdf",
    },
  });
};
