import { GjennomforingerService } from "@mr/api-client-v2";
import { ActionFunctionArgs } from "react-router";

export async function publiserAction({ request }: ActionFunctionArgs) {
  const formData = await request.formData();
  const id = String(formData.get("id"));
  const publisert = formData.get("publisert") === "true" ? true : false;

  await GjennomforingerService.setPublisert({
    path: { id },
    body: { publisert },
  });
}
