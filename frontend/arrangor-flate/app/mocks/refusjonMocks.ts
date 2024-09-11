import { Refusjonskrav, RefusjonskravStatus } from "@mr/api-client";
import { http, HttpResponse, PathParams } from "msw";

const mockKrav: Refusjonskrav[] = [
  {
    id: "6",
    belop: "308 530",
    fristForGodkjenning: "31.08.2024",
    kravnr: "6",
    periode: "01.06.2024 - 30.06.2024",
    status: RefusjonskravStatus.KLAR_FOR_INNSENDING,
    tiltaksnr: "2024/123456",
  },
  {
    id: "5",
    belop: "123 000",
    fristForGodkjenning: "31.07.2024",
    kravnr: "5",
    periode: "01.05.2024 - 31.05.2024",
    status: RefusjonskravStatus.NARMER_SEG_FRIST,
    tiltaksnr: "2024/123456",
  },
  {
    id: "4",
    belop: "85 000",
    fristForGodkjenning: "30.06.2024",
    kravnr: "4",
    periode: "01.01.2024 - 31.01.2024",
    status: RefusjonskravStatus.ATTESTERT,
    tiltaksnr: "2024/123456",
  },
];

export const refusjonHandlers = [
  http.get<PathParams, Refusjonskrav[]>("*/api/v1/intern/refusjon/:orgnr/krav", () =>
    HttpResponse.json(mockKrav),
  ),
];
