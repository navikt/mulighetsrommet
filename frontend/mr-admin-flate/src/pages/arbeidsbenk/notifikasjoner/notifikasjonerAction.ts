import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { ActionFunctionArgs } from "react-router";
import { QueryKeys } from "../../../api/QueryKeys";

export const setLestStatusForNotifikasjonAction =
  (queryClient: QueryClient) =>
  async ({ request }: ActionFunctionArgs) => {
    const formData = await request.formData();
    const ids = formData.getAll("ids[]");
    const statuses = formData.getAll("statuses[]");

    if (ids.length === 0) {
      throw Error("Ingen notifikasjoner å oppdatere");
    }

    if (ids.length !== statuses.length) {
      throw Error("Antall IDer og statuser må være like");
    }

    const notifikasjoner = ids.map((id, index) => {
      const status = statuses[index];
      if (status !== NotificationStatus.DONE && status !== NotificationStatus.NOT_DONE) {
        throw Error("Ugyldig status for notifikasjon");
      }
      return { id: String(id), status: status as NotificationStatus };
    });

    await NotificationsService.setNotificationStatus({
      body: { notifikasjoner },
    });

    await queryClient.invalidateQueries({
      queryKey: ["notifikasjoner"],
      type: "all",
    });

    await queryClient.invalidateQueries({
      queryKey: QueryKeys.antallUlesteNotifikasjoner(),
      type: "all",
    });
  };
