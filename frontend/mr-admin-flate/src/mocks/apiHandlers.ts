import { rest } from "msw";
import {
  Avtale,
  AvtaleNokkeltall,
  AvtaleRequest,
  NavAnsatt,
  NavEnhet,
  PaginertAvtale,
  PaginertTiltaksgjennomforing,
  PaginertTiltakstype,
  PaginertUserNotifications,
  SletteAvtale,
  Tiltaksgjennomforing,
  TiltaksgjennomforingNokkeltall,
  Tiltakstype,
  TiltakstypeNokkeltall,
  UserNotificationSummary,
  Utkast,
  Virksomhet,
} from "mulighetsrommet-api-client";
import { mockBetabruker, mockKontaktpersoner } from "./fixtures/mock_ansatt";
import { mockAvtaleNokkeltall } from "./fixtures/mock_avtale_nokkeltall";
import { mockAvtaler } from "./fixtures/mock_avtaler";
import { mockEnheter } from "./fixtures/mock_enheter";
import { mockNotifikasjoner } from "./fixtures/mock_notifikasjoner";
import { mockTiltaksgjennomforinger } from "./fixtures/mock_tiltaksgjennomforinger";
import { mockTiltaksgjennomforingerNokkeltall } from "./fixtures/mock_tiltaksgjennomforinger_nokkeltall";
import { mockTiltakstyper } from "./fixtures/mock_tiltakstyper";
import { mockTiltakstyperNokkeltall } from "./fixtures/mock_tiltakstyper_nokkeltall";
import { mockUserNotificationSummary } from "./fixtures/mock_userNotificationSummary";
import { mockUtkast } from "./fixtures/mock_utkast";
import { mockVirksomheter } from "./fixtures/mock_virksomheter";

