import { LagretDokumenttype, LagretFilterService } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { ActionFunctionArgs } from "react-router";
import { QueryKeys } from "../QueryKeys";

export const lagreFilterAction =
  (queryClient: QueryClient) =>
  async ({ request }: ActionFunctionArgs) => {
    const payload = await request.formData();
    const id = String(payload.get("id"));
    const navn = String(payload.get("navn"));
    const type = String(payload.get("type")) as LagretDokumenttype;
    const filter = String(payload.get("filter"));
    const sortOrder = Number(payload.get("sortOrder"));

    if (!navn) {
      throw new Error("Navn må være satt");
    }

    if (!type) {
      throw new Error("Type må være satt");
    }

    if (!filter) {
      throw new Error("Filter må være satt");
    }

    if (isNaN(sortOrder)) {
      throw new Error("SortOrder må være et gyldig tall");
    }
    const body = { id: id ?? null, navn, type, filter: JSON.parse(filter), sortOrder };

    await LagretFilterService.upsertFilter({ body });
    await queryClient.invalidateQueries({
      queryKey: QueryKeys.lagredeFilter(type),
      type: "all",
    });
  };
