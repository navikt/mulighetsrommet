import { NotificationType, PaginertUserNotifications } from "@mr/api-client";
import { mockAvtaler } from "./mock_avtaler";
import { formaterDato } from "../../utils/Utils";

const mockAvtale = mockAvtaler.at(0);

export const mockNotifikasjoner: PaginertUserNotifications = {
  pagination: {
    totalCount: 2,
    pageSize: 50,
    totalPages: 1,
  },
  data: [
    {
      id: "d65c2be1-980d-4782-a51f-0ad9b52c3fda",
      type: NotificationType.NOTIFICATION,
      title: `Avtalen ${mockAvtale?.navn} utløper den ${mockAvtale?.sluttDato ? formaterDato(mockAvtale.sluttDato) : "-"}`,
      description:
        "Beskrivelse med en ganske lang tekst fordi vi har så komplekse prosesser så det viktig at det blir gitt nøyaktig beskjed om hva som skal til for å løse denne vanskelige problemstillingen.",
      user: "B123456",
      createdAt: "2023-01-26T13:51:50.417-07:00",
      doneAt: "2023-01-26T13:51:50.417-07:00",
      metadata: {
        link: `/avtaler/${mockAvtale?.id}`,
        linkText: "Gå til tiltaksgjennomføring",
      },
    },
    {
      id: "eccd9a93-3f08-4006-b3cb-751762e8bccf",
      type: NotificationType.NOTIFICATION,
      title: "Avtalen Oppfølging, tjenesteområde E - Moss utløper den 31.07.2023",
      description: "Beskrivelsen her er ikke så lang som den forrige, men sånn er det av og til.",
      user: "B123456",
      createdAt: "2023-01-26T13:51:50.417-07:00",
      doneAt: "2023-01-26T13:51:50.417-07:00",
    },
    {
      id: "eccd9a93-3f08-4006-b3cb-751762e8bcce",
      type: NotificationType.TASK,
      title: "Du må fikse noe greier",
      description:
        "Dette er en 'TASK' som betyr at du må få den late rumpen din opp av stolen og gjøre noe fornuftig",
      user: "B123456",
      createdAt: "2023-01-26T13:51:50.417-07:00",
      doneAt: "2023-01-26T13:51:50.417-07:00",
    },
  ],
};
