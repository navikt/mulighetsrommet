import { GjennomforingerService } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { ActionFunctionArgs } from "react-router";
import { QueryKeys } from "../../api/QueryKeys";

export const publiserAction =
  (queryClient: QueryClient) =>
  async ({ request }: ActionFunctionArgs) => {
    try {
      const formData = await request.formData();
      const id = String(formData.get("id"));
      const publisert = formData.get("publisert") === "true" ? true : false;

      await GjennomforingerService.setPublisert({
        path: { id },
        body: { publisert },
      });

      await queryClient.invalidateQueries({
        queryKey: QueryKeys.gjennomforing(id),
        refetchType: "all",
      });

      return { publisert };
    } catch (error) {
      return { error: !!error, message: "Det oppstod en feil ved publisering", publisert: false };
    }
  };
