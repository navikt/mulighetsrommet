import { LoaderFunction } from "react-router";
import { ArrangorflateService } from "api-client";
import { apiHeaders } from "~/auth/auth.server";

export const loader: LoaderFunction = async ({ request, params }): Promise<Response> => {
  const { id } = params;
  if (!id) throw Error("Mangler id");

  const { data: kvittering } = await ArrangorflateService.getUtbetalingsdetaljerPdf({
    path: { id },
    headers: await apiHeaders(request),
  });
  if (!kvittering) {
    return new Response("Generering av pdf feilet", {
      status: 500,
    });
  }

  const url = new URL(request.url);
  const filename = url.searchParams.get("filename") ?? "kvittering.pdf";

  return new Response(kvittering, {
    status: 200,
    headers: {
      contentType: "application/pdf",
      "Content-Disposition": `attachment; filename="${filename}"`,
    },
  });
};