export const apiHandlers = [
  rest.get<any, any, PaginertTiltakstype>(
    "*/api/v1/internal/tiltakstyper",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltakstyper));
    }
  ),

  rest.get<any, { id: string }, Tiltakstype | undefined>(
    "*/api/v1/internal/tiltakstyper/:id",
    (req, res, ctx) => {
      const { id } = req.params;
      return res(
        ctx.status(200),

        ctx.json(mockTiltakstyper.data.find((gj) => gj.id === id))
      );
    }
  ),
  rest.get<any, { id: string }, TiltakstypeNokkeltall | undefined>(
    "*/api/v1/internal/tiltakstyper/:id/nokkeltall",
    (req, res, ctx) => {
      return res(
        ctx.status(200),

        ctx.json(mockTiltakstyperNokkeltall)
      );
    }
  ),

  rest.get<any, { id: string }, PaginertAvtale>(
    "*/api/v1/internal/avtaler/tiltakstype/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };
      const avtaler =
        mockAvtaler.data.filter((a) => a.tiltakstype.id === id) ?? [];
      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            currentPage: 1,
            pageSize: 50,
            totalCount: avtaler.length,
          },
          data: avtaler,
        })
      );
    }
  ),

  rest.delete<SletteAvtale>("/api/v1/internal/avtaler/:id", (req, res, ctx) => {
    const responsErOk = Math.random() > 0.5;
    if (responsErOk) {
      return res(
        ctx.status(200),
        ctx.json<SletteAvtale>({
          statusCode: 200,
          message: "Avtalen ble slettet",
        })
      );
    }

    const responses = [
      "Avtalen er mellom start- og sluttdato og må avsluttes før den kan slettes.",
      "Avtalen har 3 tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan slette avtalen.",
    ];
    const randomIndex = Math.floor(Math.random() * responses.length);

    return res(
      ctx.status(200),
      ctx.json<SletteAvtale>({
        statusCode: 400,
        message: responses[randomIndex],
      })
    );
  }),

  rest.get<any, any, NavEnhet[]>(
    "*/api/v1/internal/enheter",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockEnheter));
    }
  ),

  rest.get<any, any, PaginertAvtale | undefined>(
    "*/api/v1/internal/avtaler",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockAvtaler));
    }
  ),

  rest.get<any, any, Avtale | undefined>(
    "*/api/v1/internal/avtaler/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };
      const avtale = mockAvtaler.data.find((a) => a.id === id) ?? undefined;
      return res(ctx.status(200), ctx.json(avtale));
    }
  ),

  rest.get<any, any, AvtaleNokkeltall | undefined>(
    "*/api/v1/internal/avtaler/:id/nokkeltall",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockAvtaleNokkeltall));
    }
  ),

  rest.get<any, any, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger));
    }
  ),

  rest.put<any, any, Tiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger.data[0]));
    }
  ),

  rest.get<any, any, Tiltaksgjennomforing[]>(
    "*/api/v1/internal/tiltaksgjennomforinger/sok",
    (req, res, ctx) => {
      const tiltaksnummer = req.url.searchParams.get("tiltaksnummer");

      if (!tiltaksnummer) {
        throw new Error("Tiltaksnummer er ikke satt som query-param");
      }

      const gjennomforing = mockTiltaksgjennomforinger.data.filter((tg) =>
        tg.tiltaksnummer.toString().includes(tiltaksnummer)
      );

      return res(ctx.status(200), ctx.json(gjennomforing));
    }
  ),
  rest.get<any, { id: string }, TiltaksgjennomforingNokkeltall | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/:id/nokkeltall",
    (req, res, ctx) => {
      return res(
        ctx.status(200),

        ctx.json(mockTiltaksgjennomforingerNokkeltall)
      );
    }
  ),

  rest.get<any, { id: string }, Tiltaksgjennomforing | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/:id",
    (req, res, ctx) => {
      const { id } = req.params;

      const gjennomforing = mockTiltaksgjennomforinger.data.find(
        (gj) => gj.id === id
      );
      if (!gjennomforing) {
        return res(ctx.status(404), ctx.json(undefined));
      }

      return res(ctx.status(200), ctx.json(gjennomforing));
    }
  ),

  rest.get<any, { id: string }, PaginertTiltaksgjennomforing | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/tiltakstype/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };

      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakstype.id === id
      );
      if (!gjennomforinger) {
        return res(ctx.status(404), ctx.json(undefined));
      }

      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            totalCount: gjennomforinger.length,
            currentPage: 1,
            pageSize: 50,
          },
          data: gjennomforinger,
        })
      );
    }
  ),

  rest.get<any, { tiltakskode: string }, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger/tiltakskode/:tiltakskode",
    (req, res, ctx) => {
      const { tiltakskode } = req.params;
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakstype.arenaKode === tiltakskode
      );
      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            totalCount: gjennomforinger.length,
            currentPage: 1,
            pageSize: 50,
          },
          data: gjennomforinger,
        })
      );
    }
  ),

  rest.get<any, { enhet: string }, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger/enhet/:enhet",
    (req, res, ctx) => {
      const { enhet } = req.params;
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.arenaAnsvarligEnhet === enhet
      );
      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            totalCount: gjennomforinger.length,
            currentPage: 1,
            pageSize: 50,
          },
          data: gjennomforinger,
        })
      );
    }
  ),

  rest.get<any, any, NavAnsatt[]>(
    "*/api/v1/internal/ansatt",
    (req, res, ctx) => {
      const roller = req.url.searchParams.getAll("roller");
      return res(
        ctx.status(200),
        ctx.json(
          mockKontaktpersoner.filter((k) =>
            k.roller.every((r) => roller.includes(r))
          )
        )
      );
    }
  ),

  rest.get<any, any, NavAnsatt>(
    "*/api/v1/internal/ansatt/me",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockBetabruker));
    }
  ),

  rest.put<AvtaleRequest>("*/api/v1/internal/avtaler", (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({ id: "d1f163b7-1a41-4547-af16-03fd4492b7ba" })
    );
  }),
  rest.get<any, any, PaginertUserNotifications>(
    "*/api/v1/internal/notifications",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockNotifikasjoner));
    }
  ),
  rest.get<any, any, UserNotificationSummary>(
    "*/api/v1/internal/notifications/summary",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockUserNotificationSummary));
    }
  ),
  rest.post<any, any, any>(
    "*/api/v1/internal/notifications/:id/status",
    (req, res, ctx) => {
      return res(ctx.status(200));
    }
  ),

  rest.get<any, any, Virksomhet[]>(
    "*/api/v1/internal/virksomhet/sok/:sok",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          mockVirksomheter.filter((enhet) =>
            enhet.navn?.toLowerCase().includes(req.params.sok.toLowerCase())
          )
        )
      );
    }
  ),
  rest.get<any, any, Virksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:orgnr",
    (req, res, ctx) => {
      return res(
        ctx.status(200),
        ctx.json(
          mockVirksomheter.find(
            (enhet) => enhet.organisasjonsnummer === req.params.orgnr
          )
        )
      );
    }
  ),
  rest.get<any, any, Virksomhet[] | undefined>(
    "*/api/v1/internal/virksomhet",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockVirksomheter));
    }
  ),

  rest.put<Utkast, any, any>(
    "*/api/v1/internal/utkast",
    async (req, res, ctx) => {
      const data = await req.json<Utkast>();

      const lagretUtkastIndex = mockUtkast.findIndex((ut) => ut.id === data.id);

      let payload: Utkast = {
        ...data,
        createdAt: new Date().toDateString(),
        updatedAt: new Date().toDateString(),
      };

      if (lagretUtkastIndex > -1) {
        const lagretUtkast = mockUtkast[lagretUtkastIndex];
        payload = {
          ...payload,
          ...lagretUtkast,
          utkastData: { ...data.utkastData },
        };
        mockUtkast[lagretUtkastIndex] = payload;
        console.log("Upserted", mockUtkast);
      } else {
        mockUtkast.push(data);
        console.log("Pushed", data);
      }

      return res(
        ctx.status(200),
        ctx.delay(),
        ctx.json<Utkast>({ ...payload })
      );
    }
  ),
  rest.get<Utkast, any, any>(
    "*/api/v1/internal/utkast/:id",
    async (req, res, ctx) => {
      return res(ctx.status(200), ctx.delay(), ctx.json(mockUtkast));
    }
  ),
  rest.delete<Utkast, any, any>(
    "*/api/v1/internal/utkast/:id",
    async (req, res, ctx) => {
      const { id } = req.params;
      const updated = mockUtkast.filter((ut) => ut.id !== id);
      return res(ctx.status(200), ctx.delay(), ctx.json(updated));
    }
  ),
  rest.get<Utkast, any, any>(
    "*/api/v1/internal/utkast/mine",
    async (req, res, ctx) => {
      return res(ctx.status(200), ctx.delay(), ctx.json(mockUtkast));
    }
  ),
];
